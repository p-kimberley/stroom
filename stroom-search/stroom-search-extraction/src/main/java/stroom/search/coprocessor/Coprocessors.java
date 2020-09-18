package stroom.search.coprocessor;

import stroom.query.common.v2.Payload;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Coprocessors {
    private final Set<NewCoprocessor> set;

    Coprocessors(final Set<NewCoprocessor> set) {
        this.set = set;
    }

    public List<Payload> createPayloads() {
        // Produce payloads for each coprocessor.
        List<Payload> payloads = null;
        if (set != null && set.size() > 0) {
            for (final NewCoprocessor coprocessor : set) {
                final Payload payload = coprocessor.createPayload();
                if (payload != null) {
                    if (payloads == null) {
                        payloads = new ArrayList<>();
                    }

                    payloads.add(payload);
                }
            }
        }
        return payloads;
    }

    public int size() {
        return set.size();
    }

    public Set<NewCoprocessor> getSet() {
        return set;
    }
}
