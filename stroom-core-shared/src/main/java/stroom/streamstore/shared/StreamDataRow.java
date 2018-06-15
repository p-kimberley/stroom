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

package stroom.streamstore.shared;

import stroom.docref.SharedObject;
import stroom.streamstore.meta.api.Stream;

import java.util.HashMap;
import java.util.Map;

public class StreamDataRow implements SharedObject {
    private static final long serialVersionUID = -8198186456924478908L;

    private Stream stream;
    private Map<String, String> attributes = new HashMap<>();

    public StreamDataRow() {
        // Default constructor necessary for GWT serialisation.
    }

    public StreamDataRow(Stream stream) {
        setStream(stream);
    }

    public Stream getStream() {
        return stream;
    }

    public void setStream(Stream stream) {
        this.stream = stream;
    }

    public void addAttribute(final String name, final String value) {
        attributes.put(name, value);
    }

    public String getAttributeValue(final String name) {
        return attributes.get(name);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof StreamDataRow)) return false;

        final StreamDataRow that = (StreamDataRow) o;

        return stream.equals(that.stream);
    }

    @Override
    public int hashCode() {
        return stream.hashCode();
    }
}
