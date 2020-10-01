package stroom.search.coprocessor;

import stroom.datasource.api.v2.AbstractField;

import java.util.Optional;

public class FieldUtil {
    public static String[] getFieldNames(final AbstractField[] fields) {
        final String[] fieldNames = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            final AbstractField field = fields[i];
            fieldNames[i] = field.getName();
        }
        return fieldNames;
    }

    public static AbstractField getFieldByName(final AbstractField[] fields, final String fieldName) {
        for (final AbstractField field : fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public static int getFieldIndex(final AbstractField[] fields, final String fieldName) {
        for (int i = 0; i < fields.length; i++) {
            final AbstractField field = fields[i];
            if (field.getName().equals(fieldName)) {
                return i;
            }
        }
        return -1;
    }

    public static Optional<Integer> getOptFieldIndexPosition(final AbstractField[] fields, final String fieldName) {
        final int index = getFieldIndex(fields, fieldName);
        if (index == -1) {
            return Optional.empty();
        }
        return Optional.of(index);
    }
}
