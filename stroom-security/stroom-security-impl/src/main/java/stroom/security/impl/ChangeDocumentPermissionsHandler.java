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

package stroom.security.impl;

import stroom.security.shared.ChangeDocumentPermissionsAction;
import stroom.task.api.AbstractTaskHandler;
import stroom.util.shared.VoidResult;

import javax.inject.Inject;

class ChangeDocumentPermissionsHandler
        extends AbstractTaskHandler<ChangeDocumentPermissionsAction, VoidResult> {
    private final DocumentPermissionService documentPermissionService;

    @Inject
    ChangeDocumentPermissionsHandler(final DocumentPermissionService documentPermissionService) {
        this.documentPermissionService = documentPermissionService;
    }

    @Override
    public VoidResult exec(final ChangeDocumentPermissionsAction action) {
        documentPermissionService.changeDocumentPermissions(action.getDocRef(), action.getChangeSet(), action.getCascade());
        return VoidResult.INSTANCE;
    }
}