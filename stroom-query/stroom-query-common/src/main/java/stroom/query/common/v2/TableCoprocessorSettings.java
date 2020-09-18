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

import stroom.docref.DocRef;
import stroom.query.api.v2.TableSettings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(Include.NON_NULL)
public class TableCoprocessorSettings implements CoprocessorSettings {
    @JsonProperty
    private final String key;
    @JsonProperty
    private final List<String> componentIdList;
    @JsonProperty
    private final TableSettings tableSettings;

    @JsonCreator
    public TableCoprocessorSettings(@JsonProperty("key") final String key,
                                    @JsonProperty("componentIdList") final List<String> componentIdList,
                                    @JsonProperty("tableSettings") final TableSettings tableSettings) {
        this.key = key;
        this.componentIdList = componentIdList;
        this.tableSettings = tableSettings;
    }

    @Override
    public String getKey() {
        return key;
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
}
