package stroom.security.identity.authenticate.api;

import java.net.URI;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

public interface AuthenticationService {

    String UNAUTHORISED_URL_PATH = "/s/unauthorised";
    String SIGN_IN_URL_PATH = "/s/signIn";
    String CONFIRM_PASSWORD_URL_PATH = "/s/confirmpassword";
    String CHANGE_PASSWORD_URL_PATH = "/s/changepassword";
    String API_KEYS_URL_PATH = "/s/apiKeys";
    String USERS_URL_PATH = "/s/users";

    Optional<AuthState> currentAuthState(HttpServletRequest request);

    URI createSignInUri(String redirectUri);

    URI createConfirmPasswordUri(String redirectUri);

    URI createChangePasswordUri(String redirectUri);

    interface AuthState {

        String getSubject();

        boolean isRequirePasswordChange();

        long getLastCredentialCheckMs();
    }
}