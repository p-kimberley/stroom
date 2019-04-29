package stroom.security.impl;

import stroom.security.shared.CopyPermissionsFromParentAction;
import stroom.security.shared.DocumentPermissions;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;

public class CopyPermissionsFromParentHandler
        extends AbstractTaskHandler<CopyPermissionsFromParentAction, DocumentPermissions> {

    private final DocumentPermissionService documentPermissionService;

    @Inject
    CopyPermissionsFromParentHandler(
            final DocumentPermissionService documentPermissionService) {
        this.documentPermissionService = documentPermissionService;
    }

    @Override
    public DocumentPermissions exec(CopyPermissionsFromParentAction action) {
        return documentPermissionService.copyPermissionsFromParent(action.getDocRef());
    }
}