package stroom.query.common.v2;

import stroom.dashboard.expression.v1.Val;
import stroom.lmdb.AbstractLmdbDb;
import stroom.lmdb.LmdbUtils;
import stroom.lmdb.serdes.LongSerde;
import stroom.lmdb.bytebuffer.ByteBufferPool;
import stroom.lmdb.bytebuffer.PooledByteBuffer;
import stroom.util.logging.LogUtil;

import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.KeyRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class RawResultStoreDb extends AbstractLmdbDb<Long, Val[]> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawResultStoreDb.class);

    public RawResultStoreDb(final Env<ByteBuffer> lmdbEnvironment,
                            final ByteBufferPool byteBufferPool,
                            final LongSerde keySerde,
                            final ValArraySerde valueSerde,
                            final String dbName) {
        super(lmdbEnvironment, byteBufferPool, keySerde, valueSerde, dbName);
    }

    public byte[] getRange(final long from,
                           final long to) {
        try (final PooledByteBuffer pooledByteBuffer = getByteBufferPool().getPooledByteBuffer(1000)) {
            final ByteBuffer byteBuffer = pooledByteBuffer.getByteBuffer();
            getRange(from, to, byteBuffer);
            byteBuffer.flip();
            final byte[] arr = new byte[byteBuffer.remaining()];
            byteBuffer.get(arr);
            return arr;
        }
    }

    public void getRange(final long from,
                         final long to,
                         final ByteBuffer output) {
        ByteBuffer start = getKeySerde().serialize(from);
        ByteBuffer stop = getKeySerde().serialize(to);

        LmdbUtils.doWithReadTxn(getLmdbEnvironment(), txn ->
                forEachEntryAsBytes(txn, KeyRange.closed(start, stop), keyVal -> {
                    final ByteBuffer byteBuffer = keyVal.val();
//                    final byte[] bytesArray = new byte[byteBuffer.remaining()];
//                    byteBuffer.get(bytesArray, 0, bytesArray.length);
                    output.put(byteBuffer);
//                    byteBuffer.rewind();
                }));
    }

    @Override
    protected Dbi<ByteBuffer> openDbi(final Env<ByteBuffer> env, final String name) {
        LOGGER.debug("Opening LMDB database with name: {}", name);
        try {
            return env.openDbi(name, DbiFlags.MDB_CREATE, DbiFlags.MDB_INTEGERKEY);
        } catch (Exception e) {
            throw new RuntimeException(LogUtil.message("Error opening LMDB database {}", name), e);
        }
    }
}
