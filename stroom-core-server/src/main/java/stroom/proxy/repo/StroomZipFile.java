package stroom.proxy.repo;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.streamstore.server.fs.serializable.RASegmentInputStream;
import stroom.streamstore.server.fs.serializable.StreamSourceInputStream;
import stroom.streamstore.server.fs.serializable.StreamSourceInputStreamProvider;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;

public class StroomZipFile implements Closeable {
    private static final String SINGLE_ENTRY_ZIP_BASE_NAME = "001";

    public static final StroomZipEntry SINGLE_DATA_ENTRY = new StroomZipEntry(null, SINGLE_ENTRY_ZIP_BASE_NAME,
            StroomZipFileType.Data);
    public static final StroomZipEntry SINGLE_META_ENTRY = new StroomZipEntry(null, SINGLE_ENTRY_ZIP_BASE_NAME,
            StroomZipFileType.Meta);

    private static Logger LOGGER = LoggerFactory.getLogger(StroomZipFile.class);

    private final Path file;
    private ZipFile zipFile;
    private RuntimeException openStack;
    private StroomZipNameSet stroomZipNameSet;
//    private long totalSize = 0;

    public StroomZipFile(Path path) {
        this.file = path;
        openStack = new RuntimeException();
    }

    private ZipFile getZipFile() throws IOException {
        if (zipFile == null) {
            this.zipFile = new ZipFile(file.toFile());
        }
        return zipFile;
    }

    public Path getFile() {
        return file;
    }

    public StroomZipNameSet getStroomZipNameSet() throws IOException {
        if (stroomZipNameSet == null) {
            stroomZipNameSet = new StroomZipNameSet(false);
            Enumeration<ZipArchiveEntry> entryE = getZipFile().getEntries();

            while (entryE.hasMoreElements()) {
                ZipArchiveEntry entry = entryE.nextElement();

                // Skip Dir's
                if (!entry.isDirectory()) {
                    String fileName = entry.getName();
                    stroomZipNameSet.add(fileName);
                }

//                long entrySize = entry.getSize();
//                if (entrySize > 0) {
//                    totalSize += entrySize;
//                }

            }
        }
        return stroomZipNameSet;
    }

//    public Long getTotalSize() throws IOException {
//        getStroomZipNameSet();
//        if (totalSize == -1) {
//            return null;
//        } else {
//            return totalSize;
//        }
//    }

    @Override
    public void close() throws IOException {
        if (zipFile != null) {
            zipFile.close();
            zipFile = null;
        }
        stroomZipNameSet = null;

    }

    public StreamSourceInputStreamProvider getStreamProvider(long streamNo, String baseName, StroomZipFileType fileType) throws IOException {
        final ZipArchiveEntry entry = getEntry(baseName, fileType);
        if (entry != null) {
            return new BasicInputStreamProvider(streamNo, getZipFile().getInputStream(entry), entry.getSize());
        }
        return null;
    }

    public InputStream getInputStream(String baseName, StroomZipFileType fileType) throws IOException {
        final ZipArchiveEntry entry = getEntry(baseName, fileType);
        if (entry != null) {
            return getZipFile().getInputStream(entry);
        }
        return null;
    }

    public long getSize(String baseName, StroomZipFileType fileType) throws IOException {
        final ZipArchiveEntry entry = getEntry(baseName, fileType);
        if (entry != null) {
            return entry.getSize();
        }
        return 0;
    }

    public boolean containsEntry(String baseName, StroomZipFileType fileType) throws IOException {
        return getEntry(baseName, fileType) != null;
    }

    private ZipArchiveEntry getEntry(String baseName, StroomZipFileType fileType) throws IOException {
        final String fullName = getStroomZipNameSet().getName(baseName, fileType);
        if (fullName == null) {
            return null;
        }
        return getZipFile().getEntry(fullName);
    }

    void renameTo(Path newFileName) throws IOException {
        close();
        Files.move(file, newFileName);
    }

    void delete() throws IOException {
        close();
        Files.delete(file);
    }

    private static class BasicInputStreamProvider implements StreamSourceInputStreamProvider {
        private final long streamCount;
        private final StreamSourceInputStream inputStream;

        BasicInputStreamProvider(final long streamCount, final InputStream inputStream, final long size) {
            this.streamCount = streamCount;
            this.inputStream = new StreamSourceInputStream(inputStream, size);
        }

        @Override
        public long getStreamCount() {
            return streamCount;
        }

        @Override
        public StreamSourceInputStream getStream(final long streamNo) {
            return inputStream;
        }

        @Override
        public RASegmentInputStream getSegmentInputStream(final long streamNo) {
            return null;
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }
}
