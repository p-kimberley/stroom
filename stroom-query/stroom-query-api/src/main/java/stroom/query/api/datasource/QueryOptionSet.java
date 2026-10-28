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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// The field-level query options a data source supports for a kind of field. A data source names
/// the set for each field with {@link QueryField#getQueryOptionSet()}, and the options dialog is built
/// from the set's definitions.
///
/// Definitions are listed in the order they are shown and resolved in. A rule should refer to an
/// option listed before it where possible; see {@link QueryOptionUtil#resolve(QueryOptionSet, java.util.Map)}.
public enum QueryOptionSet {
    /// Elasticsearch `keyword` and `wildcard` fields.
    ELASTIC_KEYWORD(
            caseInsensitive(),
            keywordFuzziness(),
            fuzzyPrefixLength(),
            fuzzyTranspositions()),

    /// Elasticsearch `text` and `match_only_text` fields.
    ELASTIC_TEXT(
            matchMode(),
            phraseSlop(),
            defaultOperator(),
            textFuzziness(),
            fuzzyPrefixLength(),
            fuzzyMaxExpansions());

    /// Match values regardless of case. Value `true` or `false`.
    public static final String CASE_INSENSITIVE = "caseInsensitive";
    /// Maximum edit distance for fuzzy matching. One of {@link #FUZZINESS_OFF}, `AUTO`, `1` or `2`.
    public static final String FUZZINESS = "fuzziness";
    /// Number of leading characters that must match exactly in fuzzy matching.
    public static final String FUZZY_PREFIX_LENGTH = "fuzzyPrefixLength";
    /// Whether swapping two adjacent characters counts as one edit in fuzzy matching.
    public static final String FUZZY_TRANSPOSITIONS = "fuzzyTranspositions";
    /// Maximum number of terms a fuzzy text match expands to.
    public static final String FUZZY_MAX_EXPANSIONS = "fuzzyMaxExpansions";
    /// How a text value is matched. {@link #MATCH_MODE_STANDARD} or {@link #MATCH_MODE_PHRASE}.
    public static final String MATCH_MODE = "matchMode";
    /// Number of positions words in a phrase may move and still match.
    public static final String PHRASE_SLOP = "phraseSlop";
    /// Whether all or any words of a text value must match. {@link #OPERATOR_OR} or {@link #OPERATOR_AND}.
    public static final String DEFAULT_OPERATOR = "defaultOperator";

    /// The {@link #FUZZINESS} value that turns fuzzy matching off.
    public static final String FUZZINESS_OFF = "OFF";
    /// The {@link #FUZZINESS} value that picks the edit distance from the value's length.
    public static final String FUZZINESS_AUTO = "AUTO";
    /// The {@link #MATCH_MODE} value for matching words in any order.
    public static final String MATCH_MODE_STANDARD = "STANDARD";
    /// The {@link #MATCH_MODE} value for matching words as a phrase.
    public static final String MATCH_MODE_PHRASE = "PHRASE";
    /// The {@link #DEFAULT_OPERATOR} value for matching any word.
    public static final String OPERATOR_OR = "OR";
    /// The {@link #DEFAULT_OPERATOR} value for matching every word.
    public static final String OPERATOR_AND = "AND";

    private final List<QueryOptionDefinition> definitions;

    QueryOptionSet(final QueryOptionDefinition... definitions) {
        this.definitions = Collections.unmodifiableList(Arrays.asList(definitions));
    }

    /// @return The option definitions, in the order they are shown and resolved in.
    public List<QueryOptionDefinition> getDefinitions() {
        return definitions;
    }

    /// @param id The option id.
    /// @return The definition with the given id, or null if the set has no such option.
    public QueryOptionDefinition getDefinition(final String id) {
        for (final QueryOptionDefinition definition : definitions) {
            if (definition.getId().equals(id)) {
                return definition;
            }
        }
        return null;
    }

    private static QueryOptionDefinition caseInsensitive() {
        return QueryOptionDefinition.builder()
                .id(CASE_INSENSITIVE)
                .displayName("Case-insensitive")
                .description("Match values regardless of case. Wildcard and regular expression matches " +
                             "that ignore case are slower on large indices.")
                .booleanType(false)
                .addRule(QueryOptionRule.disabledUnless(FUZZINESS, FUZZINESS_OFF))
                .build();
    }

    private static QueryOptionDefinition keywordFuzziness() {
        return fuzziness("Match values within a number of single-character edits of the value, e.g. to " +
                         "find lookalike domains or user names. Fuzzy matching is always case-sensitive " +
                         "and does not apply to values containing wildcards.")
                .addRule(QueryOptionRule.disabledWhen(CASE_INSENSITIVE, "true"))
                .build();
    }

    private static QueryOptionDefinition textFuzziness() {
        return fuzziness("Match words within a number of single-character edits of each word, e.g. to " +
                         "allow for misspellings. Does not apply to values containing wildcards.")
                .addRule(QueryOptionRule.disabledWhen(MATCH_MODE, MATCH_MODE_PHRASE))
                .build();
    }

    private static QueryOptionDefinition.Builder fuzziness(final String description) {
        return QueryOptionDefinition.builder()
                .id(FUZZINESS)
                .displayName("Fuzzy matching")
                .description(description)
                .choiceType(FUZZINESS_OFF)
                .addChoice(FUZZINESS_OFF, "Off")
                .addChoice(FUZZINESS_AUTO, "Auto (by length)")
                .addChoice("1", "1 edit")
                .addChoice("2", "2 edits");
    }

    private static QueryOptionDefinition fuzzyPrefixLength() {
        return QueryOptionDefinition.builder()
                .id(FUZZY_PREFIX_LENGTH)
                .displayName("Exact prefix length")
                .description("Number of leading characters that must match exactly. A longer prefix " +
                             "makes fuzzy matching faster.")
                .integerType(0, 0, 20)
                .addRule(QueryOptionRule.disabledWhen(FUZZINESS, FUZZINESS_OFF))
                .build();
    }

    private static QueryOptionDefinition fuzzyTranspositions() {
        return QueryOptionDefinition.builder()
                .id(FUZZY_TRANSPOSITIONS)
                .displayName("Allow transpositions")
                .description("Count swapping two adjacent characters (ab to ba) as one edit.")
                .booleanType(true)
                .addRule(QueryOptionRule.disabledWhen(FUZZINESS, FUZZINESS_OFF))
                .build();
    }

    private static QueryOptionDefinition fuzzyMaxExpansions() {
        return QueryOptionDefinition.builder()
                .id(FUZZY_MAX_EXPANSIONS)
                .displayName("Maximum expansions")
                .description("Maximum number of similar words each word is expanded to.")
                .integerType(50, 1, 1000)
                .addRule(QueryOptionRule.disabledWhen(FUZZINESS, FUZZINESS_OFF))
                .build();
    }

    private static QueryOptionDefinition matchMode() {
        return QueryOptionDefinition.builder()
                .id(MATCH_MODE)
                .displayName("Match mode")
                .description("Standard matches the words in any order. Phrase matches them in the order " +
                             "given, e.g. for log messages or command lines. Wildcards are not supported " +
                             "in phrase mode.")
                .choiceType(MATCH_MODE_STANDARD)
                .addChoice(MATCH_MODE_STANDARD, "Standard")
                .addChoice(MATCH_MODE_PHRASE, "Phrase")
                .build();
    }

    private static QueryOptionDefinition phraseSlop() {
        return QueryOptionDefinition.builder()
                .id(PHRASE_SLOP)
                .displayName("Phrase slop")
                .description("Number of positions words may move and still match the phrase. " +
                             "0 requires the exact phrase.")
                .integerType(0, 0, 100)
                .addRule(QueryOptionRule.disabledUnless(MATCH_MODE, MATCH_MODE_PHRASE))
                .build();
    }

    private static QueryOptionDefinition defaultOperator() {
        return QueryOptionDefinition.builder()
                .id(DEFAULT_OPERATOR)
                .displayName("Words to match")
                .description("Whether a document must contain any or all of the words in the value.")
                .choiceType(OPERATOR_OR)
                .addChoice(OPERATOR_OR, "Any word")
                .addChoice(OPERATOR_AND, "All words")
                .addRule(QueryOptionRule.disabledWhen(MATCH_MODE, MATCH_MODE_PHRASE))
                .build();
    }
}
