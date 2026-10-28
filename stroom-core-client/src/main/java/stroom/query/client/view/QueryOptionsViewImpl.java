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

package stroom.query.client.view;

import stroom.item.client.SelectionBox;
import stroom.query.api.datasource.QueryOptionChoice;
import stroom.query.api.datasource.QueryOptionDefinition;
import stroom.query.client.presenter.QueryOptionsPresenter.QueryOptionsView;
import stroom.widget.form.client.FormGroup;
import stroom.widget.tickbox.client.view.CustomCheckBox;
import stroom.widget.valuespinner.client.ValueSpinner;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryOptionsViewImpl extends ViewImpl implements QueryOptionsView {

    private final FlowPanel panel;
    private final Map<String, OptionControl> controls = new LinkedHashMap<>();
    private final Map<String, FormGroup> formGroups = new HashMap<>();
    private final Map<String, QueryOptionDefinition> definitions = new HashMap<>();

    @Inject
    public QueryOptionsViewImpl() {
        panel = new FlowPanel();
        panel.setStyleName("max form queryOptions");
    }

    @Override
    public void setDefinitions(final List<QueryOptionDefinition> definitions, final Runnable changeHandler) {
        panel.clear();
        controls.clear();
        formGroups.clear();
        this.definitions.clear();

        for (final QueryOptionDefinition definition : definitions) {
            final OptionControl control = createControl(definition, changeHandler);

            final FormGroup formGroup = new FormGroup();
            formGroup.setLabel(definition.getDisplayName());
            if (definition.getDescription() != null) {
                formGroup.setHelpText(definition.getDescription());
            }
            formGroup.add(control.asWidget());
            panel.add(formGroup);

            controls.put(definition.getId(), control);
            formGroups.put(definition.getId(), formGroup);
            this.definitions.put(definition.getId(), definition);
        }
    }

    @Override
    public void setValues(final Map<String, String> values) {
        controls.forEach((id, control) -> {
            final String value = values.get(id);
            control.setValue(value != null
                    ? value
                    : definitions.get(id).getDefaultValue());
        });
    }

    @Override
    public Map<String, String> getValues() {
        final Map<String, String> values = new LinkedHashMap<>();
        controls.forEach((id, control) -> values.put(id, control.getValue()));
        return values;
    }

    @Override
    public void setDisabledOptions(final Set<String> disabledOptionIds) {
        controls.forEach((id, control) -> {
            final boolean disabled = disabledOptionIds.contains(id);
            if (disabled) {
                control.setValue(definitions.get(id).getDefaultValue());
            }
            control.setEnabled(!disabled);
            formGroups.get(id).setDisabled(disabled);
        });
    }

    @Override
    public void focus() {
        controls.values().stream()
                .findFirst()
                .ifPresent(OptionControl::focus);
    }

    @Override
    public Widget asWidget() {
        return panel;
    }

    private OptionControl createControl(final QueryOptionDefinition definition, final Runnable changeHandler) {
        return switch (definition.getType()) {
            case BOOLEAN -> createCheckBox(changeHandler);
            case CHOICE -> createSelectionBox(definition, changeHandler);
            case INTEGER -> createSpinner(definition, changeHandler);
        };
    }

    private OptionControl createCheckBox(final Runnable changeHandler) {
        final CustomCheckBox checkBox = new CustomCheckBox();
        checkBox.addValueChangeHandler(e -> changeHandler.run());
        return new OptionControl() {
            @Override
            public String getValue() {
                return String.valueOf(Boolean.TRUE.equals(checkBox.getValue()));
            }

            @Override
            public void setValue(final String value) {
                checkBox.setValue(Boolean.valueOf(value));
            }

            @Override
            public void setEnabled(final boolean enabled) {
                checkBox.setEnabled(enabled);
            }

            @Override
            public void focus() {
                checkBox.focus();
            }

            @Override
            public Widget asWidget() {
                return checkBox;
            }
        };
    }

    private OptionControl createSelectionBox(final QueryOptionDefinition definition, final Runnable changeHandler) {
        final SelectionBox<QueryOptionChoice> selectionBox = new SelectionBox<>();
        selectionBox.addItems(definition.getChoices());
        selectionBox.addValueChangeHandler(e -> changeHandler.run());
        return new OptionControl() {
            @Override
            public String getValue() {
                final QueryOptionChoice choice = selectionBox.getValue();
                return choice != null
                        ? choice.getValue()
                        : definition.getDefaultValue();
            }

            @Override
            public void setValue(final String value) {
                for (final QueryOptionChoice choice : definition.getChoices()) {
                    if (choice.getValue().equals(value)) {
                        selectionBox.setValue(choice);
                    }
                }
            }

            @Override
            public void setEnabled(final boolean enabled) {
                selectionBox.setEnabled(enabled);
            }

            @Override
            public void focus() {
                selectionBox.focus();
            }

            @Override
            public Widget asWidget() {
                return selectionBox;
            }
        };
    }

    private OptionControl createSpinner(final QueryOptionDefinition definition, final Runnable changeHandler) {
        final ValueSpinner spinner = new ValueSpinner();
        if (definition.getMinValue() != null) {
            spinner.setMin(definition.getMinValue());
        }
        if (definition.getMaxValue() != null) {
            spinner.setMax(definition.getMaxValue());
        }
        spinner.addValueChangeHandler(e -> changeHandler.run());
        return new OptionControl() {
            @Override
            public String getValue() {
                return String.valueOf(spinner.getIntValue());
            }

            @Override
            public void setValue(final String value) {
                spinner.setValue(Long.valueOf(value));
            }

            @Override
            public void setEnabled(final boolean enabled) {
                spinner.setEnabled(enabled);
            }

            @Override
            public void focus() {
                spinner.focus();
            }

            @Override
            public Widget asWidget() {
                return spinner;
            }
        };
    }


    // --------------------------------------------------------------------------------


    /// Edits the value of one option.
    private interface OptionControl {

        String getValue();

        void setValue(String value);

        void setEnabled(boolean enabled);

        void focus();

        Widget asWidget();
    }
}
