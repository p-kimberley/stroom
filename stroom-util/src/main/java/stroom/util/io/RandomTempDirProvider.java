package stroom.util.io;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class RandomTempDirProvider implements TempDirProvider {
    private final Path tempDir;

    @Inject
    public RandomTempDirProvider() throws IOException {
        this.tempDir = Files.createTempDirectory("stroom");
    }

    @Override
    public Path get() {
        return tempDir;
    }
}
