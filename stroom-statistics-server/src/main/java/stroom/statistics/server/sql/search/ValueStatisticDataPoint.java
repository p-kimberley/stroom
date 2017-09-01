

/*
 * Copyright 2017 Crown Copyright
 *
 * This file is part of Stroom-Stats.
 *
 * Stroom-Stats is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Stroom-Stats is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Stroom-Stats.  If not, see <http://www.gnu.org/licenses/>.
 */

package stroom.statistics.server.sql.search;

import com.google.common.collect.ImmutableMap;
import stroom.statistics.server.sql.StatisticTag;
import stroom.statistics.shared.StatisticStoreEntity;
import stroom.statistics.shared.StatisticType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;


/**
 * Value object to hold a statistic data point for a VALUE statistic as
 * retrieved from a statistic store.
 * This represents an aggregated value of 1-many statistic events
 */
public class ValueStatisticDataPoint implements StatisticDataPoint {

    private static final StatisticType STATISTIC_TYPE = StatisticType.VALUE;

    private static final Map<String, Function<ValueStatisticDataPoint, String>> FIELD_VALUE_FUNCTION_MAP;

    static {
        //hold a map of field names to functions that we get a value for that named field, converted to a string
        FIELD_VALUE_FUNCTION_MAP = ImmutableMap.<String, Function<ValueStatisticDataPoint, String>>builder()
                .put(StatisticStoreEntity.FIELD_NAME_COUNT, dataPoint -> Long.toString(dataPoint.count))
                .put(StatisticStoreEntity.FIELD_NAME_VALUE, dataPoint -> Double.toString(dataPoint.value))
                .build();
    }

    private final BasicStatisticDataPoint delegate;
    private final long count;
    private final double value;

    /**
     * Constructor for a value type statistic data point
     *
     * @param timeMs   The timestamp of the aggregated data point
     * @param tags     The list of tav/value pairs that qualify the data point
     * @param value    The mean value of the data point in this time period
     * @param count    The count of the number of statistic events that have happened
     *                 in this period
     * @return A populated {@link ValueStatisticDataPoint} instance
     */
    public ValueStatisticDataPoint(final long timeMs,
                                   final long precisionMs,
                                   final List<StatisticTag> tags,
                                   final long count,
                                   final double value) {

        this.delegate = new BasicStatisticDataPoint(timeMs, precisionMs, tags);
        this.count = count;
        this.value = value;
    }

    @Override
    public long getTimeMs() {
        return delegate.getTimeMs();
    }

    @Override
    public long getPrecisionMs() {
        return delegate.getPrecisionMs();
    }

    @Override
    public List<StatisticTag> getTags() {
        return delegate.getTags();
    }

    @Override
    public Map<String, String> getTagsAsMap() {
        return delegate.getTagsAsMap();
    }

    public long getCount() {
        return count;
    }

    public double getValue() {
        return value;
    }

    @Override
    public StatisticType getStatisticType() {
        return STATISTIC_TYPE;
    }

    @Override
    public String getFieldValue(final String fieldName) {
        Function<ValueStatisticDataPoint, String> fieldValueFunction = FIELD_VALUE_FUNCTION_MAP.get(fieldName);

        if (fieldValueFunction == null) {
            //we don't know what it is so see if the delegate does
            return delegate.getFieldValue(fieldName);
        } else {
            return fieldValueFunction.apply(this);
        }
    }

    @Override
    public String toString() {
        return "ValueStatisticDataPoint{" +
                "delegate=" + delegate +
                ", count=" + count +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ValueStatisticDataPoint that = (ValueStatisticDataPoint) o;

        if (count != that.count) return false;
        if (Double.compare(that.value, value) != 0) return false;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = delegate.hashCode();
        result = 31 * result + (int) (count ^ (count >>> 32));
        temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
