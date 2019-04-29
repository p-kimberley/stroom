package stroom.security.api;

import stroom.security.shared.UserToken;

import java.util.function.Supplier;

public interface Security {
    /**
     * Convenience method to get the current user id.
     *
     * @return The id of the user associated with this security context.
     */
    String getUserId();

    <T> T asUserResult(UserToken userToken, Supplier<T> supplier);

    void asUser(UserToken userToken, Runnable runnable);

    <T> T asProcessingUserResult(Supplier<T> supplier);

    void asProcessingUser(Runnable runnable);

    <T> T useAsReadResult(Supplier<T> supplier);

    void useAsRead(Runnable runnable);

    void secure(String permission, Runnable runnable);

    <T> T secureResult(String permission, Supplier<T> supplier);

    void secure(Runnable runnable);

    <T> T secureResult(Supplier<T> supplier);

    void insecure(Runnable runnable);

    <T> T insecureResult(Supplier<T> supplier);
}
