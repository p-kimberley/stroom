package stroom.lmdb;

import com.google.common.base.Preconditions;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class LmdbKeyValueStore implements KeyValueStore, Closeable {

    public static final int VALUE_BUFFER_SIZE = 700;

    private final Path dir;
    private final String dbName;
    private final Env<ByteBuffer> env;
    private final Dbi<ByteBuffer> db;

    public LmdbKeyValueStore(final String dbName,
                             final Path dir) {
        this.dbName = dbName;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating directory %s", dir.toAbsolutePath()), e);
        }
        this.dir = dir;

        env = Env.<ByteBuffer>create()
                .setMapSize(10_485_760)
                .setMaxDbs(1)
                .open(dir.toFile());

        db = env.openDbi(dbName, DbiFlags.MDB_CREATE);
    }

    @Override
    public void put(final String key, final String value) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(value);

        final ByteBuffer keyBuf = stringToBuffer(key, env.getMaxKeySize());
        final ByteBuffer valueBuf = stringToBuffer(value, VALUE_BUFFER_SIZE);

        db.put(keyBuf, valueBuf);
    }

    @Override
    public Optional<String> get(final String key) {
        Preconditions.checkNotNull(key);
        ByteBuffer keyBuf = stringToBuffer(key, env.getMaxKeySize());

        try (Txn<ByteBuffer> txn = env.txnRead()) {
            final ByteBuffer valBuffer = db.get(txn, keyBuf);

            return Optional
                    .ofNullable(valBuffer)
                    .map(this::byteBufferToString);
        }
    }

    @Override
    public void close() throws IOException {
        if (env != null) {
            try {
                env.close();
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error closing LMDB env"), e);
            }
        }
    }

    private ByteBuffer stringToBuffer(final String str, final int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        buffer.put(str.getBytes(StandardCharsets.UTF_8)).flip();
        return buffer;
    }

    private String byteBufferToString(final ByteBuffer byteBuffer) {
        Preconditions.checkNotNull(byteBuffer);
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }
}
