/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.query.common.v2;

import stroom.datasource.api.v2.AbstractField;
import stroom.docref.DocRef;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.TableSettings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(Include.NON_NULL)
public class TableCoprocessorSettings implements CoprocessorSettings {
    @JsonProperty
    private final QueryKey queryKey;
    @JsonProperty
    private final String coprocessorId;
    @JsonProperty
    private final List<String> componentIdList;
    @JsonProperty
    private final TableSettings tableSettings;
    @JsonProperty
    private final String[] fieldNames;
    @JsonProperty
    private final AbstractField[] fields;

    @JsonCreator
    public TableCoprocessorSettings(@JsonProperty("queryKey") final QueryKey queryKey,
                                    @JsonProperty("coprocessorId") final String coprocessorId,
                                    @JsonProperty("componentIdList") final List<String> componentIdList,
                                    @JsonProperty("tableSettings") final TableSettings tableSettings,
                                    @JsonProperty("fieldNames") final String[] fieldNames,
                                    @JsonProperty("fields") final AbstractField[] fields) {
        this.queryKey = queryKey;
        this.coprocessorId = coprocessorId;
        this.componentIdList = componentIdList;
        this.tableSettings = tableSettings;
        this.fieldNames = fieldNames;
        this.fields = fields;
    }

    public QueryKey getQueryKey() {
        return queryKey;
    }

    @Override
    public String getCoprocessorId() {
        return coprocessorId;
    }

    public List<String> getComponentIdList() {
        return componentIdList;
    }

    public TableSettings getTableSettings() {
        return tableSettings;
    }

    @Override
    public boolean extractValues() {
        return tableSettings.extractValues();
    }

    @Override
    public DocRef getExtractionPipeline() {
        return tableSettings.getExtractionPipeline();
    }

    @Override
    public String[] getFieldNames() {
        return fieldNames;
    }

    @Override
    public AbstractField[] getFields() {
        return fields;
    }
}
