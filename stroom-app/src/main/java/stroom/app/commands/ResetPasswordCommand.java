package stroom.app.commands;

import stroom.config.app.Config;
import stroom.event.logging.api.StroomEventLoggingService;
import stroom.security.api.SecurityContext;
import stroom.security.identity.account.AccountDao;
import stroom.util.logging.LogUtil;

import com.google.inject.Injector;
import event.logging.AuthenticateAction;
import event.logging.AuthenticateEventAction;
import event.logging.AuthenticateOutcome;
import event.logging.User;
import event.logging.util.EventLoggingUtil;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import javax.inject.Inject;

/**
 * Resets the password of an account in the internal identity provider
 */
public class ResetPasswordCommand extends AbstractStroomAccountConfiguredCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetPasswordCommand.class);

    private static final String COMMAND_NAME = "reset_password";
    private static final String COMMAND_DESCRIPTION = "Reset the password of the user account " +
            "in the internal identity provider";

    private static final String USERNAME_ARG_NAME = "user";
    private static final String PASSWORD_ARG_NAME = "password";

    private final Path configFile;

    @Inject
    private AccountDao accountDao;
    @Inject
    private SecurityContext securityContext;
    @Inject
    private StroomEventLoggingService stroomEventLoggingService;

    public ResetPasswordCommand(final Path configFile) {
        super(configFile, COMMAND_NAME, COMMAND_DESCRIPTION);
        this.configFile = configFile;
    }

    @Override
    public void configure(final Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("-u", "--" + USERNAME_ARG_NAME)
                .dest(USERNAME_ARG_NAME)
                .type(String.class)
                .required(true)
                .help("The user id of the account, e.g. 'admin'");

        subparser.addArgument("-p", "--" + PASSWORD_ARG_NAME)
                .dest(PASSWORD_ARG_NAME)
                .type(String.class)
                .required(true)
                .help("The new password for the account");
    }

    @Override
    protected void runCommand(final Bootstrap<Config> bootstrap,
                              final Namespace namespace,
                              final Config config,
                              final Injector injector) {

        final String username = namespace.getString(USERNAME_ARG_NAME);
        final String newPassword = namespace.getString(PASSWORD_ARG_NAME);

        LOGGER.debug("Resetting password for account {}", username);

        injector.injectMembers(this);


        securityContext.asProcessingUser(() -> {
            try {
                accountDao.resetPassword(username, newPassword);

                String msg = LogUtil.message("Password reset complete for user {}", username);
                LOGGER.info(msg);
                logEvent(username, true, msg);
                System.exit(0);

            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage());
                logEvent(username, false, e.getMessage());
                System.exit(1);
            }
        });
    }

    private void logEvent(final String username,
                          final boolean wasSuccessful,
                          final String description) {

        stroomEventLoggingService.log(
                "CliChangePassword",
                LogUtil.message("The password for user {} was changed from the command line", username),
                AuthenticateEventAction.builder()
                        .withAction(AuthenticateAction.CHANGE_PASSWORD)
                        .withUser(User.builder()
                                .withName(username)
                                .build())
                        .withOutcome(EventLoggingUtil.createOutcome(
                                AuthenticateOutcome.class,
                                wasSuccessful,
                                description))
                        .build());
    }
}
