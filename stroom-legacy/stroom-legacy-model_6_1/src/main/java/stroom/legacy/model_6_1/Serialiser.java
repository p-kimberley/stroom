package stroom.legacy.model_6_1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Deprecated
public interface Serialiser<D> {
    D read(InputStream inputStream, Class<D> clazz) throws IOException;

    void write(OutputStream outputStream, D document) throws IOException;

    void write(OutputStream outputStream, D document, boolean export) throws IOException;
}
