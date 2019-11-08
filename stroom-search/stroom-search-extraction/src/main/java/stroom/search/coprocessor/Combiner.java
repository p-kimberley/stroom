package stroom.search.coprocessor;

import stroom.dashboard.expression.v1.Generator;
import stroom.query.common.v2.Item;

public class Combiner {
    private final int[] depths;
    private final int maxDepth;

    public Combiner(final int[] depths, final int maxDepth) {
        this.depths = depths;
        this.maxDepth = maxDepth;
    }

    public Item combine(final Item existingValue, final Item addedValue) {
        Generator[] existingGenerators = existingValue.getGenerators();
        Generator[] addedGenerators = addedValue.getGenerators();
        for (int i = 0; i < depths.length; i++) {
            existingGenerators[i] = combine(depths[i], maxDepth, existingGenerators[i], addedGenerators[i], addedValue.getDepth());
        }
        return existingValue;
    }

    private Generator combine(final int groupDepth, final int maxDepth, final Generator existingValue,
                              final Generator addedValue, final int depth) {
        Generator output = null;

        if (maxDepth >= depth) {
            if (existingValue != null && addedValue != null) {
                existingValue.merge(addedValue);
                output = existingValue;
            } else if (groupDepth >= 0 && groupDepth <= depth) {
                // This field is grouped so output existing as it must match the
                // added value.
                output = existingValue;
            }
        } else {
            // This field is not grouped so output existing.
            output = existingValue;
        }

        return output;
    }
}