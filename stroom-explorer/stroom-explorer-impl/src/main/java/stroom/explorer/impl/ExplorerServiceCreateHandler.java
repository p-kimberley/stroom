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

import stroom.docref.DocRef;
import stroom.explorer.api.ExplorerService;
import stroom.explorer.shared.ExplorerServiceCreateAction;
import stroom.explorer.shared.SharedDocRef;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;

class ExplorerServiceCreateHandler extends AbstractTaskHandler<ExplorerServiceCreateAction, SharedDocRef> {
    private final ExplorerServiceImpl explorerService;
    private final ExplorerEventLog explorerEventLog;

    @Inject
    ExplorerServiceCreateHandler(final ExplorerServiceImpl explorerService,
                                 final ExplorerEventLog explorerEventLog) {
        this.explorerService = explorerService;
        this.explorerEventLog = explorerEventLog;
    }

    @Override
    public SharedDocRef exec(final ExplorerServiceCreateAction action) {
        DocRef docRef;
        try {
            docRef = explorerService.create(action.getDocType(), action.getDocName(), action.getDestinationFolderRef(), action.getPermissionInheritance());
            explorerEventLog.create(docRef.getType(), docRef.getName(), docRef.getUuid(), action.getDestinationFolderRef(), action.getPermissionInheritance(), null);
        } catch (final RuntimeException e) {
            explorerEventLog.create(action.getDocType(), action.getDocName(), null, action.getDestinationFolderRef(), action.getPermissionInheritance(), e);
            throw e;
        }

        return SharedDocRef.create(docRef);
    }
}