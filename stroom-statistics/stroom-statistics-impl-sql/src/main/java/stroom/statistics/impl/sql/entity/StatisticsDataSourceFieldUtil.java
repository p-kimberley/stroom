package stroom.statistics.impl.sql.entity;

import stroom.datasource.api.v2.AbstractField;
import stroom.datasource.api.v2.DateField;
import stroom.datasource.api.v2.LongField;
import stroom.datasource.api.v2.TextField;
import stroom.index.shared.IndexConstants;
import stroom.index.shared.IndexDoc;
import stroom.index.shared.IndexField;
import stroom.query.api.v2.ExpressionTerm.Condition;
import stroom.security.api.SecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.statistics.impl.sql.Statistics;
import stroom.statistics.impl.sql.shared.StatisticField;
import stroom.statistics.impl.sql.shared.StatisticStoreDoc;
import stroom.statistics.impl.sql.shared.StatisticType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StatisticsDataSourceFieldUtil {
    public static List<AbstractField> getDataSourceFields(final StatisticStoreDoc entity,
                                                          final Statistics statistics) {
        List<AbstractField> fields = new ArrayList<>();

        // TODO currently only BETWEEN is supported, but need to add support for
        // more conditions like >, >=, <, <=, =
        fields.add(new DateField(StatisticStoreDoc.FIELD_NAME_DATE_TIME, true, Collections.singletonList(Condition.BETWEEN)));

        // one field per tag
        if (entity.getConfig() != null) {
            final List<Condition> supportedConditions = Arrays.asList(Condition.EQUALS, Condition.IN);

            for (final StatisticField statisticField : entity.getStatisticFields()) {
                // TODO currently only EQUALS is supported, but need to add
                // support for more conditions like CONTAINS
                fields.add(new TextField(statisticField.getFieldName(), true, supportedConditions));
            }
        }

        fields.add(new LongField(StatisticStoreDoc.FIELD_NAME_COUNT, false, Collections.emptyList()));

        if (entity.getStatisticType().equals(StatisticType.VALUE)) {
            fields.add(new LongField(StatisticStoreDoc.FIELD_NAME_VALUE, false, Collections.emptyList()));
        }

        fields.add(new LongField(StatisticStoreDoc.FIELD_NAME_PRECISION_MS, false, Collections.emptyList()));

        // Filter fields.
        if (entity.getConfig() != null) {
            fields = statistics.getSupportedFields(fields);
        }

        return fields;
    }
}
