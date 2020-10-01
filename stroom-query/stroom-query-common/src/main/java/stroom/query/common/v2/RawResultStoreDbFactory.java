package stroom.query.common.v2;

import stroom.lmdb.serdes.LongSerde;

public interface RawResultStoreDbFactory {
    RawResultStoreDb create(LongSerde keySerde, ValArraySerde valueSerde, String dbName);
}
