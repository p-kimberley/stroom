package stroom.security.impl;

import stroom.security.shared.DocumentPermissions;

import java.util.Set;

public interface DocumentPermissionDao {
    Set<String> getPermissionsForDocumentForUser(String docRefUuid, String userUuid);

    DocumentPermissions getPermissionsForDocument(String docRefUuid);

    void addPermission(String docRefUuid, String userUuid, String permission);

    void removePermission(String docRefUuid, String userUuid, String permission);

    void clearDocumentPermissionsForUser(String docRefUuid, String userUuid);

    void clearDocumentPermissions(String docRefUuid);

}
