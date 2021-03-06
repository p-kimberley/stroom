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

import java.io.Serializable;

@FunctionDef(
        name = First.NAME,
        commonCategory = FunctionCategory.SELECTION,
        commonReturnType = Val.class,
        commonReturnDescription = "",
        signatures = @FunctionSignature(
                description = "Selects the first value found in the group even if it is " +
                        Null.NAME + "() or " + Err.NAME + "(). " +
                        "If no explicit ordering is set then the value selected is indeterminate.",
                returnDescription = "The first value of the group.",
                args = {
                        @FunctionArg(
                                name = "values",
                                description = "Grouped field or the result of another function",
                                argType = Val.class)
                }))
public class First extends AbstractSelectorFunction implements Serializable {

    static final String NAME = "first";
    private static final long serialVersionUID = -305845496003936297L;

    public First(final String name) {
        super(name, 1, 1);
    }

    @Override
    public Generator createGenerator() {
        return new FirstSelector(super.createGenerator());
    }

    public static class FirstSelector extends Selector {

        private static final long serialVersionUID = 8153777070911899616L;

        FirstSelector(final Generator childGenerator) {
            super(childGenerator);
        }

        public Val select(final Selection<Val> selection) {
            if (selection.size() > 0) {
                return selection.get(0);
            }
            return eval();
        }
    }
}
