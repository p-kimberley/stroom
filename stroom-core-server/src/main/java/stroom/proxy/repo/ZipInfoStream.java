package stroom.proxy.repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.feed.MetaMap;
import stroom.task.server.TaskContext;
import stroom.util.date.DateUtil;
import stroom.util.io.AbstractFileVisitor;
import stroom.util.io.FileUtil;
import stroom.util.shared.ModelStringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ZipInfoStream implements Iterable<ZipInfoStream.ZipInfo> {
    private final Logger LOGGER = LoggerFactory.getLogger(ZipInfoStream.class);

    private final Path dir;
    private final TaskContext taskContext;
    private final Executor walkFileTreeExecutor;
    private final Executor processFileExecutor;
    private final FileFilter fileFilter;
    private final ErrorReceiver errorReceiver;
    private final int queueSize;

    ZipInfoStream(final Path dir, final TaskContext taskContext, final Executor walkFileTreeExecutor, final Executor processFileExecutor, final FileFilter fileFilter, final ErrorReceiver errorReceiver, final int queueSize) {
        this.dir = dir;
        this.taskContext = taskContext;
        this.walkFileTreeExecutor = walkFileTreeExecutor;
        this.processFileExecutor = processFileExecutor;
        this.fileFilter = fileFilter;
        this.errorReceiver = errorReceiver;
        this.queueSize = queueSize;
    }

    @Override
    public Iterator<ZipInfo> iterator() {
        Iterator<ZipInfo> iterator = Collections.emptyIterator();
        try {
            if (dir != null && Files.isDirectory(dir)) {
                iterator = createIterator();
            }
        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return iterator;
    }

    private Iterator<ZipInfo> createIterator() {
        final LinkedBlockingQueue<ZipInfo> queue = new LinkedBlockingQueue<>(queueSize);
        final Set<CompletableFuture> futures = Collections.newSetFromMap(new ConcurrentHashMap<>());

        final Runnable runnable = () -> {
            taskContext.setName("Walk Repository");
            taskContext.info(FileUtil.getCanonicalPath(dir));

            try {
                Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new AbstractFileVisitor() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                        try {
                            final Runnable runnable = () -> {
                                if (fileFilter.match(file, attrs)) {
                                    // Process the file to extract ZipInfo

                                    taskContext.setName("Extract Zip Info");
                                    taskContext.info(FileUtil.getCanonicalPath(file));

                                    try {
                                        if (!taskContext.isTerminated()) {
                                            final ZipInfo zipInfo = getZipInfo(file, attrs);
                                            if (zipInfo != null) {
                                                while (!queue.offer(zipInfo, 1, TimeUnit.SECONDS) && !taskContext.isTerminated()) {
                                                    if (LOGGER.isTraceEnabled()) {
                                                        LOGGER.trace("Offering: " + file);
                                                    }
                                                }
                                            }
                                        }
                                    } catch (final InterruptedException e) {
                                        if (Thread.interrupted()) {
                                            LOGGER.info("Thread was interrupted");
                                        }
                                    }
                                }
                            };
                            final CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(runnable, processFileExecutor);
                            completableFuture.thenAccept(r -> futures.remove(completableFuture));
                            futures.add(completableFuture);
                        } catch (final RuntimeException e) {
                            LOGGER.error(e.getMessage(), e);
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (final IOException | RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }

            // Wait for all inner tasks to complete.
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
        };
        final CompletableFuture completableFuture = CompletableFuture.runAsync(runnable, walkFileTreeExecutor);

        return new Iterator<ZipInfo>() {
            @Override
            public boolean hasNext() {
                final boolean complete = completableFuture.isDone();
                return queue.peek() != null || !complete;
            }

            @Override
            public ZipInfo next() {
                try {
                    return queue.poll(1, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    if (Thread.interrupted()) {
                        LOGGER.info("Thread was interrupted");
                    }
                }

                return null;
            }
        };
    }

    private ZipInfo getZipInfo(final Path path, final BasicFileAttributes attrs) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Getting zip info for  '" + FileUtil.getCanonicalPath(path) + "'");
        }

        MetaMap metaMap = null;
        long totalUncompressedSize = 0;

        try (final StroomZipFile stroomZipFile = new StroomZipFile(path)) {
            final Set<String> baseNameSet = stroomZipFile.getStroomZipNameSet().getBaseNameSet();
            if (baseNameSet.isEmpty()) {
                errorReceiver.onError(path, "Unable to find any entry?");
            } else {
                for (final String sourceName : baseNameSet) {
                    // Extract meta data
                    if (metaMap == null) {
                        try {
                            final InputStream metaStream = stroomZipFile.getInputStream(sourceName, StroomZipFileType.Meta);
                            if (metaStream == null) {
                                errorReceiver.onError(path, "Unable to find meta?");
                            } else {
                                metaMap = new MetaMap();
                                metaMap.read(metaStream, false);
                            }
                        } catch (final RuntimeException e) {
                            errorReceiver.onError(path, e.getMessage());
                            LOGGER.error(e.getMessage(), e);
                        }
                    }

                    totalUncompressedSize += getRawEntrySize(stroomZipFile, sourceName, StroomZipFileType.Meta);
                    totalUncompressedSize += getRawEntrySize(stroomZipFile, sourceName, StroomZipFileType.Context);
                    totalUncompressedSize += getRawEntrySize(stroomZipFile, sourceName, StroomZipFileType.Data);
                }
            }
        } catch (final IOException | RuntimeException e) {
            // Unable to open file ... must be bad.
            errorReceiver.onError(path, e.getMessage());
            LOGGER.error(e.getMessage(), e);
        }

        // Get compressed size.
        Long totalCompressedSize = null;
        try {
            totalCompressedSize = Files.size(path);
        } catch (final IOException | RuntimeException e) {
            errorReceiver.onError(path, e.getMessage());
            LOGGER.error(e.getMessage(), e);
        }

        final ZipInfo zipInfo = new ZipInfo(path, metaMap, totalUncompressedSize, totalCompressedSize, attrs.lastModifiedTime().toMillis());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Zip info for  '" + FileUtil.getCanonicalPath(path) + "' is " + zipInfo);
        }

        return zipInfo;
    }

    private long getRawEntrySize(final StroomZipFile stroomZipFile,
                                 final String sourceName,
                                 final StroomZipFileType fileType)
            throws IOException {
        final long size = stroomZipFile.getSize(sourceName, fileType);
        if (size == -1) {
            throw new IOException("Unknown raw file size");
        }

        return size;
    }

    interface ErrorReceiver {
        void onError(Path path, String message);
    }

    static class ZipInfo {
        private final Path path;
        private final MetaMap metaMap;
        private final Long uncompressedSize;
        private final Long compressedSize;
        private final Long lastModified;

        ZipInfo(final Path path, final MetaMap metaMap, final Long uncompressedSize, final Long compressedSize, final Long lastModified) {
            this.path = path;
            this.metaMap = metaMap;
            this.uncompressedSize = uncompressedSize;
            this.compressedSize = compressedSize;
            this.lastModified = lastModified;
        }

        public Path getPath() {
            return path;
        }

        public MetaMap getMetaMap() {
            return metaMap;
        }

        public Long getUncompressedSize() {
            return uncompressedSize;
        }

        public Long getCompressedSize() {
            return compressedSize;
        }

        public Long getLastModified() {
            return lastModified;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(FileUtil.getCanonicalPath(path));
            if (metaMap != null) {
                sb.append("\n\tmetaMap=");
                sb.append(metaMap);
            }
            if (uncompressedSize != null) {
                sb.append("\n\tuncompressedSize=");
                sb.append(ModelStringUtil.formatIECByteSizeString(uncompressedSize));
            }
            if (compressedSize != null) {
                sb.append("\n\tcompressedSize=");
                sb.append(ModelStringUtil.formatIECByteSizeString(compressedSize));
            }
            if (lastModified != null) {
                sb.append("\n\tlastModified=");
                sb.append(DateUtil.createNormalDateTimeString(lastModified));
            }
            return sb.toString();
        }
    }

    interface FileFilter {
        boolean match(Path file, BasicFileAttributes attrs);
    }
}
