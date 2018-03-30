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
 *
 */

package stroom.explorer;

import stroom.explorer.shared.BulkActionResult;
import stroom.explorer.shared.ExplorerServiceDeleteAction;
import stroom.security.Security;
import stroom.task.AbstractTaskHandler;
import stroom.task.TaskHandlerBean;

import javax.inject.Inject;

@TaskHandlerBean(task = ExplorerServiceDeleteAction.class)
class ExplorerServiceDeleteHandler extends AbstractTaskHandler<ExplorerServiceDeleteAction, BulkActionResult> {
    private final ExplorerService explorerService;
    private final Security security;

    @Inject
    ExplorerServiceDeleteHandler(final ExplorerService explorerService,
                                 final Security security) {
        this.explorerService = explorerService;
        this.security = security;
    }

    @Override
    public BulkActionResult exec(final ExplorerServiceDeleteAction action) {
        return security.secureResult(() -> explorerService.delete(action.getDocRefs()));
    }
}
