package stroom.pipeline.refdata;

import stroom.lmdb.LmdbConfig;
import stroom.util.cache.CacheConfig;
import stroom.util.shared.AbstractConfig;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.inject.Singleton;

@Singleton
public class ReferenceDataConfig extends AbstractConfig {
    private LmdbConfig lmdbConfig;
    private CacheConfig effectiveStreamCache = new CacheConfig.Builder()
            .maximumSize(1000L)
            .expireAfterAccess(StroomDuration.ofMinutes(10))
            .build();

    public ReferenceDataConfig() {
        lmdbConfig = new LmdbConfig();
        lmdbConfig.setLocalDir("${stroom.temp}/lmdb/refDataOffHeapStore");
    }

    @JsonProperty("lmdb")
    public LmdbConfig getLmdbConfig() {
        return lmdbConfig;
    }

    public void setLmdbConfig(final LmdbConfig lmdbConfig) {
        this.lmdbConfig = lmdbConfig;
    }

    public CacheConfig getEffectiveStreamCache() {
        return effectiveStreamCache;
    }

    public void setEffectiveStreamCache(final CacheConfig effectiveStreamCache) {
        this.effectiveStreamCache = effectiveStreamCache;
    }

    @Override
    public String toString() {
        return "ReferenceDataConfig{" +
                "lmdbConfig=" + lmdbConfig +
                ", effectiveStreamCache=" + effectiveStreamCache +
                '}';
    }
}
