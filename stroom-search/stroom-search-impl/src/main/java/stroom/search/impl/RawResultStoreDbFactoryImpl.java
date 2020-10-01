package stroom.search.impl;

import stroom.lmdb.LmdbConfig;
import stroom.lmdb.LmdbEnvFactory;
import stroom.lmdb.bytebuffer.ByteBufferPool;
import stroom.lmdb.serdes.LongSerde;
import stroom.query.common.v2.RawResultStoreDb;
import stroom.query.common.v2.RawResultStoreDbFactory;
import stroom.query.common.v2.ValArraySerde;

import org.lmdbjava.Env;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;

@Singleton
public class RawResultStoreDbFactoryImpl implements RawResultStoreDbFactory {
    private final Env<ByteBuffer> lmdbEnvironment;
    private final ByteBufferPool byteBufferPool;

    @Inject
    RawResultStoreDbFactoryImpl(final LmdbEnvFactory lmdbEnvFactory,
                                final ByteBufferPool byteBufferPool,
                                final SearchConfig searchConfig) {
        final LmdbConfig lmdbConfig = searchConfig.getLmdbConfig();
        this.lmdbEnvironment = lmdbEnvFactory.create(lmdbConfig);
        this.byteBufferPool = byteBufferPool;
    }

    @Override
    public RawResultStoreDb create(final LongSerde keySerde,
                                   final ValArraySerde valueSerde,
                                   final String dbName) {
        return new RawResultStoreDb(
                lmdbEnvironment,
                byteBufferPool,
                keySerde,
                valueSerde,
                dbName);
    }
}
