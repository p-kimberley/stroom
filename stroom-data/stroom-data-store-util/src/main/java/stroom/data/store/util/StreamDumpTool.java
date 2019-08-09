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

package stroom.data.store.util;

import com.google.inject.Injector;
import stroom.data.store.api.Store;
import stroom.data.store.impl.DataDownloadSettings;
import stroom.data.store.impl.DataDownloadTask;
import stroom.data.store.impl.DataDownloadTaskHandler;
import stroom.meta.shared.FindMetaCriteria;
import stroom.meta.shared.MetaFieldNames;
import stroom.meta.shared.MetaService;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.security.api.Security;
import stroom.security.api.UserTokenUtil;
import stroom.task.api.SimpleTaskContext;
import stroom.task.api.TaskContext;
import stroom.util.AbstractCommandLineTool;
import stroom.util.io.BufferFactory;
import stroom.util.logging.LogUtil;
import stroom.util.shared.ModelStringUtil;
import stroom.util.shared.Sort.Direction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Handy tool to dump out content.
 */
public class StreamDumpTool extends AbstractCommandLineTool {
    private final ToolInjector toolInjector = new ToolInjector();

    private String feed;
    private String streamType;
    private String createPeriodFrom;
    private String createPeriodTo;
    private String outputDir;
    private String format;

    public static void main(final String[] args) {
        new StreamDumpTool().doMain(args);
    }

    public void setFeed(final String feed) {
        this.feed = feed;
    }

    public void setStreamType(final String streamType) {
        this.streamType = streamType;
    }

    public void setCreatePeriodFrom(final String createPeriodFrom) {
        this.createPeriodFrom = createPeriodFrom;
    }

    public void setCreatePeriodTo(final String createPeriodTo) {
        this.createPeriodTo = createPeriodTo;
    }

    public void setOutputDir(final String outputDir) {
        this.outputDir = outputDir;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    @Override
    public void run() {
        // Boot up Guice
        process(toolInjector.getInjector());
    }

    private void process(final Injector injector) {
        if (outputDir == null || outputDir.length() == 0) {
            throw new RuntimeException("Output directory must be specified");
        }

        final Path dir = Paths.get(outputDir);
        if (!Files.isDirectory(dir)) {
            System.out.println("Creating directory '" + outputDir + "'");
            try {
                Files.createDirectories(dir);
            } catch (final IOException e) {
                throw new RuntimeException("Unable to create output directory '" + outputDir + "'");
            }
        }

        try (final Stream<Path> stream = Files.list(dir)) {
            if (stream.count() > 0) {
                throw new RuntimeException("The output dir must be empty");
            }
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (format == null || format.trim().length() == 0) {
            System.out.println("Using default output format: \"${feed}/${pathId}/${id}\"");
            format = "${feed}/${pathId}/${id}";
        }

        final TaskContext taskContext = new SimpleTaskContext() {
            @Override
            public void info(final Object... args) {
                final Object[] otherArgs = new Object[args.length - 1];
                System.arraycopy(args, 1, otherArgs, 0, otherArgs.length);
                System.out.println(LogUtil.message((String) args[0], otherArgs));
            }
        };
        final Store streamStore = injector.getInstance(Store.class);
        final MetaService metaService = injector.getInstance(MetaService.class);
        final Security security = injector.getInstance(Security.class);
        final BufferFactory bufferFactory = () -> new byte[4096];
        final DataDownloadTaskHandler streamDownloadTaskHandler = new DataDownloadTaskHandler(taskContext, streamStore, metaService, security, bufferFactory);

        download(feed, streamType, createPeriodFrom, createPeriodTo, dir, format, streamDownloadTaskHandler);

        System.out.println("Finished dumping streams");
    }

    private void download(final String feedName,
                          final String streamType,
                          final String createPeriodFrom,
                          final String createPeriodTo,
                          final Path dir,
                          final String format,
                          final DataDownloadTaskHandler dataDownloadTaskHandler) {
        System.out.println("Dumping data for " + feedName);

        final ExpressionOperator.Builder builder = new ExpressionOperator.Builder(Op.AND);

        if (createPeriodFrom != null && !createPeriodFrom.isEmpty() && createPeriodTo != null && !createPeriodTo.isEmpty()) {
            builder.addTerm(MetaFieldNames.CREATE_TIME, Condition.BETWEEN, createPeriodFrom + "," + createPeriodTo);
        } else if (createPeriodFrom != null && !createPeriodFrom.isEmpty()) {
            builder.addTerm(MetaFieldNames.CREATE_TIME, Condition.GREATER_THAN_OR_EQUAL_TO, createPeriodFrom);
        } else if (createPeriodTo != null && !createPeriodTo.isEmpty()) {
            builder.addTerm(MetaFieldNames.CREATE_TIME, Condition.LESS_THAN_OR_EQUAL_TO, createPeriodTo);
        }

        if (feedName != null) {
            builder.addTerm(MetaFieldNames.FEED_NAME, Condition.EQUALS, feedName);
        }

        if (streamType != null) {
            builder.addTerm(MetaFieldNames.TYPE_NAME, Condition.EQUALS, streamType);
        }

        final FindMetaCriteria criteria = new FindMetaCriteria();
        criteria.setExpression(builder.build());
        criteria.addSort(FindMetaCriteria.FIELD_FEED, Direction.ASCENDING, true);
        criteria.addSort(FindMetaCriteria.FIELD_ID, Direction.ASCENDING, true);

        final DataDownloadSettings dataDownloadSettings = new DataDownloadSettings();
        dataDownloadSettings.setMultipleFiles(true);
        dataDownloadSettings.setMaxFileSize(ModelStringUtil.parseIECByteSizeString("2G"));
        dataDownloadSettings.setMaxFileParts(10000L);

        final DataDownloadTask streamDownloadTask = new DataDownloadTask(UserTokenUtil.processingUser(), criteria, dir, format, dataDownloadSettings);
        dataDownloadTaskHandler.exec(streamDownloadTask);


//        final StreamStore streamStore = appContext.getBean(StreamStore.class);
//        final FeedService feedService = (FeedService) appContext.getBean("cachedFeedService");
//        final StreamTypeService streamTypeService = (StreamTypeService) appContext.getBean("cachedStreamTypeService");
//
//        new ThreadScopeRunnable() {
//            @Override
//            protected void exec() {
//                Feed definition = null;
//                if (feed != null) {
//                    definition = feedService.loadByName(feed);
//                    if (definition == null) {
//                        throw new RuntimeException("Unable to locate Feed " + feed);
//                    }
//                    criteria.obtainFeeds().obtainInclude().add(definition.getId());
//                }
//
//                if (streamType != null) {
//                    final StreamType type = streamTypeService.loadByName(streamType);
//                    if (type == null) {
//                        throw new RuntimeException("Unable to locate stream type " + streamType);
//                    }
//                    criteria.obtainStreamTypeIdSet().add(type.getId());
//                } else {
//                    criteria.obtainStreamTypeIdSet().add(StreamType.RAW_EVENTS.getId());
//                }
//
//                // Query the stream store
//                final List<Stream> results = streamStore.find(criteria);
//                System.out.println("Starting dump of " + results.size() + " streams");
//
//                int count = 0;
//                for (final Stream stream : results) {
//                    count++;
//                    processFile(count, results.size(), streamStore, stream.getId(), dir);
//                }
//
//                System.out.println("Finished dumping " + results.size() + " streams");
//            }
//        }.run();
    }

//    /**
//     * Scan a file
//     */
//    private void processFile(final int count, final int total, final Store streamStore, final long streamId,
//                             final Path outputDir) {
//        try (final Source streamSource = streamStore.openSource(streamId)) {
//            try (final InputStreamProvider inputStreamProvider = streamSource.get(0)) {
//                try (final InputStream inputStream = inputStreamProvider.get()) {
//                    final Path outputFile = outputDir.resolve(streamId + ".dat");
//                    System.out.println(
//                            "Dumping stream " + count + " of " + total + " to file '" + FileUtil.getCanonicalPath(outputFile) + "'");
//                    StreamUtil.streamToFile(inputStream, outputFile);
//                } catch (final RuntimeException e) {
//                    e.printStackTrace();
//                }
//            }
//        } catch (final IOException | RuntimeException e) {
//            e.printStackTrace();
//        }
//    }
}