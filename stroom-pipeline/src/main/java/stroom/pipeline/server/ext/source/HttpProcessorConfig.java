package stroom.pipeline.server.ext.source;

import stroom.query.api.v2.DocRef;

public class HttpProcessorConfig {
    private DocRef pipelineRef;

    public DocRef getPipelineRef() {
        return pipelineRef;
    }

    public void setPipelineRef(final DocRef pipelineRef) {
        this.pipelineRef = pipelineRef;
    }
}
