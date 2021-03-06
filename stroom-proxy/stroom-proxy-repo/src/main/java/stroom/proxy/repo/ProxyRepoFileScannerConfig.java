package stroom.proxy.repo;

import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Duration;
import javax.inject.Singleton;

@Singleton
@JsonPropertyOrder({
        "scanningEnabled",
        "scanFrequency"
})
public class ProxyRepoFileScannerConfig {

    private boolean scanningEnabled;
    private StroomDuration scanFrequency = StroomDuration.of(Duration.ofSeconds(10));

    @JsonProperty
    public boolean isScanningEnabled() {
        return scanningEnabled;
    }

    public void setScanningEnabled(final boolean scanningEnabled) {
        this.scanningEnabled = scanningEnabled;
    }

    @JsonProperty
    public StroomDuration getScanFrequency() {
        return scanFrequency;
    }

    public void setScanFrequency(final StroomDuration scanFrequency) {
        this.scanFrequency = scanFrequency;
    }
}
