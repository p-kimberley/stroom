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

package stroom.search.elastic.suggest;

import stroom.dictionary.api.WordListProvider;
import stroom.docref.DocRef;
import stroom.query.api.DateTimeSettings;
import stroom.query.api.TimeRange;
import stroom.query.api.datasource.FieldType;
import stroom.query.api.datasource.QueryField;
import stroom.query.common.v2.MockIndexFieldCache;
import stroom.query.shared.FetchFieldValuesRequest;
import stroom.query.shared.FieldValueCount;
import stroom.query.shared.FieldValues;
import stroom.search.elastic.ElasticClientCache;
import stroom.search.elastic.ElasticClusterStore;
import stroom.search.elastic.ElasticIndexCache;
import stroom.search.elastic.ElasticWireMock;
import stroom.search.elastic.shared.ElasticClusterDoc;
import stroom.search.elastic.shared.ElasticConnectionConfig;
import stroom.search.elastic.shared.ElasticIndexDoc;
import stroom.search.elastic.shared.ElasticIndexField;
import stroom.task.api.ExecutorProvider;
import stroom.task.api.SimpleTaskContextFactory;
import stroom.util.shared.PermissionException;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestElasticSuggestionsQueryHandlerImpl {

    private static final String INDEX_NAME = "logs";
    private static final String SEARCH_PATH = "/" + INDEX_NAME + "/_search";
    private static final DocRef CLUSTER_REF = new DocRef(ElasticClusterDoc.TYPE, UUID.randomUUID().toString());
    private static final DocRef INDEX_REF = new DocRef(ElasticIndexDoc.TYPE, UUID.randomUUID().toString(), "logs");
    private static final String TIME_FIELD = "@timestamp";

    @RegisterExtension
    static final WireMockExtension ELASTIC = ElasticWireMock.createExtension();

    @Mock
    private ElasticClientCache elasticClientCache;
    @Mock
    private ElasticClusterStore elasticClusterStore;
    @Mock
    private ElasticIndexCache elasticIndexCache;
    @Mock
    private WordListProvider wordListProvider;
    @Mock
    private ExecutorProvider executorProvider;

    private final MockIndexFieldCache indexFieldCache = new MockIndexFieldCache();
    private ElasticsearchClient elasticClient;
    private ElasticSuggestConfig config;

    @BeforeEach
    void setUp() {
        elasticClient = ElasticWireMock.createClient(ELASTIC);
        config = new ElasticSuggestConfig(true, 5);

        final ElasticClusterDoc cluster = ElasticClusterDoc.builder()
                .uuid(CLUSTER_REF.getUuid())
                .connection(new ElasticConnectionConfig(List.of(ELASTIC.baseUrl()), null, null))
                .build();
        when(elasticClusterStore.readDocument(CLUSTER_REF)).thenReturn(cluster);
        when(elasticClientCache.contextResult(eq(cluster.getConnection()), any()))
                .thenAnswer(invocation -> invocation
                        .<Function<ElasticsearchClient, ?>>getArgument(1)
                        .apply(elasticClient));
        givenIndex(TIME_FIELD);
        when(executorProvider.get(any())).thenReturn((Executor) Runnable::run);

        addField("user", FieldType.KEYWORD, "keyword");
        addField("message", FieldType.TEXT, "text");
        addField("message.keyword", FieldType.KEYWORD, "keyword");
        addField("title", FieldType.TEXT, "text");
        addField("flag", FieldType.BOOLEAN, "boolean");
        addField("source.ip", FieldType.IPV4_ADDRESS, "ip");
        addField(TIME_FIELD, FieldType.DATE, "date");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (elasticClient != null) {
            elasticClient.close();
        }
    }

    @Test
    void testKeywordField_allTime() {
        stubSearch(10, """
                "sterms#values": {
                  "doc_count_error_upper_bound": 0,
                  "sum_other_doc_count": 1,
                  "buckets": [
                    { "key": "alice", "doc_count": 6 },
                    { "key": "bob", "doc_count": 3 }
                  ]
                }""");

        final FieldValues fieldValues = createHandler().getFieldValues(request("user", null, false, null));

        assertThat(fieldValues.getValues())
                .containsExactly(new FieldValueCount("alice", 6L), new FieldValueCount("bob", 3L));
        assertThat(fieldValues.getTotalCount()).isEqualTo(10);
        assertThat(fieldValues.isAllTime()).isTrue();
        ELASTIC.verify(WireMock.postRequestedFor(WireMock.urlPathEqualTo(SEARCH_PATH))
                .withRequestBody(WireMock.matchingJsonPath("$.size", WireMock.equalTo("0")))
                .withRequestBody(WireMock.matchingJsonPath("$.track_total_hits", WireMock.equalTo("true")))
                .withRequestBody(WireMock.matchingJsonPath("$.query.match_all", WireMock.equalToJson("{}")))
                .withRequestBody(WireMock.matchingJsonPath(
                        "$.aggregations.values.terms.field", WireMock.equalTo("user")))
                .withRequestBody(WireMock.matchingJsonPath(
                        "$.aggregations.values.terms.size", WireMock.equalTo("5"))));
    }

    @Test
    void testTimeRange() {
        stubSearch(4, emptyStringTerms());

        final FieldValues fieldValues = createHandler().getFieldValues(request(
                "user",
                null,
                false,
                new TimeRange("Last day", null, "2026-10-27T00:00:00.000Z", "2026-10-28T00:00:00.000Z")));

        assertThat(fieldValues.isAllTime()).isFalse();
        ELASTIC.verify(WireMock.postRequestedFor(WireMock.urlPathEqualTo(SEARCH_PATH))
                .withRequestBody(WireMock.matchingJsonPath("$..range['" + TIME_FIELD + "'].gte"))
                .withRequestBody(WireMock.matchingJsonPath("$..range['" + TIME_FIELD + "'].lt")));
    }

    @Test
    void testTimeRange_timeFieldNotInIndexCountsAllTime() {
        givenIndex("EventTime");
        stubSearch(4, emptyStringTerms());

        final FieldValues fieldValues = createHandler().getFieldValues(request(
                "user",
                null,
                false,
                new TimeRange("Last day", null, "now() - 1d", "now()")));

        assertThat(fieldValues.isAllTime()).isTrue();
        ELASTIC.verify(WireMock.postRequestedFor(WireMock.urlPathEqualTo(SEARCH_PATH))
                .withRequestBody(WireMock.matchingJsonPath("$.query.match_all", WireMock.equalToJson("{}"))));
    }

    @Test
    void testFilter_caseInsensitive() {
        stubSearch(4, emptyStringTerms());

        createHandler().getFieldValues(request("user", " Ad ", true, null));

        ELASTIC.verify(WireMock.postRequestedFor(WireMock.urlPathEqualTo(SEARCH_PATH))
                .withRequestBody(WireMock.matchingJsonPath("$.aggregations.values.terms.include",
                        WireMock.equalTo(".*[aA][dD].*"))));
    }

    @Test
    void testFilter_ignoredForNonStringField() {
        stubSearch(4, emptyStringTerms());

        createHandler().getFieldValues(request("source.ip", "10.", false, null));

        ELASTIC.verify(WireMock.postRequestedFor(WireMock.urlPathEqualTo(SEARCH_PATH))
                .withRequestBody(WireMock.notContaining("include")));
    }

    @Test
    void testTextField_aggregatesKeywordMultiField() {
        stubSearch(4, emptyStringTerms());

        createHandler().getFieldValues(request("message", null, false, null));

        ELASTIC.verify(WireMock.postRequestedFor(WireMock.urlPathEqualTo(SEARCH_PATH))
                .withRequestBody(WireMock.matchingJsonPath("$.aggregations.values.terms.field",
                        WireMock.equalTo("message.keyword"))));
    }

    @Test
    void testBooleanField_usesKeyAsString() {
        stubSearch(5, """
                "lterms#values": {
                  "doc_count_error_upper_bound": 0,
                  "sum_other_doc_count": 0,
                  "buckets": [
                    { "key": 1, "key_as_string": "true", "doc_count": 3 },
                    { "key": 0, "key_as_string": "false", "doc_count": 2 }
                  ]
                }""");

        final FieldValues fieldValues = createHandler().getFieldValues(request("flag", null, false, null));

        assertThat(fieldValues.getValues())
                .containsExactly(new FieldValueCount("true", 3L), new FieldValueCount("false", 2L));
    }

    @Test
    void testUnsupportedFields_noRequest() {
        final ElasticSuggestionsQueryHandlerImpl handler = createHandler();

        // A text field without a keyword multi-field, a date field, and an unknown field
        assertThat(handler.getFieldValues(request("title", null, false, null))).isEqualTo(FieldValues.EMPTY);
        assertThat(handler.getFieldValues(request(TIME_FIELD, null, false, null))).isEqualTo(FieldValues.EMPTY);
        assertThat(handler.getFieldValues(request("unknown", null, false, null))).isEqualTo(FieldValues.EMPTY);
        ELASTIC.verify(0, WireMock.postRequestedFor(WireMock.urlPathEqualTo(SEARCH_PATH)));
    }

    @Test
    void testDisabled_noRequest() {
        config = new ElasticSuggestConfig(false, 5);

        assertThat(createHandler().getFieldValues(request("user", null, false, null)))
                .isEqualTo(FieldValues.EMPTY);
        ELASTIC.verify(0, WireMock.postRequestedFor(WireMock.urlPathEqualTo(SEARCH_PATH)));
    }

    @Test
    void testPermissionDenied() {
        // The index cache checks the user may search the index
        when(elasticIndexCache.get(INDEX_REF)).thenThrow(new PermissionException(null, "Not allowed"));

        assertThatThrownBy(() -> createHandler().getFieldValues(request("user", null, false, null)))
                .isInstanceOf(PermissionException.class);
        ELASTIC.verify(0, WireMock.postRequestedFor(WireMock.urlPathEqualTo(SEARCH_PATH)));
    }

    @Test
    void testElasticError() {
        ELASTIC.stubFor(WireMock.post(WireMock.urlPathEqualTo(SEARCH_PATH))
                .willReturn(ElasticWireMock.elasticResponse(400, """
                        {
                          "error": {
                            "root_cause": [ { "type": "illegal_argument_exception", "reason": "bad field" } ],
                            "type": "illegal_argument_exception",
                            "reason": "bad field"
                          },
                          "status": 400
                        }""")));

        assertThatThrownBy(() -> createHandler().getFieldValues(request("user", null, false, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error fetching values of field 'user'");
    }

    @Test
    void testConfig_countLimited() {
        assertThat(new ElasticSuggestConfig(true, 0).getEffectiveTopValuesCount()).isEqualTo(1);
        assertThat(new ElasticSuggestConfig(true, 5000).getEffectiveTopValuesCount())
                .isEqualTo(ElasticSuggestConfig.MAX_TOP_VALUES_COUNT);
        assertThat(new ElasticSuggestConfig(true, null).getTopValuesCount())
                .isEqualTo(ElasticSuggestConfig.DEFAULT_TOP_VALUES_COUNT);
    }

    private ElasticSuggestionsQueryHandlerImpl createHandler() {
        return new ElasticSuggestionsQueryHandlerImpl(
                () -> elasticClientCache,
                () -> elasticClusterStore,
                () -> elasticIndexCache,
                () -> indexFieldCache,
                () -> wordListProvider,
                () -> null,
                () -> config,
                SimpleTaskContextFactory::new,
                executorProvider);
    }

    private void givenIndex(final String timeField) {
        when(elasticIndexCache.get(INDEX_REF)).thenReturn(ElasticIndexDoc.builder()
                .uuid(INDEX_REF.getUuid())
                .name(INDEX_REF.getName())
                .clusterRef(CLUSTER_REF)
                .indexName(INDEX_NAME)
                .timeField(timeField)
                .build());
    }

    private void addField(final String name, final FieldType fieldType, final String nativeType) {
        indexFieldCache.put(name, ElasticIndexField.builder()
                .fldName(name)
                .fldType(fieldType)
                .nativeType(nativeType)
                .build());
    }

    private static FetchFieldValuesRequest request(final String fieldName,
                                                   final String filter,
                                                   final boolean caseInsensitive,
                                                   final TimeRange timeRange) {
        return new FetchFieldValuesRequest(
                INDEX_REF,
                QueryField.builder().fldName(fieldName).fldType(FieldType.KEYWORD).build(),
                filter,
                caseInsensitive,
                timeRange,
                DateTimeSettings.builder().referenceTime(1761609600000L).build());
    }

    private static String emptyStringTerms() {
        return """
                "sterms#values": {
                  "doc_count_error_upper_bound": 0,
                  "sum_other_doc_count": 0,
                  "buckets": []
                }""";
    }

    private static void stubSearch(final long totalHits, final String aggregations) {
        ELASTIC.stubFor(WireMock.post(WireMock.urlPathEqualTo(SEARCH_PATH))
                .willReturn(ElasticWireMock.elasticResponse(200, """
                        {
                          "took": 1,
                          "timed_out": false,
                          "_shards": { "total": 1, "successful": 1, "skipped": 0, "failed": 0 },
                          "hits": { "total": { "value": %d, "relation": "eq" }, "max_score": null, "hits": [] },
                          "aggregations": { %s }
                        }""".formatted(totalHits, aggregations))));
    }
}
