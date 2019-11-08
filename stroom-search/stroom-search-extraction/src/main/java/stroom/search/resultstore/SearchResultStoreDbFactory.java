package stroom.search.resultstore;

import stroom.lmdb.util.ByteBufferPool;

public class SearchResultStoreDbFactory {

    public SearchResultStoreDb create(final LmdbEnvironment lmdbEnvironment) {
        final GKeySerde keySerde = new GKeySerde();
        final ItemSerde itemSerde = new ItemSerde();
        final ByteBufferPool byteBufferPool = new ByteBufferPool();
        SearchResultStoreDb searchResultStoreDb = new SearchResultStoreDb(lmdbEnvironment.getLmdbEnv(), byteBufferPool, keySerde, itemSerde);
        return searchResultStoreDb;
    }

}
