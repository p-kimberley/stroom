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
import stroom.security.api.SecurityContext;
import stroom.security.api.UserTokenUtil;
import stroom.security.impl.exception.AuthenticationException;
import stroom.security.shared.User;
import stroom.security.shared.UserToken;

import javax.inject.Inject;
import java.util.Optional;

class SecurityContextImpl implements SecurityContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityContextImpl.class);
    private static final String USER = "user";
    private static final UserToken INTERNAL_PROCESSING_USER_TOKEN = UserTokenUtil.processingUser();
    private static final User INTERNAL_PROCESSING_USER = new User.Builder()
            .id(0)
            .uuid("0")
            .name(INTERNAL_PROCESSING_USER_TOKEN.getUserId())
            .group(false)
            .build();

    private final AppPermissionService appPermissionService;
    private final DocumentPermissionService documentPermissionService;
    private final UserCache userCache;
    private final ApiTokenCache apiTokenCache;

    @Inject
    SecurityContextImpl(
            final AppPermissionService appPermissionService,
            final DocumentPermissionService documentPermissionService,
            final UserCache userCache,
            final ApiTokenCache apiTokenCache) {
        this.appPermissionService = appPermissionService;
        this.documentPermissionService = documentPermissionService;
        this.userCache = userCache;
        this.apiTokenCache = apiTokenCache;
    }

    @Override
    public void pushUser(final UserToken token) {
        User userRef = null;

        if (token != null) {
            final String type = token.getType();
            final String name = token.getUserId();

            if (INTERNAL_PROCESSING_USER_TOKEN.getType().equals(type)) {
                if (INTERNAL_PROCESSING_USER_TOKEN.getUserId().equals(name)) {
                    userRef = INTERNAL_PROCESSING_USER;
                } else {
                    LOGGER.error("Unexpected system user '" + name + "'");
                    throw new AuthenticationException("Unexpected system user '" + name + "'");
                }
            } else if (USER.equals(type)) {
                if (name.length() > 0) {
                    final Optional<User> optional = userCache.get(name);
                    if (!optional.isPresent()) {
                        final String message = "Unable to push user '" + name + "' as user is unknown";
                        LOGGER.error(message);
                        throw new AuthenticationException(message);
                    } else {
                        userRef = optional.get();
                    }
                }
            } else {
                LOGGER.error("Unexpected token type '" + type + "'");
                throw new AuthenticationException("Unexpected token type '" + type + "'");
            }
        }

        CurrentUserState.push(token, userRef);
    }

    @Override
    public void popUser() {
        CurrentUserState.pop();
    }

    private User getUser() {
        return CurrentUserState.currentUser();
    }

    @Override
    public UserToken getUserToken() {
        return CurrentUserState.currentUserToken();
    }

    @Override
    public String getUserId() {
        final User userRef = getUser();
        if (userRef == null) {
            return null;
        }
        return userRef.getName();
    }

    @Override
    public String getApiToken() {
        return apiTokenCache.get(getUserId());
    }

    @Override
    public boolean isLoggedIn() {
        return getUser() != null;
    }

    @Override
    public boolean isAdmin() {
        return appPermissionService.isAdmin();
    }

    @Override
    public void elevatePermissions() {
        CurrentUserState.elevatePermissions();
    }

    @Override
    public void restorePermissions() {
        CurrentUserState.restorePermissions();
    }

    @Override
    public boolean hasAppPermission(final String permission) {
        return appPermissionService.hasAppPermission(permission);
    }

    @Override
    public boolean hasDocumentPermission(final String documentType, final String documentId, final String permission) {
        return documentPermissionService.hasDocumentPermission(documentType, documentId, permission);
    }

    @Override
    public void clearDocumentPermissions(final String documentType, final String documentUuid) {
        documentPermissionService.clearDocumentPermissions(documentType, documentUuid);
    }

    @Override
    public void addDocumentPermissions(final String sourceType, final String sourceUuid, final String documentType, final String documentUuid, final boolean owner) {
        documentPermissionService.addDocumentPermissions(sourceType, sourceUuid, documentType, documentUuid, owner);
    }
}