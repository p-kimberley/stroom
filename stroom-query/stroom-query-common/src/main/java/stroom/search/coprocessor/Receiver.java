package stroom.search.coprocessor;

import stroom.datasource.api.v2.AbstractField;

import java.util.function.Consumer;

public interface Receiver {
    Consumer<Values> getValuesConsumer();

    Consumer<Error> getErrorConsumer();

    Consumer<Long> getCompletionCountConsumer();

    AbstractField[] getFields();
}
