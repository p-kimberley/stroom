package stroom.security.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.security.api.UserTokenUtil;
import stroom.security.impl.exception.AuthenticationException;
import stroom.security.shared.PermissionException;
import stroom.security.shared.PermissionNames;
import stroom.security.shared.User;
import stroom.security.shared.UserAndPermissions;
import stroom.security.shared.UserAppPermissions;
import stroom.security.shared.UserToken;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class AppPermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppPermissionService.class);

    private static final UserToken INTERNAL_PROCESSING_USER_TOKEN = UserTokenUtil.processingUser();
    private static final User INTERNAL_PROCESSING_USER = new User.Builder()
            .id(0)
            .uuid("0")
            .name(INTERNAL_PROCESSING_USER_TOKEN.getUserId())
            .group(false)
            .build();

    private final UserGroupsCache userGroupsCache;
    private final AppPermissionsCache userAppPermissionsCache;
    private final UserCache userCache;
    private final AppPermissionDao appPermissionDao;
    private final AuthenticationConfig authenticationConfig;

    @Inject
    AppPermissionService(final UserGroupsCache userGroupsCache,
                         final AppPermissionsCache userAppPermissionsCache,
                         final UserCache userCache,
                         final AppPermissionDao appPermissionDao,
                         final AuthenticationConfig authenticationConfig) {
        this.userGroupsCache = userGroupsCache;
        this.userAppPermissionsCache = userAppPermissionsCache;
        this.userCache = userCache;
        this.appPermissionDao = appPermissionDao;
        this.authenticationConfig = authenticationConfig;
    }

    boolean isAdmin() {
        return hasAppPermission(PermissionNames.ADMINISTRATOR);
    }

    boolean hasAppPermission(final String permission) {
        // Get the current user.
        final User userRef = getUser();

        // If there is no logged in user then throw an exception.
        if (userRef == null) {
            throw new AuthenticationException("No user is currently logged in");
        }

        // If the user is the internal processing user then they automatically have permission.
        if (INTERNAL_PROCESSING_USER.equals(userRef)) {
            return true;
        }

        // See if the user has permission.
        boolean result = hasAppPermission(userRef, permission);

        // If the user doesn't have the requested permission see if they are an admin.
        if (!result && !PermissionNames.ADMINISTRATOR.equals(permission)) {
            result = hasAppPermission(userRef, PermissionNames.ADMINISTRATOR);
        }

        return result;
    }

    private boolean hasAppPermission(final User userRef, final String permission) {
        // See if the user has an explicit permission.
        boolean result = hasUserAppPermission(userRef, permission);

        // See if the user belongs to a group that has permission.
        if (!result) {
            final List<User> userGroups = userGroupsCache.get(userRef.getUuid());
            result = hasUserGroupsAppPermission(userGroups, permission);
        }

        return result;
    }

    private boolean hasUserGroupsAppPermission(final List<User> userGroups, final String permission) {
        if (userGroups != null) {
            for (final User userGroup : userGroups) {
                final boolean result = hasUserAppPermission(userGroup, permission);
                if (result) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasUserAppPermission(final User userRef, final String permission) {
        final UserAppPermissions userAppPermissions = userAppPermissionsCache.get(userRef);
        if (userAppPermissions != null) {
            return userAppPermissions.getUserPermissons().contains(permission);
        }
        return false;
    }

    Set<String> getPermissionNamesForUser(final String userUuid) {
        if (!isAdmin() && !getUser().getUuid().equals(userUuid)) {
            throw new PermissionException("Only administrators or the specified user can get permission names");
        }

        return appPermissionDao.getPermissionsForUser(userUuid);
    }

    Set<String> getAllPermissionNames() {
        return PermissionNames.ALL_PERMISSIONS;
    }

    Set<String> getPermissionNamesForUserName(String userName) {
        final Optional<User> optional = userCache.get(userName);
        if (!optional.isPresent()) {
            throw new PermissionException("No user found with name '" + userName + "'");
        }

        return getPermissionNamesForUser(optional.get().getUuid());
    }

    void addPermission(final String userUuid, final String permission) {
        if (!isAdmin()) {
            throw new PermissionException("Only administrators can add permissions");
        }

        try {
            appPermissionDao.addPermission(userUuid, permission);
        } catch (final RuntimeException e) {
            LOGGER.error("addPermission()", e);
            throw e;
        }
    }

    void removePermission(final String userUuid, final String permission) {
        if (!isAdmin()) {
            throw new PermissionException("Only administrators can remove permissions");
        }

        try {
            appPermissionDao.removePermission(userUuid, permission);
        } catch (final RuntimeException e) {
            LOGGER.error("removePermission()", e);
            throw e;
        }
    }

    UserAndPermissions fetchUserAndPermissions() {
        final User userRef = CurrentUserState.currentUser();
        if (userRef == null) {
            return null;
        }

        final boolean preventLogin = authenticationConfig.isPreventLogin();
        if (preventLogin) {
            if (!isAdmin()) {
                throw new AuthenticationException("Stroom is down for maintenance. Please try again later.");
            }
        }

        return new UserAndPermissions(userRef, getUserPermissions(userRef));
    }

    private Set<String> getUserPermissions(final User userRef) {
        final Set<String> appPermissionSet = new HashSet<>();

        // Add app permissions set explicitly for this user first.
        addPermissions(appPermissionSet, userRef);

        // Get user groups for this user.
        final List<User> userGroups = userGroupsCache.get(userRef.getUuid());

        // Add app permissions set on groups this user belongs to.
        if (userGroups != null) {
            for (final User userGroup : userGroups) {
                addPermissions(appPermissionSet, userGroup);
            }
        }

        return appPermissionSet;
    }

    private void addPermissions(final Set<String> appPermissionSet, final User userRef) {
        final UserAppPermissions userAppPermissions = userAppPermissionsCache.get(userRef);
        if (userAppPermissions != null && userAppPermissions.getUserPermissons() != null) {
            appPermissionSet.addAll(userAppPermissions.getUserPermissons());
        }
    }

    private User getUser() {
        return CurrentUserState.currentUser();
    }
}
