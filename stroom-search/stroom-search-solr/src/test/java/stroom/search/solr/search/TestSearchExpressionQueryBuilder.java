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

package stroom.search.solr.search;

import stroom.docref.DocRef;
import stroom.query.api.DateTimeSettings;
import stroom.query.api.ExpressionOperator;
import stroom.query.api.ExpressionTerm.Condition;
import stroom.query.api.datasource.FieldType;
import stroom.query.common.v2.MockIndexFieldCache;
import stroom.search.solr.shared.SolrIndexField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TestSearchExpressionQueryBuilder {

    @ParameterizedTest
    @EnumSource(value = FieldType.class,
            names = {"TEXT", "BOOLEAN", "INTEGER", "LONG", "FLOAT", "DOUBLE", "DATE", "ID"})
    void testNullConditions(final FieldType fieldType) {
        final SolrIndexField field = SolrIndexField.builder()
                .fldName("field")
                .fldType(fieldType)
                .build();
        final SolrIndexField otherField = SolrIndexField.builder()
                .fldName("other")
                .fldType(FieldType.TEXT)
                .build();
        final MockIndexFieldCache indexFieldCache = new MockIndexFieldCache();
        indexFieldCache.put(field.getFldName(), field);
        indexFieldCache.put(otherField.getFldName(), otherField);
        final SearchExpressionQueryBuilder builder = new SearchExpressionQueryBuilder(
                new DocRef("test", "test"),
                indexFieldCache,
                null,
                1024,
                DateTimeSettings.builder().build());

        // StroomQL gives null conditions an empty value. The query string is what is sent to Solr.
        assertThat(buildQueryString(builder, ExpressionOperator.builder()
                .addTerm(field.getFldName(), Condition.IS_NULL, "")
                .build()))
                .isEqualTo("*:* -field:[* TO *]");
        assertThat(buildQueryString(builder, ExpressionOperator.builder()
                .addTerm(field.getFldName(), Condition.IS_NOT_NULL, "")
                .build()))
                .isEqualTo("field:[* TO *]");
        assertThat(buildQueryString(builder, ExpressionOperator.builder()
                .addTerm(otherField.getFldName(), Condition.EQUALS, "foo")
                .addTerm(field.getFldName(), Condition.IS_NULL, "")
                .build()))
                .isEqualTo("+other:foo -field:[* TO *] +*:*");
    }

    private static String buildQueryString(final SearchExpressionQueryBuilder builder,
                                           final ExpressionOperator expression) {
        return builder.buildQuery(expression).getQuery().toString();
    }
}
