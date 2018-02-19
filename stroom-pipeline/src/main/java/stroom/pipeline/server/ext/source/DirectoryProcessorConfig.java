package stroom.pipeline.server.ext.source;

import stroom.pipeline.server.ext.source.DirectoryProcessor.FileFilter;
import stroom.query.api.v2.DocRef;

import java.nio.file.Path;

public class DirectoryProcessorConfig {
    private DocRef pipelineRef;
    private Path dir;
    private FileFilter fileFilter;
    private String processName;

    public DocRef getPipelineRef() {
        return pipelineRef;
    }

    public void setPipelineRef(final DocRef pipelineRef) {
        this.pipelineRef = pipelineRef;
    }

    public Path getDir() {
        return dir;
    }

    public void setDir(final Path dir) {
        this.dir = dir;
    }

    public FileFilter getFileFilter() {
        return fileFilter;
    }

    public void setFileFilter(final FileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(final String processName) {
        this.processName = processName;
    }
}
