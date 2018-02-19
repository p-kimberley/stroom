/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.pipeline.server.ext.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.io.StreamCloser;
import stroom.pipeline.server.errorhandler.ErrorReceiver;
import stroom.pipeline.state.RecordCount;
import stroom.query.api.v2.DocRef;
import stroom.task.server.TaskContext;
import stroom.util.date.DateUtil;
import stroom.util.io.AbstractFileVisitor;
import stroom.util.io.FileUtil;
import stroom.util.io.StreamUtil;
import stroom.util.shared.ModelStringUtil;
import stroom.util.shared.Severity;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class that reads a nested directory tree of stroom zip files.
 */
public final class DirectoryProcessor {
    private static final String PROC_EXTENSION = ".proc";
    private static final String OK_EXTENSION = ".ok";
    private static final String ERROR_EXTENSION = ".err";
    private final Logger LOGGER = LoggerFactory.getLogger(DirectoryProcessor.class);

    private static final String PROCESSING = "Processing:";
    private static final String FINISHED = "Finished:";
//    private static final String INTERNAL_STAT_KEY_HTTP_SOURCE = "directorySource";

    private final Executor executor;
    private final TaskContext taskContext;
    private final RecordCount recordCount;
    private final StreamCloser streamCloser;
    private final Provider<ProxyZipSource> proxyZipSourceProvider;

    @Inject
    public DirectoryProcessor(final Executor executor,
            final TaskContext taskContext,
                              final RecordCount recordCount,
                              final StreamCloser streamCloser,
                              final Provider<ProxyZipSource> proxyZipSourceProvider) {
        this.executor = executor;
        this.taskContext = taskContext;
        this.recordCount = recordCount;
        this.streamCloser = streamCloser;
        this.proxyZipSourceProvider = proxyZipSourceProvider;
    }

    public void process(final DirectoryProcessorConfig directoryProcessorConfig) {
        final DocRef pipelineRef = directoryProcessorConfig.getPipelineRef();
        final Path dir = directoryProcessorConfig.getDir();
        final FileFilter fileFilter = directoryProcessorConfig.getFileFilter();
        final String processName = directoryProcessorConfig.getProcessName();

        // Record when processing began so we know how long it took
        // afterwards.
        final long startTime = System.currentTimeMillis();

        final String startInfo = PROCESSING +
                " " +
                FileUtil.getCanonicalPath(dir);
        taskContext.info(startInfo);
        LOGGER.info(startInfo);

        try {
            try {
                Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new AbstractFileVisitor() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                        try {
                            final FileFilter fileFilter = directoryProcessorConfig.getFileFilter();
                            if (fileFilter.match(file, attrs)) {
                                // Define the process file, error file and ok file paths.
                                final String fileName = file.getFileName().toString();
                                final Path currentDir = file.getParent();
                                final String processFileStem = fileName + "." + processName;
                                final Path processFile = currentDir.resolve(processFileStem + PROC_EXTENSION);
                                final Path errorFile = currentDir.resolve(processFileStem + ERROR_EXTENSION);
                                final Path okFile = currentDir.resolve(processFileStem + OK_EXTENSION);

                                // If the file has already been processed by this directory source processor then ignore it.
                                if (!(Files.exists(errorFile) || Files.exists(okFile))) {
                                    executor.execute(() -> {
                                        final AtomicBoolean allOk = new AtomicBoolean(true);

                                        try {
                                            appendToProcessLog(processFile, "Started processing: " + DateUtil.createNormalDateTimeString());

                                            // Setup the error handler and receiver.
                                            final ErrorReceiver errorReceiver = (severity, location, elementId, message, e) -> {
                                                appendToProcessLog(processFile, severity.getDisplayValue() + ": " + message);
                                                if (severity.greaterThan(Severity.WARNING)) {
                                                    allOk.set(false);
                                                }
                                            };

                                            final ProxyZipSource proxyZipFileSource = proxyZipSourceProvider.get();
                                            proxyZipFileSource.exec(pipelineRef, file, errorReceiver);

                                            appendToProcessLog(processFile, "Finished processing: " + DateUtil.createNormalDateTimeString());

                                            // If we completed processing then move the processing file to be named .ok, if not rename it to .err
                                            if (allOk.get()) {
                                                Files.move(processFile, okFile);
                                            } else {
                                                Files.move(processFile, errorFile);
                                            }
                                        } catch (final IOException e) {
                                            LOGGER.error(e.getMessage(), e);
                                        }
                                    });
                                }
                            }
                        } catch (final RuntimeException e) {
                            LOGGER.error(e.getMessage(), e);
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (final IOException | RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }

            // Create processing finished message.
            final String finishedInfo = FINISHED +
                    " " +
                    FileUtil.getCanonicalPath(dir) +
                    " in " +
                    ModelStringUtil.formatDurationString(System.currentTimeMillis() - startTime);

            // Log that we have finished processing.
            taskContext.info(finishedInfo);
            LOGGER.info(finishedInfo);

        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);

        } finally {
//            // Record some statistics about processing.
//            recordStats(feed, pipelineEntity);
//
//            // Update the meta data for all output streams to use.
//            updateMetaData();

            try {
                // Close all open streams.
                streamCloser.close();
            } catch (final IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void appendToProcessLog(final Path processFile, final String message) {
        try {
            Files.write(processFile, (message + "\n").getBytes(StreamUtil.DEFAULT_CHARSET), StandardOpenOption.APPEND);
        } catch (final IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

//    private void recordStats(final String feed, final PipelineEntity pipelineEntity) {
//        try {
//            InternalStatisticEvent event = InternalStatisticEvent.createPlusOneCountStat(
//                    INTERNAL_STAT_KEY_HTTP_SOURCE,
//                    System.currentTimeMillis(),
//                    ImmutableMap.of(
//                            "Feed", feed,
//                            "Pipeline", pipelineEntity.getName(),
//                            "Node", nodeCache.getDefaultNode().getName()));
//
//            internalStatisticsReceiver.putEvent(event);
//
//        } catch (final Exception ex) {
//            LOGGER.error("recordStats", ex);
//        }
//    }

    public long getRead() {
        return recordCount.getRead();
    }

    public long getWritten() {
        return recordCount.getWritten();
    }

//    public long getMarkerCount(final Severity... severity) {
//        long count = 0;
//        if (errorReceiverProxy.getErrorReceiver() instanceof ErrorStatistics) {
//            final ErrorStatistics statistics = (ErrorStatistics) errorReceiverProxy.getErrorReceiver();
//            for (final Severity sev : severity) {
//                count += statistics.getRecords(sev);
//            }
//        }
//        return count;
//    }

//    /**
//     * Processes a source and writes the result to a target.
//     */
//    private void processFile(final Pipeline pipeline, final Path file) {
//
//
//        // Get the feed.
//        feed = headerMetaMap.get(StroomHeaderArguments.FEED);
//        feedHolder.setFeed(feed);
//
//        // Create a meta map from the HTTP header.
//        headerMetaMap = createHeaderMetaMap(request);
//        metaData.set(headerMetaMap);
//
//
//        try {
//            try {
//                final StreamLocationFactory streamLocationFactory = new StreamLocationFactory();
//                locationFactory.setLocationFactory(streamLocationFactory);
//
//                final InputStream inputStream = Files.newInputStream(file);
//                pipeline.startProcessing();
//
//                // Process the input.
//                try {
//                    processRequestInputStream(pipeline, inputStream);
//                } catch (final LoggedException e) {
//                    // The exception has already been logged so ignore it.
//                    if (LOGGER.isTraceEnabled()) {
//                        LOGGER.trace("Error while processing: " + e.getMessage(), e);
//                    }
//                } catch (final Exception e) {
//                    outputError(e);
//                }
//
//                // Reset the error statistics for the next stream.
//                if (errorReceiverProxy.getErrorReceiver() instanceof ErrorStatistics) {
//                    ((ErrorStatistics) errorReceiverProxy.getErrorReceiver()).reset();
//                }
//
//            } catch (final LoggedException e) {
//                // The exception has already been logged so ignore it.
//                if (LOGGER.isTraceEnabled()) {
//                    LOGGER.trace("Error while processing: " + e.getMessage(), e);
//                }
//            } catch (final Exception e) {
//                // An exception that's gets here is definitely a failure.
//                outputError(e);
//
//            } finally {
//                // Update the meta data for all output streams to use
//                updateMetaData();
//
//                try {
//                    pipeline.endProcessing();
//                } catch (final LoggedException e) {
//                    // The exception has already been logged so ignore it.
//                    if (LOGGER.isTraceEnabled()) {
//                        LOGGER.trace("Error while processing: " + e.getMessage(), e);
//                    }
//                } catch (final Exception e) {
//                    outputError(e);
//                }
//            }
//        } catch (final Exception e) {
//            outputError(e);
//        }
//    }
//
//    private void updateMetaData() {
//        try {
//            // Write some meta data to the map for all output streams to use
//            // when they close.
//            metaData.put(StreamAttributeConstants.REC_READ, String.valueOf(recordCount.getRead()));
//            metaData.put(StreamAttributeConstants.REC_WRITE, String.valueOf(recordCount.getWritten()));
//            metaData.put(StreamAttributeConstants.REC_INFO, String.valueOf(getMarkerCount(Severity.INFO)));
//            metaData.put(StreamAttributeConstants.REC_WARN, String.valueOf(getMarkerCount(Severity.WARNING)));
//            metaData.put(StreamAttributeConstants.REC_ERROR, String.valueOf(getMarkerCount(Severity.ERROR)));
//            metaData.put(StreamAttributeConstants.REC_FATAL, String.valueOf(getMarkerCount(Severity.FATAL_ERROR)));
//            metaData.put(StreamAttributeConstants.DURATION, String.valueOf(recordCount.getDuration()));
//            metaData.put(StreamAttributeConstants.NODE, nodeCache.getDefaultNode().getName());
//        } catch (final Exception e) {
//            outputError(e);
//        }
//    }

//    private void outputError(final Exception ex) {
//        outputError(ex, Severity.FATAL_ERROR);
//    }
//
//    /**
//     * Used to handle any errors that may occur during translation.
//     */
//    private void outputError(final Exception ex, final Severity severity) {
//        if (errorReceiverProxy != null && !(ex instanceof LoggedException)) {
//            try {
//                if (ex.getMessage() != null) {
//                    errorReceiverProxy.log(severity, null, "HttpSource", ex.getMessage(), ex);
//                } else {
//                    errorReceiverProxy.log(severity, null, "HttpSource", ex.toString(), ex);
//                }
//            } catch (final Throwable e) {
//                // Ignore exception as we generated it.
//            }
//
//            if (errorReceiverProxy.getErrorReceiver() instanceof ErrorStatistics) {
//                ((ErrorStatistics) errorReceiverProxy.getErrorReceiver()).checkRecord(-1);
//            }
//
//            if (LOGGER.isTraceEnabled()) {
//                LOGGER.trace("Error while processing: " + ex.getMessage(), ex);
//            }
//        } else {
//            LOGGER.error(MarkerFactory.getMarker("FATAL"), ex.getMessage(), ex);
//        }
//    }
//

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
//
//
//    String getHostName() {
//        if (hostName == null) {
//            try {
//                setHostName(InetAddress.getLocalHost().getHostName());
//            } catch (final Exception ex) {
//                setHostName("Unknown");
//            }
//        }
//        return hostName;
//    }
//
//    static void setHostName(final String hostName) {
//        HttpSource.hostName = hostName;
//    }
//
//    public void setAppendReceivedPath(final boolean appendReceivedPath) {
//        this.appendReceivedPath = appendReceivedPath;
//    }
//
//    public void setStreamProgressMonitor(final StreamProgressMonitor streamProgressMonitor) {
//        this.streamProgressMonitor = streamProgressMonitor;
//    }
//
//    private MetaMap createHeaderMetaMap(final HttpServletRequest request) {
//        final MetaMap metaMap = MetaMapFactory.create(request);
//        String guid = metaMap.get(StroomHeaderArguments.GUID);
//
//        // Allocate a GUID if we have not got one.
//        if (guid == null) {
//            guid = UUID.randomUUID().toString();
//            metaMap.put(StroomHeaderArguments.GUID, guid);
//
//            // Only allocate RemoteXxx details if the GUID has not been
//            // allocated.
//
//            // Allocate remote address if not set.
//            if (StringUtils.hasText(request.getRemoteAddr())) {
//                metaMap.put(StroomHeaderArguments.REMOTE_ADDRESS, request.getRemoteAddr());
//            }
//
//            // Save the time the data was received.
//            metaMap.put(StroomHeaderArguments.RECEIVED_TIME, DateUtil.createNormalDateTimeString());
//
//            // Allocate remote address if not set.
//            if (StringUtils.hasText(request.getRemoteHost())) {
//                metaMap.put(StroomHeaderArguments.REMOTE_HOST, request.getRemoteHost());
//            }
//        }
//
//        return metaMap;
//    }
//
//    private void processFile(final Pipeline pipeline, final Path file) {
//        try {
//            // If there is a corresponding meta file then read it and use it.
//            final Path metaFile = file.getParent().resolve(file.getFileName().toString() + ".meta");
//            final Optional<MetaMap> optionalMetaMap;
//            if (Files.isRegularFile(metaFile)) {
//                final MetaMap metaMap = new MetaMap();
//                metaMap.read(Files.newInputStream(metaFile), true);
//                optionalMetaMap = Optional.of(metaMap);
//            } else {
//                optionalMetaMap = Optional.empty();
//            }
//
//            if (file.getFileName().toString().endsWith(".zip")) {
//                // Process a zip stream.
//                processStroomZipFile(pipeline, file,  optionalMetaMap);
//            }
////            else {
////                // Process a regular stream.
////                processStream(pipeline, file, optionalMetaMap);
////
////            }
//        } catch (final IOException e) {
//            LOGGER.error(e.getMessage(),e);
//        }
//    }
//
//    private void processStream(final Pipeline pipeline, final Path file, final Optional<MetaMap> optionalMetaMap) throws IOException {
//        final ByteCountInputStream byteCountInputStream = new ByteCountInputStream(Files.newInputStream(file));
//
//        pipeline.startStream();
//        pipeline.process(byteCountInputStream);
//
//        final MetaMap entryMetaMap = MetaMapFactory.cloneAllowable(headerMetaMap);
//        entryMetaMap.put(StroomHeaderArguments.STREAM_SIZE, String.valueOf(byteCountInputStream.getByteCount()));
//        metaData.set(entryMetaMap);
//
//        pipeline.endStream();
//    }
//
//    private void processStroomZipFile(final Pipeline pipeline, final Path file, final Optional<MetaMap> optionalMetaMap) throws IOException {
//        try (final StroomZipFile stroomZipFile = new StroomZipFile(file)) {
//            long entrySequence = 1;
//            for (final String sourceName : stroomZipFile.getStroomZipNameSet().getBaseNameSet()) {
//                final long streamNo = entrySequence++;
//
//                final StreamSourceInputStreamProvider metaInputStreamProvider = stroomZipFile.getStreamProvider(streamNo, sourceName, StroomZipFileType.Meta);
//                final StreamSourceInputStreamProvider contextInputStreamProvider = stroomZipFile.getStreamProvider(streamNo, sourceName, StroomZipFileType.Context);
//                final StreamSourceInputStreamProvider dataInputStreamProvider = stroomZipFile.getStreamProvider(streamNo, sourceName, StroomZipFileType.Data);
//
//                final AtomicLong totalStreamSize = new AtomicLong();
//                totalStreamSize.addAndGet(metaInputStreamProvider.getStream(streamNo).size());
//                totalStreamSize.addAndGet(contextInputStreamProvider.getStream(streamNo).size());
//                totalStreamSize.addAndGet(dataInputStreamProvider.getStream(streamNo).size());
//
//                // Set the stream holder streams.
//                streamHolder.addProvider(metaInputStreamProvider, StreamType.META);
//                streamHolder.addProvider(contextInputStreamProvider, StreamType.CONTEXT);
//
//                // Get the meta data and set it for the task.
//                final MetaMap entryMetaMap;
//                if (optionalMetaMap.isPresent()) {
//                    entryMetaMap = MetaMapFactory.cloneAllowable(optionalMetaMap.get());
//                } else {
//                    entryMetaMap = new MetaMap();
//                }
//                metaData.set(entryMetaMap);
//                if (metaInputStreamProvider != null) {
//                    final StreamSourceInputStream inputStream = metaInputStreamProvider.getStream(streamNo);
//                    // Make sure we got an input stream.
//                    if (inputStream != null) {
//                        // Only use meta data if we actually have some.
//                        if (inputStream.size() > 10) {
//                            entryMetaMap.read(inputStream, false);
//                        }
//                    }
//                }
//
//                feedHolder.setFeed(entryMetaMap.get(StroomHeaderArguments.FEED));
//
//                pipeline.startStream();
//
//                pipeline.process(dataInputStreamProvider.getStream(streamNo));
//
//                // Before we end processing update the raw size for the stream.
//                entryMetaMap.put(StroomHeaderArguments.STREAM_SIZE, String.valueOf(totalStreamSize.get()));
//
//                pipeline.endStream();
//            }
//        }
//    }
//


//
//    public DirectorySource(final Executor executor, final TaskContext taskContext, final FileProcessor processor) {
//        this.executor = executor;
//        this.taskContext = taskContext;
//        this.processor = processor;
//    }
//
//    /**
//     * Process a Stroom zip repository,
//     *
//     * @param stroomZipRepository The Stroom zip repository to process.
//     * @return True is there are more files to process, i.e. we reached our max
//     * file scan limit.
//     */
//    public final void process(final DocRef pipelineRef, final Path dir) {
//        final Set<CompletableFuture> futures = Collections.newSetFromMap(new ConcurrentHashMap<>());
//
//            taskContext.setName("DirectorySource");
//            taskContext.info(FileUtil.getCanonicalPath(dir));
//
//            try {
//                Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new AbstractFileVisitor() {
//                    @Override
//                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
//                        try {
//                                if (fileFilter.match(file, attrs)) {
//
//
//
//                                }
//                            };
//                            final CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(runnable, processFileExecutor);
//                            completableFuture.thenAccept(r -> futures.remove(completableFuture));
//                            futures.add(completableFuture);
//                        } catch (final RuntimeException e) {
//                            LOGGER.error(e.getMessage(), e);
//                        }
//
//                        return FileVisitResult.CONTINUE;
//                    }
//                });
//            } catch (final IOException | RuntimeException e) {
//                LOGGER.error(e.getMessage(), e);
//            }
//
//            // Wait for all inner tasks to complete.
//            try {
//                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
//            } catch (final RuntimeException e) {
//                LOGGER.error(e.getMessage(), e);
//            }
//        };
//        final CompletableFuture completableFuture = CompletableFuture.runAsync(runnable, walkFileTreeExecutor);
//    }
//
//
//    private void addError(final Path file, final String message) {
//        final Path errorFile = file.getParent().resolve(file.getFileName().toString() + ERROR_EXTENSION);
//        try {
//            if (!Files.isRegularFile(errorFile)) {
//                try (final OutputStream os = Files.newOutputStream(errorFile)) {
//                    os.write(message.getBytes(CharsetConstants.DEFAULT_CHARSET));
//                }
//            } else {
//                try (final OutputStream os = Files.newOutputStream(errorFile, StandardOpenOption.APPEND)) {
//                    os.write("\n".getBytes(CharsetConstants.DEFAULT_CHARSET));
//                    os.write(message.getBytes(CharsetConstants.DEFAULT_CHARSET));
//                }
//            }
//        } catch (final IOException ex) {
//            LOGGER.warn("Failed to write to file " + errorFile + " message " + message);
//        }
//    }
//
//    private void processFeeds(final StroomZipRepository stroomZipRepository, final FeedPathMap feedPathMap) {
//        final Set<CompletableFuture> futures = new HashSet<>();
//
//        final Iterator<Entry<String, List<Path>>> iter = feedPathMap.getMap().entrySet().iterator();
//        while (iter.hasNext() && !taskContext.isTerminated()) {
//            final Entry<String, List<Path>> entry = iter.next();
//            final String feedName = entry.getKey();
//            final List<Path> fileList = entry.getValue();
//
//            final String msg = "" +
//                    feedName +
//                    " " +
//                    ModelStringUtil.formatCsv(fileList.size()) +
//                    " files (" +
//                    fileList.get(0) +
//                    "..." +
//                    fileList.get(fileList.size() - 1) +
//                    ")";
//
//            final Runnable runnable = () -> {
//                if (!taskContext.isTerminated()) {
//                    taskContext.info(msg);
//                    feedFileProcessor.processFeedFiles(stroomZipRepository, feedName, fileList);
//                } else {
//                    LOGGER.info("Quit Feed Aggregation {}", feedName);
//                }
//            };
//
//            futures.add(CompletableFuture.runAsync(runnable, executor));
//        }
//
//        // Wait for all processes to complete.
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
//    }
//
//    private FeedPathMap createFeedPathMap(final StroomZipRepository stroomZipRepository) {
//        final Map<String, List<Path>> map = new ConcurrentHashMap<>();
//
//        // Scan all of the zip files in the repository so that we can map
//        // zip files to feeds.
//        final Set<CompletableFuture> futures = new HashSet<>();
//        final boolean completedAllFiles = findFeeds(stroomZipRepository.getRootDir(), stroomZipRepository, map, futures);
//
//        if (!completedAllFiles) {
//            LOGGER.debug("Hit scan limit of {}", maxFileScan);
//        }
//
//        // Wait for all of the feed name extraction tasks to complete.
//        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
//
//        LOGGER.debug("Found Feeds {}", map.keySet());
//
//        return new FeedPathMap(completedAllFiles, map);
//    }
//
//    private boolean findFeeds(final Path dir, final StroomZipRepository stroomZipRepository, final Map<String, List<Path>> feedPaths, final Set<CompletableFuture> futures) {
//        LogExecutionTime logExecutionTime = new LogExecutionTime();
//        final List<Path> zipFiles = listPaths(dir);
//        if (LOGGER.isDebugEnabled()) {
//            LOGGER.debug("Listed zip files in {}", logExecutionTime.toString());
//        }
//        if (LOGGER.isTraceEnabled()) {
//            LOGGER.trace(zipFiles.toString());
//        }
//
//        final Iterator<Path> iterator = zipFiles.iterator();
//        int count = 0;
//        while (iterator.hasNext() && count < maxFileScan) {
//            final Path path = iterator.next();
//            processPath(path, stroomZipRepository, feedPaths, futures);
//            count++;
//        }
//
//        // Did we complete all?
//        return count < maxFileScan;
//    }
//
//    private void processPath(final Path path, final StroomZipRepository stroomZipRepository, final Map<String, List<Path>> feedPaths, final Set<CompletableFuture> futures) {
//        final Runnable runnable = () -> {
//            if (!taskContext.isTerminated()) {
//                LOGGER.debug("Processing file: {}", path);
//                final String feed = getFeed(stroomZipRepository, path);
//                if (feed == null || feed.length() == 0) {
//                    addErrorMessage(stroomZipRepository, path, "Unable to find feed in header??", true);
//
//                } else {
//                    LOGGER.debug("{} belongs to feed {}", path, feed);
//                    // Add the file into the map, creating the list if needs be
//                    feedPaths.computeIfAbsent(feed, k -> Collections.synchronizedList(new ArrayList<>())).add(path);
//                }
//            } else {
//                LOGGER.info("Quit processing at: {}", path);
//            }
//        };
//
//        futures.add(CompletableFuture.runAsync(runnable, executor));
//    }
//
//    private List<Path> listPaths(final Path dir) {
//        final List<Path> zipFiles = new ArrayList<>();
//        try {
//            if (dir != null && Files.isDirectory(dir)) {
//                try {
//                    Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new AbstractFileVisitor() {
//                        @Override
//                        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
//                            try {
//                                if (file.toString().endsWith(StroomZipRepository.ZIP_EXTENSION)) {
//                                    zipFiles.add(file);
//                                }
//                            } catch (final Exception e) {
//                                LOGGER.error(e.getMessage(), e);
//                            }
//
//                            if (zipFiles.size() < maxFileScan) {
//                                return FileVisitResult.CONTINUE;
//                            }
//
//                            return FileVisitResult.TERMINATE;
//                        }
//                    });
//                } catch (final IOException e) {
//                    LOGGER.error(e.getMessage(), e);
//                }
//            }
//        } catch (final Exception e) {
//            LOGGER.error(e.getMessage(), e);
//        }
//
//        // TODO : DO WE WANT TO SORT THESE FILES?
//        zipFiles.sort(Comparator.naturalOrder());
//
//        return zipFiles;
//    }
//
//    private String getFeed(final StroomZipRepository stroomZipRepository, final Path path) {
//        final MetaMap metaMap = getMetaMap(stroomZipRepository, path);
//        return metaMap.get(FEED);
//    }
//
//    private MetaMap getMetaMap(final StroomZipRepository stroomZipRepository, final Path path) {
//        final MetaMap metaMap = new MetaMap();
//        StroomZipFile stroomZipFile = null;
//        try {
//            stroomZipFile = new StroomZipFile(path);
//            final Set<String> baseNameSet = stroomZipFile.getStroomZipNameSet().getBaseNameSet();
//            if (baseNameSet.isEmpty()) {
//                stroomZipRepository.addErrorMessage(stroomZipFile, "Unable to find any entry??", true);
//            } else {
//                final String anyBaseName = baseNameSet.iterator().next();
//                final InputStream anyHeaderStream = stroomZipFile.getInputStream(anyBaseName, StroomZipFileType.Meta);
//
//                if (anyHeaderStream == null) {
//                    stroomZipRepository.addErrorMessage(stroomZipFile, "Unable to find header??", true);
//                } else {
//                    metaMap.read(anyHeaderStream, false);
//                }
//            }
//        } catch (final IOException ex) {
//            // Unable to open file ... must be bad.
//            stroomZipRepository.addErrorMessage(stroomZipFile, ex.getMessage(), true);
//            LOGGER.error("getMetaMap", ex);
//
//        } finally {
//            CloseableUtil.closeLogAndIgnoreException(stroomZipFile);
//        }
//
//        return metaMap;
//    }
//
//    private void addErrorMessage(final StroomZipRepository stroomZipRepository, final Path path, final String msg, final boolean bad) {
//        StroomZipFile stroomZipFile = null;
//        try {
//            stroomZipFile = new StroomZipFile(path);
//            stroomZipRepository.addErrorMessage(stroomZipFile, msg, bad);
//        } finally {
//            CloseableUtil.closeLogAndIgnoreException(stroomZipFile);
//        }
//    }

    interface FileFilter {
        boolean match(Path file, BasicFileAttributes attrs);
    }


}
