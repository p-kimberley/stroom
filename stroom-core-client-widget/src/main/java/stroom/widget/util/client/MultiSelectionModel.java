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

package stroom.widget.util.client;

import com.google.gwt.event.shared.HandlerRegistration;

import java.util.List;

public interface MultiSelectionModel<T> {
    /**
     * Get a list of all selected items.
     */
    List<T> getSelectedItems();

    /**
     * Tests if the specified item is selected.
     */
    boolean isSelected(final T item);

    /**
     * Sets the selected state of the specified item.
     */
    void setSelected(T item, boolean selected);

    /**
     * Gets the most recently selected item or only selected item if an item is selected, null otherwise.
     */
    T getSelected();

    /**
     * Sets the specified item as the only selected item, i.e. clears the current selection and sets a single item selected.
     */
    void setSelected(final T item);

    /**
     * Clears all selected items.
     */
    void clear();

    /**
     * Add a handler to listen to selection actions on the multi selection model.
     */
    HandlerRegistration addSelectionHandler(MultiSelectEvent.Handler handler);
}
