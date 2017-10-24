/*
 * Copyright 2017 Crown Copyright
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

package stroom.query.client;

import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionItem;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionTerm;
import stroom.widget.htree.client.treelayout.util.DefaultTreeForTreeLayout;

import java.util.List;

public class ExpressionModel {
    public DefaultTreeForTreeLayout<Item> getTreeFromExpression(final ExpressionOperator expression) {
        Item root = null;

        if (expression != null) {
            root = convert(expression);
        }

        final DefaultTreeForTreeLayout<Item> tree = new DefaultTreeForTreeLayout<>(root);
        addChildrenToTree(tree, root, expression);
        return tree;
    }

    public ExpressionOperator getExpressionFromTree(final DefaultTreeForTreeLayout<Item> tree) {
        final Item item = tree.getRoot();
        if (item != null && item instanceof Operator) {
            final Operator source = (Operator) item;
            final ExpressionOperator.Builder dest = new ExpressionOperator.Builder(source.getEnabled(), source.getOp());
            addChildrenFromTree(source, dest, tree);
            return dest.build();
        }
        return null;
    }

    private Item convert(final ExpressionItem expressionItem) {
        if (expressionItem != null) {
            if (expressionItem instanceof ExpressionOperator) {
                final ExpressionOperator expressionOperator = (ExpressionOperator) expressionItem;

                final Operator operator = new Operator();
                operator.setOp(expressionOperator.getOp());
                operator.setEnabled(expressionOperator.getEnabled());
                return operator;

            } else if (expressionItem instanceof ExpressionTerm) {
                final ExpressionTerm expressionTerm = (ExpressionTerm) expressionItem;

                final Term term = new Term();
                term.setField(expressionTerm.getField());
                term.setCondition(expressionTerm.getCondition());
                term.setValue(expressionTerm.getValue());
                term.setDictionary(expressionTerm.getDictionary());
                term.setEnabled(expressionTerm.getEnabled());

                return term;
            }
        }

        return null;
    }

    private void addChildrenToTree(final DefaultTreeForTreeLayout<Item> tree,
                                   final Item parent, final ExpressionOperator expressionOperator) {
        if (expressionOperator != null) {
            final List<ExpressionItem> children = expressionOperator.getChildren();
            if (children != null) {
                for (final ExpressionItem child : children) {
                    final Item item = convert(child);
                    tree.addChild(parent, item);
                    if (child instanceof ExpressionOperator) {
                        addChildrenToTree(tree, item, (ExpressionOperator) child);
                    }
                }
            }
        }
    }

    private void addChildrenFromTree(final Operator source, final ExpressionOperator.ABuilder<?, ?> dest, final DefaultTreeForTreeLayout<Item> tree) {
        final List<Item> children = tree.getChildren(source);
        if (children != null) {
            for (final Item child : children) {
                if (child instanceof Operator) {
                    final Operator operator = (Operator) child;
                    final ExpressionOperator.OBuilder<?> childDest = dest.addOperator(operator.getEnabled(), operator.getOp());
                    addChildrenFromTree(operator, childDest, tree);
                } else if (child instanceof Term) {
                    final Term term = (Term) child;
                    dest.addTerm()
                            .enabled(term.getEnabled())
                            .field(term.getField())
                            .condition(term.getCondition())
                            .value(term.getValue())
                            .dictionary(term.getDictionary());
                }
            }
        }
    }
}
