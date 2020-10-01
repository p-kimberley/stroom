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

package stroom.dashboard.expression.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FieldNameIndex {
    private final Map<String, Integer> fieldToPos;
    private final List<String> fieldNames;
    private final boolean autoCreate;

    FieldNameIndex(final String[] names) {
        this.autoCreate = false;
        final Map<String, Integer> fieldToPos = new HashMap<>();
        final List<String> fieldNames = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            fieldNames.add(name);
            fieldToPos.put(name, i);
        }
        this.fieldToPos = Collections.unmodifiableMap(fieldToPos);
        this.fieldNames = Collections.unmodifiableList(fieldNames);
    }

    FieldNameIndex() {
        this.autoCreate = true;
        fieldToPos = new HashMap<>();
        fieldNames = new ArrayList<>();
    }

    int create(final String fieldName) {
        if (autoCreate) {
            return fieldToPos.compute(fieldName, (k, v) -> {
                if (v == null) {
                    fieldNames.add(k);
                    v = fieldNames.size() - 1;
                }
                return v;
            });
        }

        Integer currentIndex = fieldToPos.get(fieldName);
        if (currentIndex == null) {
            return -1;
        }
        return currentIndex;
    }

    String[] getNames() {
        return fieldNames.toArray(new String[0]);
    }
}
