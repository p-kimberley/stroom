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

package stroom.pipeline.shared.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Elements", propOrder = {"add", "remove"})
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({"add, remove"})
public class PipelineElements {

    @XmlElementWrapper(name = "add")
    @XmlElement(name = "element")
    @JsonProperty
    private final List<PipelineElement> add;

    @XmlElementWrapper(name = "remove")
    @XmlElement(name = "element")
    @JsonProperty
    private final List<PipelineElement> remove;

    public PipelineElements() {
        add = new ArrayList<>();
        remove = new ArrayList<>();
    }

    @JsonCreator
    public PipelineElements(@JsonProperty("add") final List<PipelineElement> add,
                            @JsonProperty("remove") final List<PipelineElement> remove) {
        this.add = add;
        this.remove = remove;
    }

    public List<PipelineElement> getAdd() {
        return add;
    }

    public List<PipelineElement> getRemove() {
        return remove;
    }
}
