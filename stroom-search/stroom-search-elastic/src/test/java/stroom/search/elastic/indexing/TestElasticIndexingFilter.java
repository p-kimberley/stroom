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

package stroom.search.elastic.indexing;

import stroom.docref.DocRef;
import stroom.meta.shared.Meta;
import stroom.pipeline.LocationFactoryProxy;
import stroom.pipeline.PipelineStore;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.errorhandler.LoggedException;
import stroom.pipeline.shared.PipelineDoc;
import stroom.pipeline.state.MetaHolder;
import stroom.pipeline.state.StreamProcessorHolder;
import stroom.pipeline.xml.converter.json.JSONParser;
import stroom.processor.shared.Processor;
import stroom.processor.shared.ProcessorFilter;
import stroom.processor.shared.ProcessorTask;
import stroom.search.elastic.ElasticClientCache;
import stroom.search.elastic.ElasticClusterStore;
import stroom.search.elastic.ElasticConfig;
import stroom.search.elastic.shared.ElasticClusterDoc;
import stroom.search.elastic.shared.ElasticConnectionConfig;
import stroom.util.shared.Severity;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestElasticIndexingFilter {

    private static final String NAMESPACE = "http://www.w3.org/2013/XSL/json";
    private static final String INDEX_NAME = "stroom-test-index";

    @Mock
    private LocationFactoryProxy locationFactory;
    @Mock
    private ErrorReceiverProxy errorReceiverProxy;
    @Mock
    private ElasticClientCache elasticClientCache;
    @Mock
    private ElasticClusterStore elasticClusterStore;
    @Mock
    private PipelineStore pipelineStore;
    @Mock
    private StreamProcessorHolder streamProcessorHolder;
    @Mock
    private MetaHolder metaHolder;
    @Mock
    private ElasticsearchClient elasticClient;

    private ElasticIndexingFilter filter;

    @BeforeEach
    void setUp() {
        final DocRef pipelineRef = new DocRef(PipelineDoc.TYPE, "pipeline-uuid", "test-pipeline");
        final Processor processor = mock(Processor.class);
        when(processor.getPipeline()).thenReturn(pipelineRef);
        final ProcessorFilter processorFilter = mock(ProcessorFilter.class);
        when(processorFilter.isReprocess()).thenReturn(false);
        final ProcessorTask processorTask = mock(ProcessorTask.class);
        when(processorTask.getProcessorFilter()).thenReturn(processorFilter);
        when(streamProcessorHolder.getStreamProcessor()).thenReturn(processor);
        when(streamProcessorHolder.getStreamTask()).thenReturn(processorTask);

        final PipelineDoc pipelineDoc = mock(PipelineDoc.class);
        when(pipelineDoc.getName()).thenReturn("test-pipeline");
        when(pipelineStore.readDocument(pipelineRef)).thenReturn(pipelineDoc);

        final ElasticClusterDoc clusterDoc = mock(ElasticClusterDoc.class);
        when(clusterDoc.getConnection()).thenReturn(new ElasticConnectionConfig());
        when(clusterDoc.getName()).thenReturn("test-cluster");
        when(elasticClusterStore.readDocument(any())).thenReturn(clusterDoc);

        final Meta meta = mock(Meta.class);
        when(meta.getId()).thenReturn(123L);
        when(metaHolder.getMeta()).thenReturn(meta);

        doAnswer(invocation -> {
            final Consumer<ElasticsearchClient> consumer = invocation.getArgument(1);
            consumer.accept(elasticClient);
            return null;
        }).when(elasticClientCache).context(any(), any());

        final ElasticConfig elasticConfig = new ElasticConfig();
        filter = new ElasticIndexingFilter(
                locationFactory,
                errorReceiverProxy,
                () -> elasticConfig,
                elasticClientCache,
                elasticClusterStore,
                pipelineStore,
                streamProcessorHolder,
                metaHolder);
        filter.setCluster(new DocRef(ElasticClusterDoc.TYPE, "cluster-uuid", "test-cluster"));
        filter.setIndexName(INDEX_NAME);
        // Send each document as soon as it is queued, so a bulk failure surfaces mid-part
        filter.setBatchSize(1);
    }

    /// Control case: with no failures, a document from each of two stream parts is sent to Elasticsearch.
    @Test
    void testDocumentFromEachPartIsIndexed() throws Exception {
        when(elasticClient.bulk(any(BulkRequest.class)))
                .thenReturn(successResponse())
                .thenReturn(successResponse());

        filter.startProcessing();
        processPart("part1");
        processPart("part2");
        filter.endProcessing();

        verify(elasticClient, times(2)).bulk(any(BulkRequest.class));
    }

    /// A bulk request rejected with a 403 (e.g. the account lacks `create_index`/`auto_configure` on the
    /// target data stream) aborts the current stream part with a fatal error. The pipeline then carries on with
    /// the next part of the same stream using the same filter instance. The filter must reset its per-part
    /// parsing state so that documents in later parts are still indexed.
    ///
    /// Currently `inOuterArray`/`currentDepth` are left over from the aborted part, so every document in every
    /// later part is silently dropped (no bulk request, no error).
    @Test
    void testDocumentsInLaterPartsAreIndexedAfterBulkFailure() throws Exception {
        // Record the number of operations in each request when it is sent, as the filter clears its operation
        // list (which the request references) once the request completes
        final List<Integer> operationCounts = new ArrayList<>();
        when(elasticClient.bulk(any(BulkRequest.class))).thenAnswer(invocation -> {
            final BulkRequest request = invocation.getArgument(0);
            operationCounts.add(request.operations().size());
            return operationCounts.size() == 1
                    ? forbiddenResponse()
                    : successResponse();
        });

        filter.startProcessing();

        // Part 1 fails with a 403. This is what AbstractProcessorTaskExecutor.processNestedStreams sees, before
        // swallowing the LoggedException and moving on to the next part.
        assertThatThrownBy(() -> processPart("part1"))
                .isInstanceOf(LoggedException.class);
        verify(errorReceiverProxy, atLeastOnce())
                .log(eq(Severity.FATAL_ERROR), any(), any(), any(), any());

        // Part 2 should be indexed as normal
        processPart("part2");
        filter.endProcessing();

        // One request for the rejected part 1 document and one for the part 2 document
        assertThat(operationCounts)
                .containsExactly(1, 1);
    }

    private void processPart(final String value) throws SAXException {
        filter.startStream();
        try {
            filter.startDocument();
            startElement(JSONParser.XML_ELEMENT_ARRAY, null);
            startElement(JSONParser.XML_ELEMENT_MAP, null);
            startElement(JSONParser.XML_ELEMENT_STRING, "value");
            filter.characters(value.toCharArray(), 0, value.length());
            endElement(JSONParser.XML_ELEMENT_STRING);
            endElement(JSONParser.XML_ELEMENT_MAP);
            endElement(JSONParser.XML_ELEMENT_ARRAY);
            filter.endDocument();
        } finally {
            filter.endStream();
        }
    }

    private void startElement(final String localName, final String key) throws SAXException {
        final AttributesImpl attributes = new AttributesImpl();
        if (key != null) {
            attributes.addAttribute("", JSONParser.XML_ATTRIBUTE_KEY, JSONParser.XML_ATTRIBUTE_KEY, "CDATA", key);
        }
        filter.startElement(NAMESPACE, localName, localName, attributes);
    }

    private void endElement(final String localName) throws SAXException {
        filter.endElement(NAMESPACE, localName, localName);
    }

    private static BulkResponse successResponse() {
        return BulkResponse.of(b -> b
                .errors(false)
                .took(1)
                .items(BulkResponseItem.of(i -> i
                        .operationType(OperationType.Create)
                        .index(INDEX_NAME)
                        .id("doc-id")
                        .status(201))));
    }

    private static BulkResponse forbiddenResponse() {
        return BulkResponse.of(b -> b
                .errors(true)
                .took(1)
                .items(BulkResponseItem.of(i -> i
                        .operationType(OperationType.Create)
                        .index(INDEX_NAME)
                        .status(403)
                        .error(ErrorCause.of(e -> e
                                .type("security_exception")
                                .reason("action [indices:admin/auto_create] is unauthorized"))))));
    }
}
