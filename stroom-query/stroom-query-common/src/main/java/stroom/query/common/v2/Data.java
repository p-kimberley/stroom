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

import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.Selector;
import stroom.dashboard.expression.v1.Val;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Data {
    public static final GroupKey ROOT_KEY = new GroupKey(null, (List<Val>) null);
    private static final DataItems EMPTY_DATA_ITEMS = new DataItems() {
        @Override
        public Iterator<DataItem> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    };

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
            return EMPTY_DATA_ITEMS;
        }











        final List<DataItem> list = new ArrayList<>(items.size());
        for (final Item item : items) {
            final Val[] arr = getValues(item);
            final DataItem values = new DataItem(item.getKey(), arr, item.getDepth());
            list.add(values);
        }












        return new DataItems() {
            @Override
            public Iterator<DataItem> iterator() {
                return list.iterator();
            }

            @Override
            public int size() {
                return list.size();
            }
        };
    }

    private Val[] getValues(final Item item) {
        final GroupKey groupKey = item.getKey();
        final Generator[] generators = item.getGenerators();
        final Val[] values = new Val[generators.length];
        for (int i = 0; i < generators.length; i++) {
            final Generator generator = item.getGenerators()[i];
            Val val;

            if (groupKey != null && generator instanceof Selector) {
                // If the generator is a selector then select a child row.
                final Items childItems = childMap.get(groupKey);
                if (childItems != null) {
                    // Create a list of child generators.
                    final List<Generator> childGenerators = new ArrayList<>(childItems.size());
                    for (final Item childItem : childItems) {
                        final Generator childGenerator = childItem.getGenerators()[i];
                        childGenerators.add(childGenerator);
                    }

                    // Make the selector select from the list of child generators.
                    final Selector selector = (Selector) generator;
                    val = selector.select(childGenerators.toArray(new Generator[0]));

                } else {
                    // If there are are no child items then just evaluate the inner expression
                    // provided to the selector function.
                    val = generator.eval();
                }
            } else {
                // Convert all list into fully resolved objects evaluating functions where
                // necessary.
                val = generator.eval();
            }

            values[i] = val;
        }
        return values;
    }

    public long getSize() {
        return size;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public interface DataItems extends Iterable<DataItem> {
        int size();
    }
}
