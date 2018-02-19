package stroom.pipeline.server.ext.source;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import stroom.feed.MetaMap;
import stroom.feed.MetaMapFactory;
import stroom.feed.StroomHeaderArguments;
import stroom.feed.StroomStatusCode;
import stroom.feed.StroomStreamException;
import stroom.io.StreamCloser;
import stroom.node.server.NodeCache;
import stroom.pipeline.server.LocationFactoryProxy;
import stroom.pipeline.server.PipelineService;
import stroom.pipeline.server.StreamLocationFactory;
import stroom.pipeline.server.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.server.errorhandler.ErrorStatistics;
import stroom.pipeline.server.errorhandler.LoggedException;
import stroom.pipeline.server.factory.Pipeline;
import stroom.pipeline.server.factory.PipelineDataCache;
import stroom.pipeline.server.factory.PipelineFactory;
import stroom.pipeline.shared.PipelineEntity;
import stroom.pipeline.shared.data.PipelineData;
import stroom.pipeline.state.FeedHolder;
import stroom.pipeline.state.MetaData;
import stroom.pipeline.state.PipelineHolder;
import stroom.pipeline.state.RecordCount;
import stroom.proxy.repo.StroomZipEntry;
import stroom.proxy.repo.StroomZipFileType;
import stroom.proxy.repo.StroomZipNameSet;
import stroom.query.api.v2.DocRef;
import stroom.statistics.internal.InternalStatisticEvent;
import stroom.statistics.internal.InternalStatisticsReceiver;
import stroom.streamstore.shared.StreamAttributeConstants;
import stroom.util.date.DateUtil;
import stroom.util.io.ByteCountInputStream;
import stroom.util.io.CloseableUtil;
import stroom.util.io.StreamUtil;
import stroom.util.shared.ModelStringUtil;
import stroom.util.shared.Severity;
import stroom.util.spring.StroomScope;
import stroom.util.task.TaskMonitor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main entry point to handling proxy requests.
 * <p>
 * This class used the main context and forwards the request on to our
 * dynamic mini proxy.
 */
@Component
@Scope(StroomScope.PROTOTYPE)
public class HttpSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSource.class);

    private static final String ZERO_CONTENT = "0";
    private static final Logger RECEIVE_LOG = LoggerFactory.getLogger("receive");

    private static final String PROCESSING = "Processing:";
    private static final String FINISHED = "Finished:";
    private static final String INTERNAL_STAT_KEY_HTTP_SOURCE = "httpSource";

    private final PipelineFactory pipelineFactory;
    private final PipelineService pipelineService;
    private final TaskMonitor taskMonitor;
    private final PipelineHolder pipelineHolder;
    private final FeedHolder feedHolder;
    private final LocationFactoryProxy locationFactory;
    private final ErrorReceiverProxy errorReceiverProxy;
    private final MetaData metaData;
    private final RecordCount recordCount;
    private final StreamCloser streamCloser;
    private final NodeCache nodeCache;
    private final PipelineDataCache pipelineDataCache;
    private final InternalStatisticsReceiver internalStatisticsReceiver;

    private static String hostName;
    private boolean appendReceivedPath = true;
    private MetaMap headerMetaMap;

    @Inject
    public HttpSource(final PipelineFactory pipelineFactory,
                      @Named("cachedPipelineService") final PipelineService pipelineService,
                      final TaskMonitor taskMonitor,
                      final PipelineHolder pipelineHolder,
                      final FeedHolder feedHolder,
                      final LocationFactoryProxy locationFactory,
                      final ErrorReceiverProxy errorReceiverProxy,
                      final MetaData metaData,
                      final RecordCount recordCount,
                      final StreamCloser streamCloser,
                      final NodeCache nodeCache,
                      final PipelineDataCache pipelineDataCache,
                      final InternalStatisticsReceiver internalStatisticsReceiver) {
        this.pipelineFactory = pipelineFactory;
        this.pipelineService = pipelineService;
        this.taskMonitor = taskMonitor;
        this.pipelineHolder = pipelineHolder;
        this.feedHolder = feedHolder;
        this.locationFactory = locationFactory;
        this.errorReceiverProxy = errorReceiverProxy;
        this.metaData = metaData;
        this.recordCount = recordCount;
        this.streamCloser = streamCloser;
        this.nodeCache = nodeCache;
        this.pipelineDataCache = pipelineDataCache;
        this.internalStatisticsReceiver = internalStatisticsReceiver;
    }

    public void exec(final DocRef pipelineDocRef, final HttpServletRequest request, final HttpServletResponse response) {
        final AtomicBoolean allOk = new AtomicBoolean(true);

        try {
            // Setup the error handler and receiver.
            errorReceiverProxy.setErrorReceiver((severity, location, elementId, message, e) -> {
                switch (severity) {
                    case INFO:
                        RECEIVE_LOG.info(message);
                        break;
                    case WARNING:
                        RECEIVE_LOG.warn(message);
                        break;
                    default:
                        RECEIVE_LOG.error(message);
                }

                if (severity.greaterThan(Severity.WARNING)) {
                    allOk.set(false);
                    StroomStreamException.sendErrorResponse(response, (Exception) e);
                }
            });

            process(pipelineDocRef, request, response);

        } catch (final Exception e) {
            outputError(e);
        }

        if (allOk.get()) {
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private void process(final DocRef pipelineDocRef, final HttpServletRequest request, final HttpServletResponse response) {
        // Record when processing began so we know how long it took
        // afterwards.
        final long startTime = System.currentTimeMillis();
        String feed = null;
        PipelineEntity pipelineEntity = null;

        // Create a meta map from the HTTP header.
        headerMetaMap = createHeaderMetaMap(request);
        metaData.set(headerMetaMap);

        try {
            // Get the feed.
            feed = headerMetaMap.get(StroomHeaderArguments.FEED);
            feedHolder.setFeed(feed);

            // Set the pipeline so it can be used by a filter if needed.
            pipelineEntity = pipelineService.loadByUuid(pipelineDocRef.getUuid());
            pipelineHolder.setPipeline(pipelineEntity);

            // Create some processing info.
            final StringBuilder infoSb = new StringBuilder();
            infoSb.append(" pipeline=");
            infoSb.append(pipelineEntity.getName());
            infoSb.append(", streamCreated=");
            infoSb.append(DateUtil.createNormalDateTimeString(startTime));
            final String info = infoSb.toString();

            // Create processing start message.
            final StringBuilder processingInfoSb = new StringBuilder();
            processingInfoSb.append(PROCESSING);
            processingInfoSb.append(info);
            final String processingInfo = processingInfoSb.toString();

            // Log that we are starting to process.
            taskMonitor.info(processingInfo);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(processingInfo);
            }

            // Process the streams.
            final PipelineData pipelineData = pipelineDataCache.get(pipelineEntity);
            final Pipeline pipeline = pipelineFactory.create(pipelineData);
            processRequest(pipeline, request, response);

            // Create processing finished message.
            final StringBuilder finishedInfoSb = new StringBuilder();
            finishedInfoSb.append(FINISHED);
            finishedInfoSb.append(info);
            finishedInfoSb.append(", finished in ");
            finishedInfoSb.append(ModelStringUtil.formatDurationString(System.currentTimeMillis() - startTime));
            final String finishedInfo = finishedInfoSb.toString();

            // Log that we have finished processing.
            taskMonitor.info(finishedInfo);
            LOGGER.info(finishedInfo);

        } catch (final Exception e) {
            outputError(e);

        } finally {
            // Record some statistics about processing.
            recordStats(feed, pipelineEntity);

            // Update the meta data for all output streams to use.
            updateMetaData();

            try {
                // Close all open streams.
                streamCloser.close();
            } catch (final IOException e) {
                outputError(e);
            }
        }
    }

    private void recordStats(final String feed, final PipelineEntity pipelineEntity) {
        try {
            InternalStatisticEvent event = InternalStatisticEvent.createPlusOneCountStat(
                    INTERNAL_STAT_KEY_HTTP_SOURCE,
                    System.currentTimeMillis(),
                    ImmutableMap.of(
                            "Feed", feed,
                            "Pipeline", pipelineEntity.getName(),
                            "Node", nodeCache.getDefaultNode().getName()));

            internalStatisticsReceiver.putEvent(event);

        } catch (final Exception ex) {
            LOGGER.error("recordStats", ex);
        }
    }

    public long getRead() {
        return recordCount.getRead();
    }

    public long getWritten() {
        return recordCount.getWritten();
    }

    public long getMarkerCount(final Severity... severity) {
        long count = 0;
        if (errorReceiverProxy.getErrorReceiver() instanceof ErrorStatistics) {
            final ErrorStatistics statistics = (ErrorStatistics) errorReceiverProxy.getErrorReceiver();
            for (final Severity sev : severity) {
                count += statistics.getRecords(sev);
            }
        }
        return count;
    }

    /**
     * Processes a source and writes the result to a target.
     */
    private void processRequest(final Pipeline pipeline, final HttpServletRequest request, final HttpServletResponse response) {
        try {
            try {
                final StreamLocationFactory streamLocationFactory = new StreamLocationFactory();
                locationFactory.setLocationFactory(streamLocationFactory);

                final InputStream inputStream = request.getInputStream();
                pipeline.startProcessing();

                // Process the input.
                try {
                    processRequestInputStream(pipeline, inputStream);
                } catch (final LoggedException e) {
                    // The exception has already been logged so ignore it.
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Error while processing: " + e.getMessage(), e);
                    }
                } catch (final Exception e) {
                    outputError(e);
                }

                // Reset the error statistics for the next stream.
                if (errorReceiverProxy.getErrorReceiver() instanceof ErrorStatistics) {
                    ((ErrorStatistics) errorReceiverProxy.getErrorReceiver()).reset();
                }

            } catch (final LoggedException e) {
                // The exception has already been logged so ignore it.
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Error while processing: " + e.getMessage(), e);
                }
            } catch (final Exception e) {
                // An exception that's gets here is definitely a failure.
                outputError(e);

            } finally {
                // Update the meta data for all output streams to use
                updateMetaData();

                try {
                    pipeline.endProcessing();
                } catch (final LoggedException e) {
                    // The exception has already been logged so ignore it.
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Error while processing: " + e.getMessage(), e);
                    }
                } catch (final Exception e) {
                    outputError(e);
                }
            }
        } catch (final Exception e) {
            outputError(e);
        }
    }

    private void updateMetaData() {
        try {
            // Write some meta data to the map for all output streams to use
            // when they close.
            metaData.put(StreamAttributeConstants.REC_READ, String.valueOf(recordCount.getRead()));
            metaData.put(StreamAttributeConstants.REC_WRITE, String.valueOf(recordCount.getWritten()));
            metaData.put(StreamAttributeConstants.REC_INFO, String.valueOf(getMarkerCount(Severity.INFO)));
            metaData.put(StreamAttributeConstants.REC_WARN, String.valueOf(getMarkerCount(Severity.WARNING)));
            metaData.put(StreamAttributeConstants.REC_ERROR, String.valueOf(getMarkerCount(Severity.ERROR)));
            metaData.put(StreamAttributeConstants.REC_FATAL, String.valueOf(getMarkerCount(Severity.FATAL_ERROR)));
            metaData.put(StreamAttributeConstants.DURATION, String.valueOf(recordCount.getDuration()));
            metaData.put(StreamAttributeConstants.NODE, nodeCache.getDefaultNode().getName());
        } catch (final Exception e) {
            outputError(e);
        }
    }

    private void outputError(final Exception ex) {
        outputError(ex, Severity.FATAL_ERROR);
    }

    /**
     * Used to handle any errors that may occur during translation.
     */
    private void outputError(final Exception ex, final Severity severity) {
        if (errorReceiverProxy != null && !(ex instanceof LoggedException)) {
            try {
                if (ex.getMessage() != null) {
                    errorReceiverProxy.log(severity, null, "HttpSource", ex.getMessage(), ex);
                } else {
                    errorReceiverProxy.log(severity, null, "HttpSource", ex.toString(), ex);
                }
            } catch (final Throwable e) {
                // Ignore exception as we generated it.
            }

            if (errorReceiverProxy.getErrorReceiver() instanceof ErrorStatistics) {
                ((ErrorStatistics) errorReceiverProxy.getErrorReceiver()).checkRecord(-1);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Error while processing: " + ex.getMessage(), ex);
            }
        } else {
            LOGGER.error(MarkerFactory.getMarker("FATAL"), ex.getMessage(), ex);
        }
    }


//    @Override
//    public void handle(final HttpServletRequest request, final HttpServletResponse response) {
//        int returnCode = HttpServletResponse.SC_OK;
//
//        final long startTimeMs = System.currentTimeMillis();
//        final MetaMap metaMap = MetaMapFactory.create(request);
//
//        try {
//            try (final ByteCountInputStream inputStream = new ByteCountInputStream(request.getInputStream())) {
//                // Test to see if we are going to accept this stream or drop the data.
//                if (metaMapFilter.filter(metaMap)) {
//                    // Send the data
//                    final List<StreamHandler> handlers = streamHandlerFactory.addReceiveHandlers(new ArrayList<>());
//
//                    try {
//                        // Set the meta map for all handlers.
//                        for (final StreamHandler streamHandler : handlers) {
//                            streamHandler.setMetaMap(metaMap);
//                        }
//
//                        final byte[] buffer = BufferFactory.create();
//                        final StroomStreamProcessor stroomStreamProcessor = new StroomStreamProcessor(metaMap, handlers, buffer, "DataFeedServlet");
//
//                        stroomStreamProcessor.processRequestHeader(request);
//
//                        for (final StreamHandler streamHandler : handlers) {
//                            streamHandler.handleHeader();
//                        }
//
//                        stroomStreamProcessor.process(request.getInputStream(), "");
//
//                        for (final StreamHandler streamHandler : handlers) {
//                            streamHandler.handleFooter();
//                        }
//
//                    } catch (final Exception e) {
//                        for (final StreamHandler streamHandler : handlers) {
//                            try {
//                                streamHandler.handleError();
//                            } catch (final Exception ex) {
//                                LOGGER.error(ex.getMessage(), ex);
//                            }
//                        }
//
//                        throw e;
//                    }
//
//                    final long duration = System.currentTimeMillis() - startTimeMs;
//                    logStream.log(RECEIVE_LOG, metaMap, "RECEIVE", request.getRequestURI(), returnCode, inputStream.getByteCount(), duration);
//
//                } else {
//                    // Just read the stream in and ignore it
//                    final byte[] buffer = BufferFactory.create();
//                    while (inputStream.read(buffer) >= 0) {
//                        // Ignore data.
//                    }
//                    returnCode = HttpServletResponse.SC_OK;
//                    LOGGER.warn("\"Dropped stream\",{}", CSVFormatter.format(metaMap));
//
//                    final long duration = System.currentTimeMillis() - startTimeMs;
//                    logStream.log(RECEIVE_LOG, metaMap, "DROP", request.getRequestURI(), returnCode, inputStream.getByteCount(), duration);
//                }
//            }
//        } catch (final StroomStreamException e) {
//            StroomStreamException.sendErrorResponse(response, e);
//            returnCode = e.getStroomStatusCode().getCode();
//
//            LOGGER.warn("\"handleException()\",{},\"{}\"", CSVFormatter.format(metaMap), CSVFormatter.escape(e.getMessage()));
//
//            final long duration = System.currentTimeMillis() - startTimeMs;
//            if (StroomStatusCode.RECEIPT_POLICY_SET_TO_REJECT_DATA.equals(e.getStroomStatusCode())) {
//                logStream.log(RECEIVE_LOG, metaMap, "REJECT", request.getRequestURI(), returnCode, -1, duration);
//            } else {
//                logStream.log(RECEIVE_LOG, metaMap, "ERROR", request.getRequestURI(), returnCode, -1, duration);
//            }
//
//        } catch (final Exception e) {
//            StroomStreamException.sendErrorResponse(response, e);
//            returnCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
//
//            LOGGER.error("\"handleException()\",{}", CSVFormatter.format(metaMap), e);
//            final long duration = System.currentTimeMillis() - startTimeMs;
//            logStream.log(RECEIVE_LOG, metaMap, "ERROR", request.getRequestURI(), returnCode, -1, duration);
//        }
//
//        response.setStatus(returnCode);
//    }


    String getHostName() {
        if (hostName == null) {
            try {
                setHostName(InetAddress.getLocalHost().getHostName());
            } catch (final Exception ex) {
                setHostName("Unknown");
            }
        }
        return hostName;
    }

    static void setHostName(final String hostName) {
        HttpSource.hostName = hostName;
    }

    public void setAppendReceivedPath(final boolean appendReceivedPath) {
        this.appendReceivedPath = appendReceivedPath;
    }

//    public void setStreamProgressMonitor(final StreamProgressMonitor streamProgressMonitor) {
//        this.streamProgressMonitor = streamProgressMonitor;
//    }

    private MetaMap createHeaderMetaMap(final HttpServletRequest request) {
        final MetaMap metaMap = MetaMapFactory.create(request);
        String guid = metaMap.get(StroomHeaderArguments.GUID);

        // Allocate a GUID if we have not got one.
        if (guid == null) {
            guid = UUID.randomUUID().toString();
            metaMap.put(StroomHeaderArguments.GUID, guid);

            // Only allocate RemoteXxx details if the GUID has not been
            // allocated.

            // Allocate remote address if not set.
            if (StringUtils.hasText(request.getRemoteAddr())) {
                metaMap.put(StroomHeaderArguments.REMOTE_ADDRESS, request.getRemoteAddr());
            }

            // Save the time the data was received.
            metaMap.put(StroomHeaderArguments.RECEIVED_TIME, DateUtil.createNormalDateTimeString());

            // Allocate remote address if not set.
            if (StringUtils.hasText(request.getRemoteHost())) {
                metaMap.put(StroomHeaderArguments.REMOTE_HOST, request.getRemoteHost());
            }
        }

        return metaMap;
    }

    private void processRequestInputStream(final Pipeline pipeline, final InputStream inputStream) {
        try {
            String compression = headerMetaMap.get(StroomHeaderArguments.COMPRESSION);
            if (StringUtils.hasText(compression)) {
                compression = compression.toUpperCase(StreamUtil.DEFAULT_LOCALE);
                if (!StroomHeaderArguments.VALID_COMPRESSION_SET.contains(compression)) {
                    throw new StroomStreamException(StroomStatusCode.UNKNOWN_COMPRESSION, compression);
                }
            }

            if (ZERO_CONTENT.equals(headerMetaMap.get(StroomHeaderArguments.CONTENT_LENGTH))) {
                LOGGER.warn("process() - Skipping Zero Content " + headerMetaMap);
                return;
            }

            if (StroomHeaderArguments.COMPRESSION_ZIP.equals(compression)) {
                // Process a zip stream.
                processZipStream(pipeline, inputStream, "");

            } else if (StroomHeaderArguments.COMPRESSION_GZIP.equals(compression)) {
                // Process a gzip stream.
                processGZipStream(pipeline, inputStream);

            } else {
                // Process a regular stream.
                processStream(pipeline, inputStream);

            }
        } finally {
            CloseableUtil.closeLogAndIgnoreException(inputStream);
        }
    }

    private void processStream(final Pipeline pipeline, final InputStream inputStream) {
        final ByteCountInputStream byteCountInputStream = new ByteCountInputStream(inputStream);

        pipeline.startStream();
        pipeline.process(byteCountInputStream);

        final MetaMap entryMetaMap = MetaMapFactory.cloneAllowable(headerMetaMap);
        entryMetaMap.put(StroomHeaderArguments.STREAM_SIZE, String.valueOf(byteCountInputStream.getByteCount()));
        metaData.set(entryMetaMap);

        pipeline.endStream();
    }

    private void processGZipStream(final Pipeline pipeline, final InputStream inputStream) {
        // We have to wrap our stream reading code in a individual
        // try/catch so we can return to the client an error in the
        // case of a corrupt stream.
        try {
            // Use the APACHE GZIP de-compressor as it handles
            // nested compressed streams
            final GzipCompressorInputStream gzipCompressorInputStream = new GzipCompressorInputStream(inputStream, true);
            final ByteCountInputStream byteCountInputStream = new ByteCountInputStream(gzipCompressorInputStream);

            pipeline.startStream();
            pipeline.process(byteCountInputStream);

            final MetaMap entryMetaMap = MetaMapFactory.cloneAllowable(headerMetaMap);
            entryMetaMap.put(StroomHeaderArguments.STREAM_SIZE, String.valueOf(byteCountInputStream.getByteCount()));
            metaData.set(entryMetaMap);

            pipeline.endStream();

        } catch (final IOException e) {
            LOGGER.debug(e.getMessage(), e);
            throw new StroomStreamException(StroomStatusCode.COMPRESSED_STREAM_INVALID, e.getMessage());
        }
    }

    private void processZipStream(final Pipeline pipeline, final InputStream inputStream, final String prefix) {
        final ByteCountInputStream totalByteCountInputStream = new ByteCountInputStream(inputStream);

        final Map<String, MetaMap> bufferedMetaMap = new HashMap<>();
        final Map<String, Long> dataStreamSizeMap = new HashMap<>();
//        final List<String> sendDataList = new ArrayList<>();
        final StroomZipNameSet stroomZipNameSet = new StroomZipNameSet(false);

        final ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(totalByteCountInputStream);

        ZipArchiveEntry zipEntry;
        while (!taskMonitor.isTerminated()) {
            // We have to wrap our stream reading code in a individual try/catch
            // so we can return to the client an error in the case of a corrupt
            // stream.
            try {
                zipEntry = zipArchiveInputStream.getNextZipEntry();
            } catch (final IOException e) {
                LOGGER.debug(e.getMessage(), e);
                throw new StroomStreamException(StroomStatusCode.COMPRESSED_STREAM_INVALID, e.getMessage());
            }

            if (zipEntry == null) {
                // All done
                break;
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("process() - " + zipEntry);
            }

            final String entryName = prefix + zipEntry.getName();
            final StroomZipEntry stroomZipEntry = stroomZipNameSet.add(entryName);

            if (StroomZipFileType.Meta.equals(stroomZipEntry.getStroomZipFileType())) {
                final MetaMap entryMetaMap = MetaMapFactory.cloneAllowable(headerMetaMap);
                // We have to wrap our stream reading code in a individual
                // try/catch so we can return to the client an error in the case
                // of a corrupt stream.
                try {
                    entryMetaMap.read(zipArchiveInputStream, false);
                } catch (final IOException ioEx) {
                    throw new StroomStreamException(StroomStatusCode.COMPRESSED_STREAM_INVALID, ioEx.getMessage());
                }

                if (appendReceivedPath) {
                    // Here we build up a list of stroom servers that have received
                    // the message

                    // The entry one will be initially set at the boundary Stroom
                    // server
                    final String entryReceivedServer = entryMetaMap.get(StroomHeaderArguments.RECEIVED_PATH);

                    if (entryReceivedServer != null) {
                        if (!entryReceivedServer.contains(getHostName())) {
                            entryMetaMap.put(StroomHeaderArguments.RECEIVED_PATH,
                                    entryReceivedServer + "," + getHostName());
                        }
                    } else {
                        entryMetaMap.put(StroomHeaderArguments.RECEIVED_PATH, getHostName());
                    }
                }

                if (entryMetaMap.containsKey(StroomHeaderArguments.STREAM_SIZE)) {
                    // Header already has stream size so just send it on

                    //                    sendHeader(stroomZipEntry, entryMetaMap);
                    metaData.set(entryMetaMap);
                } else {
                    // We need to add the stream size
                    // Send the data file yet ?
                    final String dataFile = stroomZipNameSet.getName(stroomZipEntry.getBaseName(), StroomZipFileType.Data);
                    if (dataFile != null && dataStreamSizeMap.containsKey(dataFile)) {
                        // Yes we can send the header now
                        entryMetaMap.put(StroomHeaderArguments.STREAM_SIZE,
                                String.valueOf(dataStreamSizeMap.get(dataFile)));

                        //                        sendHeader(stroomZipEntry, entryMetaMap);
                        metaData.set(entryMetaMap);
                    } else {
                        // Else we have to buffer it
                        bufferedMetaMap.put(stroomZipEntry.getBaseName(), entryMetaMap);
                    }
                }
            } else {
                //                handleEntryStart(stroomZipEntry);
                pipeline.startStream();

                final ByteCountInputStream byteCountInputStream = new ByteCountInputStream(zipArchiveInputStream);
                pipeline.process(byteCountInputStream);

//                long totalRead = 0;
//                int read = 0;
//                while (!taskMonitor.isTerminated()) {
//                    // We have to wrap our stream reading code in a individual
//                    // try/catch so we can return to the client an error in the
//                    // case of a corrupt stream.
//                    try {
//                        read = StreamUtil.eagerRead(zipArchiveInputStream, buffer);
//                    } catch (final IOException ioEx) {
//                        throw new StroomStreamException(StroomStatusCode.COMPRESSED_STREAM_INVALID, ioEx.getMessage());
//                    }
//                    if (read == -1) {
//                        break;
//                    }
//                    streamProgressMonitor.progress(read);
//                    handleEntryData(buffer, 0, read);
//                    totalRead += read;
//                }


//                if (StroomZipFileType.Data.equals(stroomZipEntry.getStroomZipFileType())) {
//                    sendDataList.add(entryName);
//                    dataStreamSizeMap.put(entryName, totalRead);
//                }

                // Buffered header can now be sent as we have sent the
                // data
                if (stroomZipEntry.getBaseName() != null) {
                    final MetaMap entryMetaMap = bufferedMetaMap.remove(stroomZipEntry.getBaseName());
                    if (entryMetaMap != null) {
                        entryMetaMap.put(StroomHeaderArguments.STREAM_SIZE, String.valueOf(byteCountInputStream.getByteCount()));
                        metaData.set(entryMetaMap);
//                        handleEntryStart(new StroomZipEntry(null, stroomZipEntry.getBaseName(), StroomZipFileType.Meta));
//                        final byte[] headerBytes = entryMetaMap.toByteArray();
//                        handleEntryData(headerBytes, 0, headerBytes.length);
//                        handleEntryEnd();
                    }
                }

                //                handleEntryEnd();
                pipeline.endStream();
            }

        }

        if (stroomZipNameSet.getBaseNameSet().isEmpty()) {
            if (totalByteCountInputStream.getByteCount() > 22) {
                throw new StroomStreamException(StroomStatusCode.COMPRESSED_STREAM_INVALID, "No Zip Entries");
            } else {
                LOGGER.warn("processZipStream() - Zip stream with no entries ! {}", headerMetaMap);
            }
        }

        // Add missing headers
        for (final String baseName : stroomZipNameSet.getBaseNameList()) {
            final String headerName = stroomZipNameSet.getName(baseName, StroomZipFileType.Meta);
            // Send Generic Header
            if (headerName == null) {
                final String dataFileName = stroomZipNameSet.getName(baseName, StroomZipFileType.Data);
//                final MetaMap entryMetaMap = MetaMapFactory.cloneAllowable(headerMetaMap);
                headerMetaMap.put(StroomHeaderArguments.STREAM_SIZE,
                        String.valueOf(dataStreamSizeMap.remove(dataFileName)));

//                sendHeader(new StroomZipEntry(null, baseName, StroomZipFileType.Meta), entryMetaMap);
            }
        }
    }

//    public void closeHandlers() {
//        for (final StroomStreamHandler handler : stroomStreamHandlerList) {
//            if (handler instanceof Closeable) {
//                CloseableUtil.closeLogAndIgnoreException((Closeable) handler);
//            }
//        }
//    }
//
//    private void sendHeader(final StroomZipEntry stroomZipEntry, final MetaMap metaMap) throws IOException {
//        handleEntryStart(stroomZipEntry);
//        // Try and use the buffer
//        final InitialByteArrayOutputStream byteArrayOutputStream = new InitialByteArrayOutputStream(buffer);
//        metaMap.write(byteArrayOutputStream, true);
//        final BufferPos bufferPos = byteArrayOutputStream.getBufferPos();
//        handleEntryData(bufferPos.getBuffer(), 0, bufferPos.getBufferPos());
//        handleEntryEnd();
//    }
//
//    private void handleHeader() throws IOException {
//        for (final StroomStreamHandler stroomStreamHandler : stroomStreamHandlerList) {
//            if (stroomStreamHandler instanceof StroomHeaderStreamHandler) {
//                ((StroomHeaderStreamHandler) stroomStreamHandler).handleHeader(globalMetaMap);
//            }
//        }
//    }
//
//    private void handleEntryStart(final StroomZipEntry stroomZipEntry) throws IOException {
//        for (final StroomStreamHandler stroomStreamHandler : stroomStreamHandlerList) {
//            stroomStreamHandler.handleEntryStart(stroomZipEntry);
//        }
//    }
//
//    private void handleEntryEnd() throws IOException {
//        for (final StroomStreamHandler stroomStreamHandler : stroomStreamHandlerList) {
//            stroomStreamHandler.handleEntryEnd();
//        }
//    }
//
//    private void handleEntryData(final byte[] data, final int off, final int len) throws IOException {
//        for (final StroomStreamHandler stroomStreamHandler : stroomStreamHandlerList) {
//            stroomStreamHandler.handleEntryData(data, off, len);
//        }
//    }
}
