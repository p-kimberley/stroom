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

package stroom.explorer.impl;

import stroom.explorer.api.ExplorerService;
import stroom.explorer.shared.BulkActionResult;
import stroom.explorer.shared.ExplorerServiceDeleteAction;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;


class ExplorerServiceDeleteHandler extends AbstractTaskHandler<ExplorerServiceDeleteAction, BulkActionResult> {
    private final ExplorerServiceImpl explorerService;
    private final ExplorerEventLog explorerEventLog;

    @Inject
    ExplorerServiceDeleteHandler(final ExplorerServiceImpl explorerService,
                                 final ExplorerEventLog explorerEventLog) {
        this.explorerService = explorerService;
        this.explorerEventLog = explorerEventLog;
    }

    @Override
    public BulkActionResult exec(final ExplorerServiceDeleteAction action) {
        BulkActionResult result = null;
        try {
            result = explorerService.delete(action.getDocRefs());
            explorerEventLog.delete(action.getDocRefs(), result, null);
        } catch (final RuntimeException e) {
            explorerEventLog.delete(action.getDocRefs(), result, e);
            throw e;
        }

        return result;
    }
}
