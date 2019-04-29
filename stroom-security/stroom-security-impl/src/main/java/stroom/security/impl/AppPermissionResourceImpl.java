package stroom.security.impl;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Set;

public class AppPermissionResourceImpl implements AppPermissionResource {
    private final AppPermissionService appPermissionService;

    @Inject
    public AppPermissionResourceImpl(final AppPermissionService appPermissionService) {
        this.appPermissionService = appPermissionService;
    }

    @Override
    public Response getPermissionNamesForUser(final String userUuid) {
        final Set<String> permissions = appPermissionService.getPermissionNamesForUser(userUuid);
        return Response.ok(permissions).build();
    }

    @Override
    public Response getPermissionNamesForUserName(final String userName) {
        final Set<String> permissions = appPermissionService.getPermissionNamesForUserName(userName);
        return Response.ok(permissions).build();
    }


    @Override
    public Response getAllPermissionNames() {
        final Set<String> allPermissions = appPermissionService.getAllPermissionNames();
        return Response.ok(allPermissions).build();
    }

    @Override
    public Response addPermission(final String userUuid,
                                  final String permission) {
        appPermissionService.addPermission(userUuid, permission);
        return Response.noContent().build();
    }

    @Override
    public Response removePermission(final String userUuid,
                                     final String permission) {
        appPermissionService.removePermission(userUuid, permission);
        return Response.noContent().build();
    }
}
