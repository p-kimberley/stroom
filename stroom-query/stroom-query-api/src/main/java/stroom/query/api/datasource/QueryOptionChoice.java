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

package stroom.query.api.datasource;

import stroom.docref.HasDisplayValue;

import java.util.Objects;

/// One of the allowed values of a {@link QueryOptionType#CHOICE} query option.
public final class QueryOptionChoice implements HasDisplayValue {

    private final String value;
    private final String displayValue;

    /// @param value        The value stored in the expression term.
    /// @param displayValue The text shown to the user for this value.
    public QueryOptionChoice(final String value, final String displayValue) {
        this.value = Objects.requireNonNull(value);
        this.displayValue = Objects.requireNonNull(displayValue);
    }

    /// @return The value stored in the expression term.
    public String getValue() {
        return value;
    }

    /// @return The text shown to the user for this value.
    @Override
    public String getDisplayValue() {
        return displayValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final QueryOptionChoice that = (QueryOptionChoice) o;
        return Objects.equals(value, that.value) && Objects.equals(displayValue, that.displayValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, displayValue);
    }

    @Override
    public String toString() {
        return value;
    }
}
