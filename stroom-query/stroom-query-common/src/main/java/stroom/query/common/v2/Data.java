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

import stroom.dashboard.expression.v1.Val;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Data {
    public static final GroupKey ROOT_KEY = new GroupKey(null, (List<Val>) null);

    private final Map<GroupKey, Items> childMap;
    private final long size;
    private final long totalSize;

    public Data(final Map<GroupKey, Items> childMap, final long size, final long totalSize) {
        this.childMap = childMap;
        this.size = size;
        this.totalSize = totalSize;
    }

    public DataItems get() {
        return get(ROOT_KEY);
    }

    public DataItems get(final GroupKey groupKey) {
        final Items items = childMap.get(groupKey);
        if (items == null) {
            return new DataItems() {
                @Override
                public Iterator<Item> iterator() {
                    return Collections.emptyIterator();
                }

                @Override
                public int size() {
                    return 0;
                }
            };
        }

        return new DataItems() {
            @Override
            public Iterator<Item> iterator() {
                return items.iterator();
            }

            @Override
            public int size() {
                return items.size();
            }
        };
    }

    public long getSize() {
        return size;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public interface DataItems extends Iterable<Item> {
        int size();
    }
}
