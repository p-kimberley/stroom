package stroom.search.coprocessor;

import stroom.query.common.v2.Coprocessor;
import stroom.query.common.v2.CoprocessorFactory;
import stroom.query.common.v2.CoprocessorSettings;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CoprocessorsFactory {
    private final CoprocessorFactory coprocessorFactory;

    @Inject
    CoprocessorsFactory(final CoprocessorFactory coprocessorFactory) {
        this.coprocessorFactory = coprocessorFactory;
    }

    public Coprocessors create(final List<CoprocessorSettings> settingsList, final Consumer<Error> errorConsumer) {
        final Set<NewCoprocessor> coprocessors = new HashSet<>();
        if (settingsList != null) {
            for (final CoprocessorSettings settings : settingsList) {
                final Coprocessor coprocessor = coprocessorFactory.create(settings);

                if (coprocessor != null) {
                    final NewCoprocessor newCoprocessor =
                            new NewCoprocessor(settings.getCoprocessorId(), settings, errorConsumer, coprocessor);
                    coprocessors.add(newCoprocessor);
                }
            }
        }

        return new Coprocessors(coprocessors);
    }
}
