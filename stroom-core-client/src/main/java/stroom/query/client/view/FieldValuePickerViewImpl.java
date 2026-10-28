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

import stroom.query.client.presenter.FieldValuePickerPresenter.FieldValuePickerView;
import stroom.query.shared.FieldValueCount;
import stroom.query.shared.FieldValues;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

import java.util.function.Consumer;

public class FieldValuePickerViewImpl extends ViewImpl implements FieldValuePickerView {

    private static final NumberFormat PERCENT_FORMAT = NumberFormat.getFormat("0.0");
    private static final String COMMA = ",";

    private final FlowPanel panel;
    private final TextBox filterBox;
    private final Label scopeLabel;
    private final FlowPanel valuesPanel;

    private Consumer<String> filterHandler;
    private Consumer<String> pickHandler;
    private String firstValue;

    @Inject
    public FieldValuePickerViewImpl() {
        filterBox = new TextBox();
        filterBox.addStyleName("fieldValuePicker-filter");
        filterBox.getElement().setAttribute("placeholder", "Filter values");
        filterBox.addKeyUpHandler(event -> {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                if (firstValue != null && pickHandler != null) {
                    pickHandler.accept(firstValue);
                }
            } else if (filterHandler != null) {
                filterHandler.accept(filterBox.getText());
            }
        });

        scopeLabel = new Label("", false);
        scopeLabel.setStyleName("fieldValuePicker-scope");

        valuesPanel = new FlowPanel();
        valuesPanel.setStyleName("fieldValuePicker-values");

        panel = new FlowPanel();
        panel.setStyleName("fieldValuePicker");
        panel.add(filterBox);
        panel.add(scopeLabel);
        panel.add(valuesPanel);
    }

    @Override
    public void init(final boolean filterable,
                     final Consumer<String> filterHandler,
                     final Consumer<String> pickHandler) {
        this.filterHandler = filterHandler;
        this.pickHandler = pickHandler;
        filterBox.setText("");
        filterBox.setVisible(filterable);
        scopeLabel.setText("");
        valuesPanel.clear();
        firstValue = null;
    }

    @Override
    public void setValues(final FieldValues fieldValues, final String scope, final boolean allowCommas) {
        valuesPanel.clear();
        firstValue = null;
        scopeLabel.setText(scope + " - " + fieldValues.getTotalCount() + " documents");

        for (final FieldValueCount valueCount : fieldValues.getValues()) {
            final boolean pickable = allowCommas || !valueCount.getValue().contains(COMMA);
            if (pickable && firstValue == null) {
                firstValue = valueCount.getValue();
            }
            valuesPanel.add(createRow(valueCount, fieldValues.getTotalCount(), pickable));
        }
    }

    @Override
    public void setMessage(final String message) {
        valuesPanel.clear();
        firstValue = null;
        final Label label = new Label(message);
        label.setStyleName("fieldValuePicker-message");
        valuesPanel.add(label);
    }

    @Override
    public void focus() {
        if (filterBox.isVisible()) {
            filterBox.setFocus(true);
        }
    }

    @Override
    public Widget asWidget() {
        return panel;
    }

    private Widget createRow(final FieldValueCount valueCount, final long totalCount, final boolean pickable) {
        final double percent = totalCount > 0
                ? Math.min(100, valueCount.getCount() * 100D / totalCount)
                : 0;

        final Label valueLabel = new Label(valueCount.getValue(), false);
        valueLabel.setStyleName("fieldValuePicker-value");
        valueLabel.setTitle(pickable
                ? valueCount.getValue() + " (" + valueCount.getCount() + " documents)"
                : "Values containing a comma can't be used with 'in'");

        final FlowPanel barFill = new FlowPanel();
        barFill.setStyleName("fieldValuePicker-barFill");
        barFill.getElement().getStyle().setWidth(percent, Unit.PCT);
        final FlowPanel bar = new FlowPanel();
        bar.setStyleName("fieldValuePicker-bar");
        bar.add(barFill);

        final Label percentLabel = new Label(PERCENT_FORMAT.format(percent) + "%", false);
        percentLabel.setStyleName("fieldValuePicker-percent");

        final FlowPanel row = new FlowPanel();
        row.setStyleName("fieldValuePicker-row");
        row.add(valueLabel);
        row.add(bar);
        row.add(percentLabel);
        if (pickable) {
            row.addDomHandler(event -> {
                if (pickHandler != null) {
                    pickHandler.accept(valueCount.getValue());
                }
            }, ClickEvent.getType());
        } else {
            row.addStyleName("fieldValuePicker-row--disabled");
        }
        return row;
    }
}
