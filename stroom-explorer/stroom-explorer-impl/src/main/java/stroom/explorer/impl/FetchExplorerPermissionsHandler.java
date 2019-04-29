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

import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.ExplorerPermissions;
import stroom.explorer.shared.FetchExplorerPermissionsAction;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.shared.SharedMap;

import javax.inject.Inject;
import java.util.Map;


class FetchExplorerPermissionsHandler
        extends AbstractTaskHandler<FetchExplorerPermissionsAction, SharedMap<ExplorerNode, ExplorerPermissions>> {
    private final ExplorerServiceImpl explorerService;
    private final ExplorerEventLog explorerEventLog;

    @Inject
    FetchExplorerPermissionsHandler(final ExplorerServiceImpl explorerService,
                                    final ExplorerEventLog explorerEventLog) {
        this.explorerService = explorerService;
        this.explorerEventLog = explorerEventLog;
    }

    @Override
    public SharedMap<ExplorerNode, ExplorerPermissions> exec(final FetchExplorerPermissionsAction action) {
        Map<ExplorerNode, ExplorerPermissions> resultMap;
        try {
            resultMap = explorerService.fetchPermissions(action.getExplorerNodeList());
            explorerEventLog.fetchPermissions(action.getExplorerNodeList(), null);
        } catch (final RuntimeException e) {
            explorerEventLog.fetchPermissions(action.getExplorerNodeList(), e);
            throw e;
        }

        return new SharedMap<>(resultMap);
    }
}
