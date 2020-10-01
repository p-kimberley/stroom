package stroom.lmdb;

import stroom.util.io.PathCreator;
import stroom.util.io.TempDirProvider;
import stroom.util.logging.LogUtil;

import org.apache.hadoop.hbase.util.Bytes;
import org.lmdbjava.Env;
import org.lmdbjava.EnvFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;

public class LmdbEnvFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(LmdbEnvFactory.class);

    // These are dups of org.lmdbjava.Library.LMDB_* but that class is pkg private for some reason.
    private static final String LMDB_EXTRACT_DIR_PROP = "lmdbjava.extract.dir";
    private static final String LMDB_NATIVE_LIB_PROP = "lmdbjava.native.lib";
    private static final String DEFAULT_STORE_SUB_DIR_NAME = "refDataOffHeapStore";

    private final TempDirProvider tempDirProvider;
    private final PathCreator pathCreator;

    @Inject
    public LmdbEnvFactory(final TempDirProvider tempDirProvider,
                          final PathCreator pathCreator) {
        this.tempDirProvider = tempDirProvider;
        this.pathCreator = pathCreator;
    }

    public Env<ByteBuffer> create(final LmdbConfig config) {
            final Path dbDir = getStoreDir(config);
            LOGGER.info(
                    "Creating RefDataOffHeapStore environment with [maxSize: {}, dbDir {}, maxReaders {}, " +
                            "maxPutsBeforeCommit {}, isReadAheadEnabled {}]",
                    config.getMaxStoreSize(),
                    dbDir.toAbsolutePath().toString() + File.separatorChar,
                    config.getMaxReaders(),
                    config.getMaxPutsBeforeCommit(),
                    config.isReadAheadEnabled());

            // By default LMDB opens with readonly mmaps so you cannot mutate the bytebuffers inside a txn.
            // Instead you need to create a new bytebuffer for the value and put that. If you want faster writes
            // then you can use EnvFlags.MDB_WRITEMAP in the open() call to allow mutation inside a txn but that
            // comes with greater risk of corruption.

            // NOTE on setMapSize() from LMDB author found on https://groups.google.com/forum/#!topic/caffe-users/0RKsTTYRGpQ
            // On Windows the OS sets the filesize equal to the mapsize. (MacOS requires that too, and allocates
            // all of the physical space up front, it doesn't support sparse files.) The mapsize should not be
            // hardcoded into software, it needs to be reconfigurable. On Windows and MacOS you really shouldn't
            // set it larger than the amount of free space on the filesystem.

            final EnvFlags[] envFlags;
            if (config.isReadAheadEnabled()) {
                envFlags = new EnvFlags[0];
            } else {
                envFlags = new EnvFlags[]{EnvFlags.MDB_NORDAHEAD};
            }

            final String lmdbSystemLibraryPath = config.getLmdbSystemLibraryPath();

            if (lmdbSystemLibraryPath != null) {
                // javax.validation should ensure the path is valid if set
                System.setProperty(LMDB_NATIVE_LIB_PROP, lmdbSystemLibraryPath);
                LOGGER.info("Using provided LMDB system library file " + lmdbSystemLibraryPath);
            } else {
                // Set the location to extract the bundled LMDB binary to
                System.setProperty(LMDB_EXTRACT_DIR_PROP, dbDir.toAbsolutePath().toString());
                LOGGER.info("Extracting bundled LMDB binary to " + dbDir);
            }

            final Env<ByteBuffer> env = Env.create()
                    .setMaxReaders(config.getMaxReaders())
                    .setMapSize(config.getMaxStoreSize().getBytes())
                    .setMaxDbs(7) //should equal the number of DBs we create which is fixed at compile time
                    .open(dbDir.toFile(), envFlags);

            LOGGER.info("Existing databases: [{}]",
                    env.getDbiNames()
                            .stream()
                            .map(Bytes::toString)
                            .collect(Collectors.joining(",")));
            return env;
    }

    public Path getStoreDir(final LmdbConfig config) {
        String storeDirStr = pathCreator.replaceSystemProperties(config.getLocalDir());
        Path storeDir;
        if (storeDirStr == null) {
            LOGGER.info("Off heap store dir is not set, falling back to {}", tempDirProvider.get());
            storeDir = tempDirProvider.get();
            Objects.requireNonNull(storeDir, "Temp dir is not set");
            storeDir = storeDir.resolve(DEFAULT_STORE_SUB_DIR_NAME);
        } else {
            storeDirStr = pathCreator.replaceSystemProperties(storeDirStr);
            storeDir = Paths.get(storeDirStr);
        }

        try {
            LOGGER.debug("Ensuring directory {}", storeDir);
            Files.createDirectories(storeDir);
        } catch (IOException e) {
            throw new RuntimeException(LogUtil.message("Error ensuring store directory {} exists", storeDirStr), e);
        }

        return storeDir;
    }
}
