package stroom.config.global.impl.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.config.global.impl.ConfigMapper;
import stroom.util.logging.LogUtil;
import stroom.util.shared.IsConfig;
import stroom.util.shared.ValidationSeverity;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ConfigValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigValidator.class);

    private final ConfigMapper configMapper;
    private final Validator validator;

    @Inject
    ConfigValidator(final ConfigMapper configMapper, final Validator validator) {
        this.configMapper = configMapper;
        this.validator = validator;
    }

    /**
     * Default validation that logs errors/warnings to the logger.
     */
    public Result validate(final IsConfig config) {
        return validate(config, this::logConstraintViolation);
    }

    public Result validate(final IsConfig config,
                           final BiConsumer<ConstraintViolation<IsConfig>, ValidationSeverity> constraintViolationConsumer) {

        final Set<ConstraintViolation<IsConfig>> constraintViolations = validator.validate(config);

        int errorCount = 0;
        int warningCount = 0;

        for (final ConstraintViolation<IsConfig> constraintViolation : constraintViolations) {
            LOGGER.debug("constraintViolation - prop: {}, value: [{}], object: {}",
                constraintViolation.getPropertyPath().toString(),
                constraintViolation.getInvalidValue(),
                constraintViolation.getLeafBean().getClass().getCanonicalName());

            ValidationSeverity severity = ValidationSeverity.fromPayloads(
                constraintViolation.getConstraintDescriptor().getPayload());

            if (severity.equals(ValidationSeverity.WARNING)) {
                warningCount++;
            } else if (severity.equals(ValidationSeverity.ERROR)) {
                errorCount++;
            }

            constraintViolationConsumer.accept(constraintViolation, severity);
        }
        return new Result(errorCount, warningCount);
    }

    private void logConstraintViolation(final ConstraintViolation<IsConfig> constraintViolation,
                                        final ValidationSeverity severity) {

        final Consumer<String> logFunc;
        final String severityStr;
        if (severity.equals(ValidationSeverity.WARNING)) {
            logFunc = LOGGER::warn;
            severityStr = "warning";
        } else {
            logFunc = LOGGER::error;
            severityStr = "error";
        }

        String propName = null;
        for (javax.validation.Path.Node node : constraintViolation.getPropertyPath()) {
            propName = node.getName();
        }
        // Use config mapper to get the path of the config object
        final String path = configMapper.getFullPath((IsConfig) constraintViolation.getLeafBean(), propName);

        logFunc.accept(LogUtil.message("  Validation {} for {} [{}] - {}",
            severityStr,
            path,
            constraintViolation.getInvalidValue(),
            constraintViolation.getMessage()));
    }

    public static class Result {
        private final int errorCount;
        private final int warningCount;

        public Result(final int errorCount, final int warningCount) {
            this.errorCount = errorCount;
            this.warningCount = warningCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public int getWarningCount() {
            return warningCount;
        }
    }
}
