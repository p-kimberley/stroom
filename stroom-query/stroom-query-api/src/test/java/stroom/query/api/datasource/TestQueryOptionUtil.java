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

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestQueryOptionUtil {

    @Test
    void testResolve_nullSet() {
        assertThat(QueryOptionUtil.resolve(null, Map.of(QueryOptionSet.CASE_INSENSITIVE, "true")))
                .isEmpty();
    }

    @Test
    void testResolve_defaults() {
        final Map<String, String> resolved = QueryOptionUtil.resolve(QueryOptionSet.ELASTIC_KEYWORD, null);

        assertThat(resolved)
                .containsExactly(
                        Map.entry(QueryOptionSet.CASE_INSENSITIVE, "false"),
                        Map.entry(QueryOptionSet.FUZZINESS, QueryOptionSet.FUZZINESS_OFF),
                        Map.entry(QueryOptionSet.FUZZY_PREFIX_LENGTH, "0"),
                        Map.entry(QueryOptionSet.FUZZY_TRANSPOSITIONS, "true"));
    }

    @Test
    void testResolve_storedValues() {
        final Map<String, String> resolved = QueryOptionUtil.resolve(QueryOptionSet.ELASTIC_KEYWORD, Map.of(
                QueryOptionSet.FUZZINESS, "2",
                QueryOptionSet.FUZZY_PREFIX_LENGTH, "3",
                QueryOptionSet.FUZZY_TRANSPOSITIONS, "false"));

        assertThat(resolved)
                .containsEntry(QueryOptionSet.FUZZINESS, "2")
                .containsEntry(QueryOptionSet.FUZZY_PREFIX_LENGTH, "3")
                .containsEntry(QueryOptionSet.FUZZY_TRANSPOSITIONS, "false");
        assertThat(QueryOptionUtil.getInteger(resolved, QueryOptionSet.FUZZY_PREFIX_LENGTH))
                .isEqualTo(3);
        assertThat(QueryOptionUtil.getBoolean(resolved, QueryOptionSet.FUZZY_TRANSPOSITIONS))
                .isFalse();
    }

    @Test
    void testResolve_invalidValuesTakeDefaults() {
        final Map<String, String> resolved = QueryOptionUtil.resolve(QueryOptionSet.ELASTIC_TEXT, Map.of(
                QueryOptionSet.MATCH_MODE, "NONSENSE",
                QueryOptionSet.FUZZINESS, "3",
                QueryOptionSet.FUZZY_MAX_EXPANSIONS, "100000",
                "unknownOption", "true"));

        assertThat(resolved)
                .containsEntry(QueryOptionSet.MATCH_MODE, QueryOptionSet.MATCH_MODE_STANDARD)
                .containsEntry(QueryOptionSet.FUZZINESS, QueryOptionSet.FUZZINESS_OFF)
                .containsEntry(QueryOptionSet.FUZZY_MAX_EXPANSIONS, "50")
                .doesNotContainKey("unknownOption");
    }

    @Test
    void testResolve_disabledOptionTakesDefault() {
        // Fuzzy settings are disabled while fuzziness is off
        final Map<String, String> resolved = QueryOptionUtil.resolve(QueryOptionSet.ELASTIC_KEYWORD, Map.of(
                QueryOptionSet.FUZZY_PREFIX_LENGTH, "4"));

        assertThat(resolved)
                .containsEntry(QueryOptionSet.FUZZY_PREFIX_LENGTH, "0");
        assertThat(QueryOptionUtil.getDisabledOptionIds(QueryOptionSet.ELASTIC_KEYWORD, Map.of(
                QueryOptionSet.FUZZY_PREFIX_LENGTH, "4")))
                .containsExactlyInAnyOrder(
                        QueryOptionSet.FUZZY_PREFIX_LENGTH,
                        QueryOptionSet.FUZZY_TRANSPOSITIONS);
    }

    @Test
    void testResolve_caseInsensitiveDisablesFuzziness() {
        final Map<String, String> options = Map.of(QueryOptionSet.CASE_INSENSITIVE, "true");

        assertThat(QueryOptionUtil.getDisabledOptionIds(QueryOptionSet.ELASTIC_KEYWORD, options))
                .containsExactlyInAnyOrder(
                        QueryOptionSet.FUZZINESS,
                        QueryOptionSet.FUZZY_PREFIX_LENGTH,
                        QueryOptionSet.FUZZY_TRANSPOSITIONS);
        assertThat(QueryOptionUtil.resolve(QueryOptionSet.ELASTIC_KEYWORD, options))
                .containsEntry(QueryOptionSet.CASE_INSENSITIVE, "true");
    }

    @Test
    void testResolve_fuzzinessDisablesCaseInsensitive() {
        final Map<String, String> options = Map.of(QueryOptionSet.FUZZINESS, QueryOptionSet.FUZZINESS_AUTO);

        assertThat(QueryOptionUtil.getDisabledOptionIds(QueryOptionSet.ELASTIC_KEYWORD, options))
                .containsExactly(QueryOptionSet.CASE_INSENSITIVE);
    }

    @Test
    void testResolve_mutuallyExclusiveBothSet_laterOptionWins() {
        // The options dialog prevents this, but stored values could still hold both
        final Map<String, String> resolved = QueryOptionUtil.resolve(QueryOptionSet.ELASTIC_KEYWORD, Map.of(
                QueryOptionSet.CASE_INSENSITIVE, "true",
                QueryOptionSet.FUZZINESS, "1"));

        assertThat(resolved)
                .containsEntry(QueryOptionSet.CASE_INSENSITIVE, "false")
                .containsEntry(QueryOptionSet.FUZZINESS, "1");
    }

    @Test
    void testResolve_phraseModeCascades() {
        // Phrase mode disables fuzziness, which in turn disables the fuzzy settings that depend on it
        final Map<String, String> options = Map.of(
                QueryOptionSet.MATCH_MODE, QueryOptionSet.MATCH_MODE_PHRASE,
                QueryOptionSet.PHRASE_SLOP, "2",
                QueryOptionSet.DEFAULT_OPERATOR, QueryOptionSet.OPERATOR_AND,
                QueryOptionSet.FUZZINESS, "2",
                QueryOptionSet.FUZZY_PREFIX_LENGTH, "1",
                QueryOptionSet.FUZZY_MAX_EXPANSIONS, "10");

        assertThat(QueryOptionUtil.resolve(QueryOptionSet.ELASTIC_TEXT, options))
                .containsExactly(
                        Map.entry(QueryOptionSet.MATCH_MODE, QueryOptionSet.MATCH_MODE_PHRASE),
                        Map.entry(QueryOptionSet.PHRASE_SLOP, "2"),
                        Map.entry(QueryOptionSet.DEFAULT_OPERATOR, QueryOptionSet.OPERATOR_OR),
                        Map.entry(QueryOptionSet.FUZZINESS, QueryOptionSet.FUZZINESS_OFF),
                        Map.entry(QueryOptionSet.FUZZY_PREFIX_LENGTH, "0"),
                        Map.entry(QueryOptionSet.FUZZY_MAX_EXPANSIONS, "50"));
    }

    @Test
    void testResolve_standardModeDisablesSlop() {
        final Set<String> disabled = QueryOptionUtil.getDisabledOptionIds(QueryOptionSet.ELASTIC_TEXT, Map.of(
                QueryOptionSet.DEFAULT_OPERATOR, QueryOptionSet.OPERATOR_AND,
                QueryOptionSet.FUZZINESS, "1"));

        assertThat(disabled)
                .containsExactly(QueryOptionSet.PHRASE_SLOP);
    }

    @Test
    void testNormalise_dropsDefaultsAndDisabled() {
        final Map<String, String> normalised = QueryOptionUtil.normalise(QueryOptionSet.ELASTIC_KEYWORD, Map.of(
                QueryOptionSet.CASE_INSENSITIVE, "true",
                QueryOptionSet.FUZZY_TRANSPOSITIONS, "true",
                QueryOptionSet.FUZZY_PREFIX_LENGTH, "2"));

        assertThat(normalised)
                .containsExactly(Map.entry(QueryOptionSet.CASE_INSENSITIVE, "true"));
    }

    @Test
    void testNormalise_allDefaultsIsNull() {
        assertThat(QueryOptionUtil.normalise(QueryOptionSet.ELASTIC_TEXT, Map.of(
                QueryOptionSet.MATCH_MODE, QueryOptionSet.MATCH_MODE_STANDARD)))
                .isNull();
        assertThat(QueryOptionUtil.normalise(QueryOptionSet.ELASTIC_TEXT, null))
                .isNull();
        assertThat(QueryOptionUtil.normalise(null, Map.of(QueryOptionSet.CASE_INSENSITIVE, "true")))
                .isNull();
    }

    @Test
    void testHasNonDefaultOptions() {
        assertThat(QueryOptionUtil.hasNonDefaultOptions(QueryOptionSet.ELASTIC_KEYWORD,
                Map.of(QueryOptionSet.CASE_INSENSITIVE, "true")))
                .isTrue();
        assertThat(QueryOptionUtil.hasNonDefaultOptions(QueryOptionSet.ELASTIC_KEYWORD,
                Map.of(QueryOptionSet.CASE_INSENSITIVE, "false")))
                .isFalse();
        // Stored but disabled, so has no effect
        assertThat(QueryOptionUtil.hasNonDefaultOptions(QueryOptionSet.ELASTIC_KEYWORD,
                Map.of(QueryOptionSet.FUZZY_PREFIX_LENGTH, "3")))
                .isFalse();
    }

    @Test
    void testGetInteger_missing() {
        assertThatThrownBy(() -> QueryOptionUtil.getInteger(Map.of(), QueryOptionSet.PHRASE_SLOP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(QueryOptionSet.PHRASE_SLOP);
    }
}
