/*
 * Copyright 2020 Crown Copyright
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

package stroom.index.lucene;

import stroom.dictionary.api.WordListProvider;
import stroom.dictionary.shared.DictionaryDoc;
import stroom.dictionary.shared.WordList;
import stroom.docref.DocRef;
import stroom.index.lucene.analyser.AnalyzerFactory;
import stroom.index.shared.LuceneIndexField;
import stroom.query.api.DateTimeSettings;
import stroom.query.api.ExpressionOperator;
import stroom.query.api.ExpressionTerm;
import stroom.query.api.ExpressionTerm.Condition;
import stroom.query.api.datasource.AnalyzerType;
import stroom.query.api.datasource.FieldType;
import stroom.query.common.v2.MockIndexFieldCache;
import stroom.query.language.functions.Val;
import stroom.query.language.functions.ValBoolean;
import stroom.query.language.functions.ValDate;
import stroom.query.language.functions.ValDouble;
import stroom.query.language.functions.ValFloat;
import stroom.query.language.functions.ValInteger;
import stroom.query.language.functions.ValLong;
import stroom.query.language.functions.ValString;
import stroom.search.extraction.FieldValue;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSearchExpressionQueryBuilder {

    @Test
    public void testDictionaryAlphaNumeric() {
        test(AnalyzerType.ALPHA_NUMERIC);
    }

    @Test
    public void testDictionaryKeyword() {
        test(AnalyzerType.KEYWORD);
    }

    private void test(final AnalyzerType analyzerType) {
//        final DocRef dictionaryRef = dictionaryStore.createDocument("test");
//        DictionaryDoc dictionaryDoc = dictionaryStore.readDocument(dictionaryRef);
//        dictionaryDoc.setData("1\n2\n3\n4");
//        dictionaryDoc = dictionaryStore.writeDocument(dictionaryDoc);

        final DocRef dictionaryRef = new DocRef(DictionaryDoc.TYPE, "test", "test");

        final WordListProvider wordListProvider = new WordListProvider() {
            @Override
            public List<DocRef> findByName(final String name) {
                return List.of(dictionaryRef);
            }

            @Override
            public Optional<DocRef> findByUuid(final String uuid) {
                return Optional.empty();
            }

            @Override
            public String getCombinedData(final DocRef dictionaryRef) {
                return null;
            }

            @Override
            public String[] getWords(final DocRef dictionaryRef) {
                return "1\n2\n3\n4".split("\n");
            }

            @Override
            public WordList getCombinedWordList(final DocRef dictionaryRef) {
                return WordList.builder(true)
                        .addWord("1", dictionaryRef)
                        .addWord("2", dictionaryRef)
                        .addWord("3", dictionaryRef)
                        .addWord("4", dictionaryRef)
                        .build();
            }
        };

        final MockIndexFieldCache indexFieldCache = new MockIndexFieldCache();
        indexFieldCache.put("test", LuceneIndexField.createField("test", analyzerType));

        final SearchExpressionQueryBuilder searchExpressionQueryBuilder = new SearchExpressionQueryBuilder(
                new DocRef("test", "test"),
                indexFieldCache,
                wordListProvider,
                DateTimeSettings.builder().build().withoutReferenceTime(),
                null);

        final ExpressionOperator expressionOperator = ExpressionOperator.builder()
                .addDocRefTerm(
                        "test",
                        ExpressionTerm.Condition.IN_DICTIONARY,
                        dictionaryRef)
                .build();
        final SearchExpressionQueryBuilder.SearchExpressionQuery query1 = searchExpressionQueryBuilder.buildQuery(
                expressionOperator);
        System.out.println(query1.toString());

//        dictionaryDoc.setData("1\r\n2\r\n3\r\n4\r\n");
//        dictionaryDoc = dictionaryStore.writeDocument(dictionaryDoc);

        final SearchExpressionQueryBuilder.SearchExpressionQuery query2 = searchExpressionQueryBuilder.buildQuery(
                expressionOperator);
        System.out.println(query2.toString());

        assertThat(query2.getQuery()).isEqualTo(query1.getQuery());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullConditionCases")
    void testNullConditions(final String name,
                            final LuceneIndexField field,
                            final Val value) throws IOException {
        final LuceneIndexField otherField = LuceneIndexField.createField("other", AnalyzerType.KEYWORD);
        final MockIndexFieldCache indexFieldCache = new MockIndexFieldCache();
        indexFieldCache.put(field.getFldName(), field);
        indexFieldCache.put(otherField.getFldName(), otherField);
        final SearchExpressionQueryBuilder builder = new SearchExpressionQueryBuilder(
                new DocRef("test", "test"),
                indexFieldCache,
                null,
                DateTimeSettings.builder().build(),
                null);

        final FieldValue otherValue = new FieldValue(otherField, ValString.create("foo"));
        final List<FieldValue> docWithValue = List.of(otherValue, new FieldValue(field, value));
        final List<FieldValue> docWithoutValue = List.of(otherValue);

        // StroomQL gives null conditions an empty value.
        final Query isNull = buildQuery(builder, ExpressionOperator.builder()
                .addTerm(field.getFldName(), Condition.IS_NULL, "")
                .build());
        final Query isNotNull = buildQuery(builder, ExpressionOperator.builder()
                .addTerm(field.getFldName(), Condition.IS_NOT_NULL, "")
                .build());
        final Query isNullAndOther = buildQuery(builder, ExpressionOperator.builder()
                .addTerm(otherField.getFldName(), Condition.EQUALS, "foo")
                .addTerm(field.getFldName(), Condition.IS_NULL, "")
                .build());

        assertThat(matches(isNull, docWithValue)).isFalse();
        assertThat(matches(isNull, docWithoutValue)).isTrue();
        assertThat(matches(isNotNull, docWithValue)).isTrue();
        assertThat(matches(isNotNull, docWithoutValue)).isFalse();
        assertThat(matches(isNullAndOther, docWithValue)).isFalse();
        assertThat(matches(isNullAndOther, docWithoutValue)).isTrue();
    }

    private static Stream<Arguments> nullConditionCases() {
        return Stream.of(
                Arguments.of("keyword",
                        LuceneIndexField.createField("field", AnalyzerType.KEYWORD),
                        ValString.create("bar")),
                Arguments.of("alpha numeric",
                        LuceneIndexField.createField("field", AnalyzerType.ALPHA_NUMERIC),
                        ValString.create("bar baz")),
                Arguments.of("boolean",
                        createField(FieldType.BOOLEAN, AnalyzerType.KEYWORD),
                        ValBoolean.create(true)),
                Arguments.of("integer",
                        createField(FieldType.INTEGER, AnalyzerType.NUMERIC),
                        ValInteger.create(-5)),
                Arguments.of("long",
                        createField(FieldType.LONG, AnalyzerType.NUMERIC),
                        ValLong.create(Long.MIN_VALUE)),
                Arguments.of("float",
                        createField(FieldType.FLOAT, AnalyzerType.NUMERIC),
                        ValFloat.create(-1.5F)),
                Arguments.of("double",
                        createField(FieldType.DOUBLE, AnalyzerType.NUMERIC),
                        ValDouble.create(Double.MAX_VALUE)),
                Arguments.of("date",
                        createField(FieldType.DATE, AnalyzerType.NUMERIC),
                        ValDate.create(1613525014000L)),
                Arguments.of("id",
                        createField(FieldType.ID, AnalyzerType.KEYWORD),
                        ValLong.create(42L)));
    }

    private static LuceneIndexField createField(final FieldType fieldType, final AnalyzerType analyzerType) {
        return LuceneIndexField.builder()
                .fldName("field")
                .fldType(fieldType)
                .analyzerType(analyzerType)
                .build();
    }

    private static Query buildQuery(final SearchExpressionQueryBuilder builder,
                                    final ExpressionOperator expression) {
        return builder.buildQuery(expression).getQuery();
    }

    private static boolean matches(final Query query, final List<FieldValue> fieldValues) throws IOException {
        // Index the fields the same way a shard would, as a single document.
        final FieldFactory fieldFactory = new FieldFactory();
        final MemoryIndex memoryIndex = new MemoryIndex();
        for (final FieldValue fieldValue : fieldValues) {
            final LuceneIndexField indexField = LuceneIndexField.fromIndexField(fieldValue.field());
            final Analyzer analyzer = AnalyzerFactory.create(indexField.getAnalyzerType(),
                    indexField.isCaseSensitive());
            for (final Field field : fieldFactory.create(fieldValue)) {
                memoryIndex.addField(field, analyzer);
            }
        }
        return memoryIndex.createSearcher().count(query) > 0;
    }
}
