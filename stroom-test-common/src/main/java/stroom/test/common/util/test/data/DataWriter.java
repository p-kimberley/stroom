package stroom.test.common.util.test.data;

import java.util.List;
import java.util.stream.Stream;

public interface DataWriter {

    Stream<String> mapRecords(final List<Field> fieldDefinitions,
                              final Stream<Rec> recordStream);
}
