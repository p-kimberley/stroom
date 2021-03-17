/*
 * Copyright 2016 Crown Copyright
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

package stroom.search.elastic;

import stroom.search.elastic.shared.ElasticConnectionConfig;
import stroom.search.elastic.shared.ElasticConnectionTestAction;
import stroom.task.server.AbstractTaskHandler;
import stroom.task.server.TaskHandlerBean;
import stroom.util.shared.SharedString;
import stroom.util.spring.StroomScope;

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.MainResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.springframework.context.annotation.Scope;

@TaskHandlerBean(task = ElasticConnectionTestAction.class)
@Scope(StroomScope.TASK)
public class ElasticConnectionTestHandler extends AbstractTaskHandler<ElasticConnectionTestAction, SharedString> {
    @Override
    public SharedString exec(final ElasticConnectionTestAction action) {
        try {
            final ElasticConnectionConfig connectionConfig = action.getElasticIndex().getConnectionConfig();
            final RestHighLevelClient elasticClient = new ElasticClientFactory().create(connectionConfig);

            MainResponse response = elasticClient.info(RequestOptions.DEFAULT);

            final StringBuilder sb = new StringBuilder();
            sb.append("Cluster URLs: ");
            sb.append(connectionConfig.getConnectionUrls());
            sb.append("\nCluster name: ");
            sb.append(response.getClusterName());
            sb.append("\nCluster UUID: ");
            sb.append(response.getClusterUuid());
            sb.append("\nNode name: ");
            sb.append(response.getNodeName());
            sb.append("\nVersion: ");
            sb.append(response.getVersion().getNumber());

            // Check whether the specified index exists
            final String indexName = action.getElasticIndex().getIndexName();
            final GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
            GetIndexResponse getIndexResponse = elasticClient.indices().get(getIndexRequest, RequestOptions.DEFAULT);
            if (getIndexResponse.getIndices().length < 1) {
                throw new ResourceNotFoundException("Index '" + indexName + "' was not found");
            }

            return SharedString.wrap(sb.toString());
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}