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

import java.util.Objects;

/// A rule that disables a query option depending on the value of another option in the same
/// {@link QueryOptionSet}, e.g. "disabled when `matchMode` is `PHRASE`".
/// A disabled option takes its default value.
public final class QueryOptionRule {

    private final String optionId;
    private final String value;
    private final boolean negate;

    private QueryOptionRule(final String optionId, final String value, final boolean negate) {
        this.optionId = Objects.requireNonNull(optionId);
        this.value = Objects.requireNonNull(value);
        this.negate = negate;
    }

    /// Creates a rule that applies when the other option has the given value.
    ///
    /// @param optionId The id of the other option.
    /// @param value    The value that disables the option the rule belongs to.
    /// @return The rule.
    public static QueryOptionRule disabledWhen(final String optionId, final String value) {
        return new QueryOptionRule(optionId, value, false);
    }

    /// Creates a rule that applies when the other option has any value other than the given value.
    ///
    /// @param optionId The id of the other option.
    /// @param value    The only value that leaves the option the rule belongs to enabled.
    /// @return The rule.
    public static QueryOptionRule disabledUnless(final String optionId, final String value) {
        return new QueryOptionRule(optionId, value, true);
    }

    /// @return The id of the other option this rule tests.
    public String getOptionId() {
        return optionId;
    }

    /// Tests the rule against the other option's effective value.
    ///
    /// @param otherValue The effective value of the option with id {@link #getOptionId()}.
    /// @return True if the option the rule belongs to is disabled.
    public boolean appliesTo(final String otherValue) {
        final boolean matches = value.equals(otherValue);
        return negate != matches;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final QueryOptionRule that = (QueryOptionRule) o;
        return negate == that.negate && Objects.equals(optionId, that.optionId) && Objects.equals(value,
                that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optionId, value, negate);
    }

    @Override
    public String toString() {
        return "disabled when " + optionId + (negate
                ? " != "
                : " == ") + value;
    }
}
