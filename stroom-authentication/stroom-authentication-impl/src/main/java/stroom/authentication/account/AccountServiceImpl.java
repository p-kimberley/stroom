package stroom.authentication.account;

import stroom.authentication.authenticate.PasswordValidator;
import stroom.authentication.config.AuthenticationConfig;
import stroom.security.api.SecurityContext;
import stroom.security.shared.PermissionNames;
import stroom.util.shared.PermissionException;
import stroom.util.shared.ResultPage;

import com.google.common.base.Strings;

import javax.inject.Inject;
import java.util.Optional;

public class AccountServiceImpl implements AccountService {
    private final AccountDao accountDao;
    private final SecurityContext securityContext;
    private final AuthenticationConfig config;

    @Inject
    AccountServiceImpl(final AccountDao accountDao,
                       final SecurityContext securityContext,
                       final AuthenticationConfig config) {
        this.accountDao = accountDao;
        this.securityContext = securityContext;
        this.config = config;
    }

    public ResultPage<Account> list() {
        checkPermission();

        return accountDao.list();
    }

    public ResultPage<Account> search(final SearchAccountRequest request) {
        checkPermission();

        return accountDao.searchUsersForDisplay(request);
    }

    public Account create(final CreateAccountRequest request) {
        checkPermission();
        validateCreateRequest(request);

        // Validate
        final String userId = securityContext.getUserId();

        final long now = System.currentTimeMillis();

        final Account account = new Account();
        account.setCreateTimeMs(now);
        account.setCreateUser(userId);
        account.setUpdateTimeMs(now);
        account.setUpdateUser(userId);
        account.setFirstName(request.getFirstName());
        account.setLastName(request.getLastName());
        account.setUserId(request.getUserId());
        account.setEmail(request.getEmail());
        account.setComments(request.getComments());
        account.setForcePasswordChange(request.isForcePasswordChange());
        account.setNeverExpires(request.isNeverExpires());
        account.setLoginCount(0);
        // Set enabled by default.
        account.setEnabled(true);

        return accountDao.create(account, request.getPassword());
    }

    public Optional<Account> read(final int accountId) {
        final String loggedInUser = securityContext.getUserId();

        Optional<Account> optionalUser = accountDao.get(accountId);
        if (optionalUser.isPresent()) {
            Account foundAccount = optionalUser.get();
            // We only need to check auth permissions if the user is trying to access a different user.
            final boolean isUserAccessingThemselves = loggedInUser.equals(foundAccount.getUserId());
            boolean canManageUsers = securityContext.hasAppPermission(PermissionNames.MANAGE_USERS_PERMISSION);
            if (!isUserAccessingThemselves && !canManageUsers) {
                throw new RuntimeException("Unauthorized");
            }
        }
        return optionalUser;
    }

    public Optional<Account> read(final String email) {
        checkPermission();
        return accountDao.get(email);
    }

    public void update(final Account account, final int accountId) {
        checkPermission();

//        // Update Stroom user
//        Optional<Account> optionalUser = accountDao.get(userId);
//        Account foundAccount = optionalUser.get();
//        boolean isEnabled = account.isEnabled();
//        stroom.security.shared.User userToUpdate = securityUserService.getUserByName(foundAccount.getEmail());
//        userToUpdate.setEnabled(isEnabled);
//        securityUserService.update(userToUpdate);

        final String loggedInUser = securityContext.getUserId();
        account.setUpdateUser(loggedInUser);
        account.setUpdateTimeMs(System.currentTimeMillis());
        account.setId(accountId);
        accountDao.update(account);
    }

    public void delete(final int accountId) {
        checkPermission();
        accountDao.delete(accountId);
    }

    public void validateCreateRequest(final CreateAccountRequest request) {
        if (request == null) {
            throw new RuntimeException("Null request");
        } else {
            if (Strings.isNullOrEmpty(request.getUserId())) {
                throw new RuntimeException("No user id has been provided");
            }

            if (request.getPassword() != null || request.getConfirmPassword() != null) {
                PasswordValidator.validateLength(request.getPassword(), config.getPasswordPolicyConfig().getMinimumPasswordLength());
                PasswordValidator.validateComplexity(request.getPassword(), config.getPasswordPolicyConfig().getPasswordComplexityRegex());
                PasswordValidator.validateConfirmation(request.getPassword(), request.getConfirmPassword());
            }
        }
    }

    private void checkPermission() {
        if (!securityContext.hasAppPermission(PermissionNames.MANAGE_USERS_PERMISSION)) {
            throw new PermissionException(securityContext.getUserId(), "You do not have permission to manage users");
        }
    }
}
