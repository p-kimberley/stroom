/*
 * Copyright 2026 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.query.shared;

import stroom.docref.DocRef;
import stroom.query.api.DateTimeSettings;
import stroom.query.api.TimeRange;
import stroom.query.api.datasource.QueryField;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/// A request for the most common values of a field in a data source, for the value picker of an
/// expression term.
@JsonPropertyOrder(alphabetic = true)
@JsonInclude(Include.NON_NULL)
public class FetchFieldValuesRequest {

    @NotNull
    @JsonProperty
    private final DocRef dataSource;
    @NotNull
    @JsonProperty
    private final QueryField field;
    @JsonProperty
    private final String filter;
    @JsonProperty
    private final Boolean caseInsensitive;
    @JsonProperty
    private final TimeRange timeRange;
    @JsonProperty
    private final DateTimeSettings dateTimeSettings;

    /// @param dataSource       The data source to read values from.
    /// @param field            The field to read values of.
    /// @param filter           Text the values must contain, or null for all values.
    /// @param caseInsensitive  Whether the filter ignores case.
    /// @param timeRange        The time range to count values in, or null for all time.
    /// @param dateTimeSettings The settings used to resolve relative times in the time range.
    @JsonCreator
    public FetchFieldValuesRequest(@JsonProperty("dataSource") final DocRef dataSource,
                                   @JsonProperty("field") final QueryField field,
                                   @JsonProperty("filter") final String filter,
                                   @JsonProperty("caseInsensitive") final Boolean caseInsensitive,
                                   @JsonProperty("timeRange") final TimeRange timeRange,
                                   @JsonProperty("dateTimeSettings") final DateTimeSettings dateTimeSettings) {
        this.dataSource = dataSource;
        this.field = field;
        this.filter = filter;
        this.caseInsensitive = caseInsensitive;
        this.timeRange = timeRange;
        this.dateTimeSettings = dateTimeSettings;
    }

    /// @return The data source to read values from.
    public DocRef getDataSource() {
        return dataSource;
    }

    /// @return The field to read values of.
    public QueryField getField() {
        return field;
    }

    /// @return Text the values must contain, or null for all values.
    public String getFilter() {
        return filter;
    }

    /// @return Whether the filter ignores case, or null for case-sensitive.
    public Boolean getCaseInsensitive() {
        return caseInsensitive;
    }

    /// @return True if the filter ignores case.
    public boolean caseInsensitive() {
        return Boolean.TRUE.equals(caseInsensitive);
    }

    /// @return The time range to count values in, or null for all time.
    public TimeRange getTimeRange() {
        return timeRange;
    }

    /// @return The settings used to resolve relative times in the time range, or null.
    public DateTimeSettings getDateTimeSettings() {
        return dateTimeSettings;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FetchFieldValuesRequest that = (FetchFieldValuesRequest) o;
        return Objects.equals(dataSource, that.dataSource)
               && Objects.equals(field, that.field)
               && Objects.equals(filter, that.filter)
               && Objects.equals(caseInsensitive, that.caseInsensitive)
               && Objects.equals(timeRange, that.timeRange)
               && Objects.equals(dateTimeSettings, that.dateTimeSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSource, field, filter, caseInsensitive, timeRange, dateTimeSettings);
    }

    @Override
    public String toString() {
        return "FetchFieldValuesRequest{" +
               "dataSource=" + dataSource +
               ", field=" + field +
               ", filter='" + filter + '\'' +
               ", caseInsensitive=" + caseInsensitive +
               ", timeRange=" + timeRange +
               '}';
    }
}
