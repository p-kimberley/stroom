package stroom.search.resultstore;

import stroom.dashboard.expression.v1.Generator;

public final class Generators {
    private final Generator[] generators;

    public Generators(final Generator[] generators) {
        this.generators = generators;
    }

    public Generator[] getGenerators() {
        return generators;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final Generator value : generators) {
            if (value != null) {
                try {
                    sb.append(value.eval().toString());
                } catch (Exception e) {
                    // if the evaluation of the generator fails record the class of the exception
                    // so we can see which one has a problem
                    sb.append(e.getClass().getCanonicalName());
                }
            } else {
                sb.append("null");
            }
            sb.append("\t");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
