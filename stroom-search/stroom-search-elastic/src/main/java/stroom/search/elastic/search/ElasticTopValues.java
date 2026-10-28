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

package stroom.search.elastic.search;

import co.elastic.clients.elasticsearch._types.mapping.Property.Kind;

import java.util.Set;
import java.util.function.Function;

/// Works out which Elasticsearch fields can list their most common values with a `terms` aggregation, for the
/// value picker of an expression term, and builds the filter applied to those values.
public final class ElasticTopValues {

    /// The name of the `keyword` multi-field that a `text` field's whole values are aggregated on.
    static final String KEYWORD_SUB_FIELD = ".keyword";

    // Mapping types whose values can be aggregated directly. A `text` field can't be, so it uses its
    // `keyword` multi-field if it has one.
    private static final Set<String> AGGREGATABLE_TYPES = Set.of(
            Kind.Keyword.jsonValue(),
            Kind.Wildcard.jsonValue(),
            Kind.Ip.jsonValue(),
            Kind.Boolean.jsonValue(),
            Kind.Integer.jsonValue(),
            Kind.Short.jsonValue(),
            Kind.Byte.jsonValue(),
            Kind.Long.jsonValue());
    private static final Set<String> STRING_TYPES = Set.of(
            Kind.Keyword.jsonValue(),
            Kind.Wildcard.jsonValue());
    private static final Set<String> TEXT_TYPES = Set.of(
            Kind.Text.jsonValue(),
            Kind.MatchOnlyText.jsonValue());
    // Characters with a special meaning in Lucene regular expressions
    private static final String REGEX_SPECIAL_CHARS = ".?+*|{}[]()\"\\#@&<>~";

    private ElasticTopValues() {
        // Utility class.
    }

    /// Finds the field to aggregate to list a field's most common values.
    ///
    /// @param fieldName         The name of the field.
    /// @param nativeType        The Elasticsearch mapping type of the field.
    /// @param nativeTypeLookup  Returns the mapping type of another field by name, or null if there is no such
    ///                          field. Used to find a `text` field's `keyword` multi-field and whether the field
    ///                          is within a `nested` object.
    /// @return The name of the field to aggregate, or null if the field's values can't be listed. Fields within
    /// a `nested` object can't be listed, as they need a `nested` aggregation.
    public static String getAggregationField(final String fieldName,
                                             final String nativeType,
                                             final Function<String, String> nativeTypeLookup) {
        if (isWithinNestedObject(fieldName, nativeTypeLookup)) {
            return null;
        } else if (AGGREGATABLE_TYPES.contains(nativeType)) {
            return fieldName;
        } else if (TEXT_TYPES.contains(nativeType)) {
            final String keywordField = fieldName + KEYWORD_SUB_FIELD;
            if (Kind.Keyword.jsonValue().equals(nativeTypeLookup.apply(keywordField))) {
                return keywordField;
            }
        }
        return null;
    }

    private static boolean isWithinNestedObject(final String fieldName,
                                                final Function<String, String> nativeTypeLookup) {
        int separatorIndex = fieldName.indexOf('.');
        while (separatorIndex > 0) {
            if (Kind.Nested.jsonValue().equals(nativeTypeLookup.apply(fieldName.substring(0, separatorIndex)))) {
                return true;
            }
            separatorIndex = fieldName.indexOf('.', separatorIndex + 1);
        }
        return false;
    }

    /// @param nativeType The Elasticsearch mapping type of the field being aggregated.
    /// @return True if the field holds strings, so its values can be filtered with a regular expression.
    public static boolean isFilterable(final String nativeType) {
        return STRING_TYPES.contains(nativeType);
    }

    /// Builds a Lucene regular expression that matches values containing the filter text.
    ///
    /// @param filter          The text the values must contain.
    /// @param caseInsensitive Whether letters match regardless of case.
    /// @return The regular expression.
    public static String toContainsRegex(final String filter, final boolean caseInsensitive) {
        final StringBuilder sb = new StringBuilder(".*");
        filter.codePoints().forEach(codePoint -> {
            final String upper = new String(Character.toChars(Character.toUpperCase(codePoint)));
            final String lower = new String(Character.toChars(Character.toLowerCase(codePoint)));
            if (caseInsensitive && !upper.equals(lower)) {
                sb.append('[').append(lower).append(upper).append(']');
            } else {
                if (REGEX_SPECIAL_CHARS.indexOf(codePoint) >= 0) {
                    sb.append('\\');
                }
                sb.appendCodePoint(codePoint);
            }
        });
        return sb.append(".*").toString();
    }
}
