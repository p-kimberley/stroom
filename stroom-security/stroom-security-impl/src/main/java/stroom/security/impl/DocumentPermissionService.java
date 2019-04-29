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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.docref.DocRef;
import stroom.explorer.api.ExplorerNodeService;
import stroom.explorer.shared.DocumentTypes;
import stroom.explorer.shared.ExplorerNode;
import stroom.security.shared.ChangeDocumentPermissionsAction;
import stroom.security.shared.ChangeDocumentPermissionsAction.Cascade;
import stroom.security.shared.ChangeSet;
import stroom.security.shared.DocumentPermissionNames;
import stroom.security.shared.DocumentPermissions;
import stroom.security.shared.PermissionException;
import stroom.security.shared.User;
import stroom.security.shared.UserPermission;
import stroom.util.shared.EntityServiceException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Singleton
class DocumentPermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentPermissionService.class);

    private final DocumentPermissionDao documentPermissionDao;
    private final DocumentTypePermissions documentTypePermissions;
    private final DocumentPermissionsCache documentPermissionsCache;
    private final UserGroupsCache userGroupsCache;
    private final AppPermissionService appPermissionService;
    private final ExplorerNodeService explorerNodeService;

    @Inject
    public DocumentPermissionService(final DocumentPermissionDao documentPermissionDao,
                                     final DocumentTypePermissions documentTypePermissions,
                                     final DocumentPermissionsCache documentPermissionsCache,
                                     final UserGroupsCache userGroupsCache,
                                     final AppPermissionService appPermissionService,
                                     final ExplorerNodeService explorerNodeService) {
        this.documentPermissionDao = documentPermissionDao;
        this.documentTypePermissions = documentTypePermissions;
        this.documentPermissionsCache = documentPermissionsCache;
        this.userGroupsCache = userGroupsCache;
        this.appPermissionService = appPermissionService;
        this.explorerNodeService = explorerNodeService;
    }

    public boolean hasDocumentPermission(final String documentType, final String documentId, final String permission) {
        // Let administrators do anything.
        if (appPermissionService.isAdmin()) {
            return true;
        }

        // Get the current user.
        final User userRef = getUser();

        final DocRef docRef = new DocRef(documentType, documentId);
        boolean result = hasDocumentPermission(userRef, docRef, permission);

        // If the user doesn't have read permission then check to see if the current task has been set to have elevated permissions.
        if (!result && DocumentPermissionNames.READ.equals(permission)) {
            if (CurrentUserState.isElevatePermissions()) {
                result = hasDocumentPermission(userRef, docRef, DocumentPermissionNames.USE);
            }
        }

        return result;
    }

    private boolean hasDocumentPermission(final User userRef, final DocRef docRef, final String permission) {
        // See if the user has an explicit permission.
        boolean result = hasUserDocumentPermission(userRef, docRef, permission);

        // See if the user belongs to a group that has permission.
        if (!result) {
            final List<User> userGroups = userGroupsCache.get(userRef.getUuid());
            result = hasUserGroupsDocumentPermission(userGroups, docRef, permission);
        }

        return result;
    }

    private boolean hasUserGroupsDocumentPermission(final List<User> userGroups, final DocRef docRef, final String permission) {
        if (userGroups != null) {
            for (final User userGroup : userGroups) {
                final boolean result = hasUserDocumentPermission(userGroup, docRef, permission);
                if (result) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasUserDocumentPermission(final User userRef,
                                              final DocRef docRef,
                                              final String permission) {
        final DocumentPermissions documentPermissions = documentPermissionsCache.get(docRef.getUuid());
        if (documentPermissions != null) {
            final Set<String> permissions = documentPermissions.getPermissionsForUser(userRef.getUuid());
            if (permissions != null) {
                String perm = permission;
                while (perm != null) {
                    if (permissions.contains(perm)) {
                        return true;
                    }

                    // If the user doesn't explicitly have this permission then see if they have a higher permission that infers this one.
                    perm = DocumentPermissionNames.getHigherPermission(perm);
                }
            }
        }
        return false;
    }

    void clearDocumentPermissions(final String documentType, final String documentUuid) {
        if (!hasDocumentPermission(documentType, documentUuid, DocumentPermissionNames.OWNER)) {
            throw new PermissionException("Only an admin or owner can remove document permissions");
        }

        final DocRef docRef = new DocRef(documentType, documentUuid);
        documentPermissionDao.clearDocumentPermissions(documentUuid);

        // Make sure cache updates for the document.
        documentPermissionsCache.remove(docRef.getUuid());
    }

    void addDocumentPermissions(final String sourceType, final String sourceUuid, final String documentType, final String documentUuid, final boolean owner) {
        if (!owner && !hasDocumentPermission(documentType, documentUuid, DocumentPermissionNames.OWNER)) {
            throw new PermissionException("Only an admin or owner can add document permissions");
        }

        // Get the current user.
        final User userRef = getUser();

        final DocRef docRef = new DocRef(documentType, documentUuid);

        if (owner) {
            // Make the current user the owner of the new document.
            try {
                documentPermissionDao.addPermission(docRef.getUuid(),
                        userRef.getUuid(),
                        DocumentPermissionNames.OWNER);
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        // Inherit permissions from the parent folder if there is one.
        // TODO : This should be part of the explorer service.
        copyPermissions(sourceType, sourceUuid, documentType, documentUuid);

        // Make sure cache updates for the document.
        documentPermissionsCache.remove(docRef.getUuid());
    }

    private void copyPermissions(final String sourceType, final String sourceUuid, final String destType, final String destUuid) {
        if (sourceType != null && sourceUuid != null) {
            final DocRef sourceDocRef = new DocRef(sourceType, sourceUuid);

            final DocumentPermissions documentPermissions = documentPermissionDao.getPermissionsForDocument(sourceDocRef.getUuid());
            if (documentPermissions != null) {
                final Map<String, Set<String>> userPermissions = documentPermissions.getPermissions();
                if (userPermissions != null && userPermissions.size() > 0) {
                    final DocRef destDocRef = new DocRef(destType, destUuid);
                    final String[] allowedPermissions = documentTypePermissions.getPermissions(destDocRef.getType());

                    for (final Map.Entry<String, Set<String>> entry : userPermissions.entrySet()) {
                        final String userUuid = entry.getKey();
                        final Set<String> permissions = entry.getValue();

                        for (final String allowedPermission : allowedPermissions) {
                            if (permissions.contains(allowedPermission)) {
                                try {
                                    documentPermissionDao.addPermission(destDocRef.getUuid(),
                                            userUuid,
                                            allowedPermission);
                                } catch (final RuntimeException e) {
                                    LOGGER.error(e.getMessage(), e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Set<String> getPermissionsForDocumentForUser(final String docRefUuid,
                                                 final String userUuid) {
        final User userRef = getUser();
        if (!userRef.getUuid().equals(userUuid)) {
            throw new PermissionException("You cannot fetch permissions for another user");
        }

        return documentPermissionDao.getPermissionsForDocumentForUser(docRefUuid, userUuid);
    }

    DocumentPermissions getPermissionsForDocument(final String docRefUuid) {
        if (!hasDocumentPermission(null, docRefUuid, DocumentPermissionNames.OWNER)) {
            throw new PermissionException("Only an admin or owner can get document permissions");
        }

        final Map<String, Set<String>> userPermissions = new HashMap<>();

        try {
            final DocumentPermissions documentPermission = documentPermissionDao.getPermissionsForDocument(docRefUuid);
            documentPermission.getPermissions().forEach(userPermissions::put);
        } catch (final RuntimeException e) {
            LOGGER.error("getPermissionsForDocument()", e);
            throw e;
        }

        return new DocumentPermissions(docRefUuid, userPermissions);
    }

    DocumentPermissions getCachedPermissionsForDocument(final String docRefUuid) {
        if (!hasDocumentPermission(null, docRefUuid, DocumentPermissionNames.OWNER)) {
            throw new PermissionException("Only an admin or owner can get document permissions");
        }

        return documentPermissionsCache.get(docRefUuid);
    }

    DocumentPermissions copyPermissionsFromParent(final DocRef docRef) {
        if (!hasDocumentPermission(docRef.getType(), docRef.getUuid(), DocumentPermissionNames.OWNER)) {
            throw new EntityServiceException("You do not have sufficient privileges to change permissions for this document!");
        }

        Optional<ExplorerNode> parent = explorerNodeService.getParent(docRef);
        if (!parent.isPresent()) {
            throw new EntityServiceException("This node does not have a parent to copy permissions from!");
        }

        DocumentPermissions parentsPermissions = getPermissionsForDocument(parent.get().getDocRef().getUuid());
        return parentsPermissions;
    }

    void addPermission(final String docRefUuid,
                       final String userUuid,
                       final String permission) {
        if (!hasDocumentPermission(null, docRefUuid, DocumentPermissionNames.OWNER)) {
            throw new PermissionException("Only an admin or owner can add document permissions");
        }

        documentPermissionDao.addPermission(docRefUuid, userUuid, permission);
    }

    void removePermission(final String docRefUuid,
                          final String userUuid,
                          final String permission) {
        if (!hasDocumentPermission(null, docRefUuid, DocumentPermissionNames.OWNER)) {
            throw new PermissionException("Only an admin or owner can remove document permissions");
        }

        documentPermissionDao.removePermission(docRefUuid, userUuid, permission);
    }

    void clearDocumentPermissionsForUser(final String docRefUuid,
                                         final String userUuid) {
        if (!hasDocumentPermission(null, docRefUuid, DocumentPermissionNames.OWNER)) {
            throw new PermissionException("Only an admin or owner can remove document permissions");
        }

        documentPermissionDao.clearDocumentPermissionsForUser(docRefUuid, userUuid);

    }

    void clearDocumentPermissions(final String docRefUuid) {
        if (!hasDocumentPermission(null, docRefUuid, DocumentPermissionNames.OWNER)) {
            throw new PermissionException("Only an admin or owner can remove document permissions");
        }

        documentPermissionDao.clearDocumentPermissions(docRefUuid);
    }


    void changeDocumentPermissions(final DocRef docRef,
                                   final ChangeSet<UserPermission> changeSet,
                                   final Cascade cascade) {
        // Check that the current user has permission to change the permissions of the document.
        if (hasDocumentPermission(docRef.getType(), docRef.getUuid(), DocumentPermissionNames.OWNER)) {
            // Record what documents and what users are affected by these changes so we can clear the relevant caches.
            final Set<String> affectedDocRefUuids = new HashSet<>();
            final Set<String> affectedUserUuids = new HashSet<>();

            // Change the permissions of the document.
            changeDocPermissions(docRef, changeSet, affectedDocRefUuids, affectedUserUuids, false);

            // Cascade changes if this is a folder and we have been asked to do so.
            if (cascade != null) {
                cascadeChanges(docRef, changeSet, affectedDocRefUuids, affectedUserUuids, cascade);
            }

            // Force refresh of cached permissions.
            affectedDocRefUuids.forEach(documentPermissionsCache::remove);
        }

        throw new EntityServiceException("You do not have sufficient privileges to change permissions for this document");
    }

    private void changeDocPermissions(final DocRef docRef,
                                      final ChangeSet<UserPermission> changeSet,
                                      final Set<String> affectedDocRefUuids,
                                      final Set<String> affectedUserUuids,
                                      final boolean clear) {
        if (clear) {
            // If we are asked to clear all permissions then get them for this document and then remove them.
            final DocumentPermissions documentPermissions = getPermissionsForDocument(docRef.getUuid());
            for (final Map.Entry<String, Set<String>> entry : documentPermissions.getPermissions().entrySet()) {
                final String userUUid = entry.getKey();
                for (final String permission : entry.getValue()) {
                    try {
                        removePermission(docRef.getUuid(), userUUid, permission);
                        // Remember the affected documents and users so we can clear the relevant caches.
                        affectedDocRefUuids.add(docRef.getUuid());
                        affectedUserUuids.add(userUUid);
                    } catch (final RuntimeException e) {
                        // Expected.
                        LOGGER.debug(e.getMessage());
                    }
                }
            }

        } else {
            // Otherwise remove permissions specified by the change set.
            for (final UserPermission userPermission : changeSet.getRemoveSet()) {
                final String userUuid = userPermission.getUserUuid();
                try {
                    removePermission(docRef.getUuid(), userUuid, userPermission.getPermission());
                    // Remember the affected documents and users so we can clear the relevant caches.
                    affectedDocRefUuids.add(docRef.getUuid());
                    affectedUserUuids.add(userUuid);
                } catch (final RuntimeException e) {
                    // Expected.
                    LOGGER.debug(e.getMessage());
                }
            }
        }

        // Add permissions from the change set.
        for (final UserPermission userPermission : changeSet.getAddSet()) {
            // Don't add create permissions to items that aren't folders as it makes no sense.
            if (DocumentTypes.isFolder(docRef.getType()) || !userPermission.getPermission().startsWith(DocumentPermissionNames.CREATE)) {
                final String userUuid = userPermission.getUserUuid();
                try {
                    addPermission(docRef.getUuid(), userUuid, userPermission.getPermission());
                    // Remember the affected documents and users so we can clear the relevant caches.
                    affectedDocRefUuids.add(docRef.getUuid());
                    affectedUserUuids.add(userUuid);
                } catch (final RuntimeException e) {
                    // Expected.
                    LOGGER.debug(e.getMessage());
                }
            }
        }
    }

//    private void cascadeChanges(final DocRef docRef, final ChangeSet<UserPermission> changeSet, final Set<DocRef> affectedDocRefs, final Set<User> affectedUsers, final ChangeDocumentPermissionsAction.Cascade cascade) {
//        final BaseEntity entity = genericEntityService.loadByUuid(docRef.getType(), docRef.getUuid());
//        if (entity != null) {
//            if (entity instanceof Folder) {
//                final Folder folder = (Folder) entity;
//
//                switch (cascade) {
//                    case CHANGES_ONLY:
//                        // We are only cascading changes so just pass on the change set.
//                        changeChildPermissions(DocRefUtil.create(folder), changeSet, affectedDocRefs, affectedUsers, false);
//                        break;
//
//                    case ALL:
//                        // We are replicating the permissions of the parent folder on all children so create a change set from the parent folder.
//                        final DocumentPermissions parentPermissions = documentPermissionService.getPermissionsForDocument(DocRefUtil.create(folder));
//                        final ChangeSet<UserPermission> fullChangeSet = new ChangeSet<>();
//                        for (final Map.Entry<User, Set<String>> entry : parentPermissions.getUserPermissions().entrySet()) {
//                            final User userRef = entry.getKey();
//                            for (final String permission : entry.getValue()) {
//                                fullChangeSet.add(new UserPermission(userRef, permission));
//                            }
//                        }
//
//                        // Set child permissions to that of the parent folder after clearing all permissions from child documents.
//                        changeChildPermissions(DocRefUtil.create(folder), fullChangeSet, affectedDocRefs, affectedUsers, true);
//
//                    break;
//
//                case NO:
//                    // Do nothing.
//                    break;
//            }
//        }
//    }
//
//    private void changeChildPermissions(final DocRef folder, final ChangeSet<UserPermission> changeSet, final Set<DocRef> affectedDocRefs, final Set<User> affectedUsers, final boolean clear) {
//        final List<String> types = getTypeList();
//        for (final String type : types) {
//            final List<DocumentEntity> children = genericEntityService.findByFolder(type, folder, null);
//            if (children != null && children.size() > 0) {
//                for (final DocumentEntity child : children) {
//                    final DocRef childDocRef = DocRefUtil.create(child);
//                    changeDocPermissions(childDocRef, changeSet, affectedDocRefs, affectedUsers, clear);
//
//                    if (child instanceof Folder) {
//                        changeChildPermissions(childDocRef, changeSet, affectedDocRefs, affectedUsers, clear);
//                    }
//                }
//            }
//        }
//    }

    private void cascadeChanges(final DocRef docRef,
                                final ChangeSet<UserPermission> changeSet,
                                final Set<String> affectedDocRefUuids,
                                final Set<String> affectedUserUuids,
                                final ChangeDocumentPermissionsAction.Cascade cascade) {
        if (DocumentTypes.isFolder(docRef.getType())) {
            switch (cascade) {
                case CHANGES_ONLY:
                    // We are only cascading changes so just pass on the change set.
                    changeDescendantPermissions(docRef, changeSet, affectedDocRefUuids, affectedUserUuids, false);
                    break;

                case ALL:
                    // We are replicating the permissions of the parent folder on all children so create a change set from the parent folder.
                    final DocumentPermissions parentPermissions = getPermissionsForDocument(docRef.getUuid());
                    final ChangeSet<UserPermission> fullChangeSet = new ChangeSet<>();
                    for (final Map.Entry<String, Set<String>> entry : parentPermissions.getPermissions().entrySet()) {
                        final String userUuid = entry.getKey();
                        for (final String permission : entry.getValue()) {
                            fullChangeSet.add(new UserPermission(userUuid, permission));
                        }
                    }

                    // Set child permissions to that of the parent folder after clearing all permissions from child documents.
                    changeDescendantPermissions(docRef, fullChangeSet, affectedDocRefUuids, affectedUserUuids, true);

                    break;

                case NO:
                    // Do nothing.
                    break;
            }
        }
    }

    private void changeDescendantPermissions(final DocRef folder,
                                             final ChangeSet<UserPermission> changeSet,
                                             final Set<String> affectedDocRefUuids,
                                             final Set<String> affectedUserUuids,
                                             final boolean clear) {
        final List<ExplorerNode> descendants = explorerNodeService.getDescendants(folder);
        if (descendants != null && descendants.size() > 0) {
            for (final ExplorerNode descendant : descendants) {
                // Ensure that the user has permission to change the permissions of this child.
                if (hasDocumentPermission(descendant.getType(), descendant.getUuid(), DocumentPermissionNames.OWNER)) {
                    changeDocPermissions(descendant.getDocRef(), changeSet, affectedDocRefUuids, affectedUserUuids, clear);
                } else {
                    LOGGER.debug("User does not have permission to change permissions on " + descendant.toString());
                }
            }
        }
    }


    private User getUser() {
        final User userRef = CurrentUserState.currentUser();
        if (userRef == null) {
            throw new PermissionException("No user is currently logged in");
        }
        return userRef;
    }
}
