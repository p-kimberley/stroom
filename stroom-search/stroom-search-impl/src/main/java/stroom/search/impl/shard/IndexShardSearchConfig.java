package stroom.search.impl.shard;

import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import javax.inject.Singleton;

@Singleton
public class IndexShardSearchConfig extends AbstractConfig {

    private static final int DEFAULT_MAX_THREADS = 4;
    private static final int DEFAULT_MAX_THREADS_PER_TASK = 2;

    private int maxDocIdQueueSize = 1000000;
    private int maxThreads = DEFAULT_MAX_THREADS;
    private int maxThreadsPerTask = DEFAULT_MAX_THREADS_PER_TASK;
    private CacheConfig searchResultCache = CacheConfig.builder()
            .maximumSize(10000L)
            .expireAfterAccess(StroomDuration.ofMinutes(10))
            .build();
    private CacheConfig indexShardSearcherCache = CacheConfig.builder()
            .maximumSize(2L)
            .expireAfterAccess(StroomDuration.ofMinutes(1))
            .build();

    @JsonPropertyDescription("The maximum number of doc ids that will be queued ready for stored data to be " +
            "retrieved from the index shard")
    public int getMaxDocIdQueueSize() {
        return maxDocIdQueueSize;
    }

    public void setMaxDocIdQueueSize(final int maxDocIdQueueSize) {
        this.maxDocIdQueueSize = maxDocIdQueueSize;
    }

    @JsonPropertyDescription("The absolute maximum number of threads per node, used to search Lucene index " +
            "shards across all searches")
    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(final int maxThreads) {
        this.maxThreads = maxThreads;
    }

    @JsonPropertyDescription("The maximum number of threads per search, per node, used to search Lucene index shards")
    public int getMaxThreadsPerTask() {
        return maxThreadsPerTask;
    }

    public void setMaxThreadsPerTask(final int maxThreadsPerTask) {
        this.maxThreadsPerTask = maxThreadsPerTask;
    }

    public CacheConfig getSearchResultCache() {
        return searchResultCache;
    }

    public void setSearchResultCache(final CacheConfig searchResultCache) {
        this.searchResultCache = searchResultCache;
    }

    public CacheConfig getIndexShardSearcherCache() {
        return indexShardSearcherCache;
    }

    public void setIndexShardSearcherCache(final CacheConfig indexShardSearcherCache) {
        this.indexShardSearcherCache = indexShardSearcherCache;
    }

    @Override
    public String toString() {
        return "IndexShardSearchConfig{" +
                "maxDocIdQueueSize=" + maxDocIdQueueSize +
                ", maxThreads=" + maxThreads +
                ", maxThreadsPerTask=" + maxThreadsPerTask +
                ", searchResultCache=" + searchResultCache +
                ", indexShardSearcherCache=" + indexShardSearcherCache +
                '}';
    }
}
