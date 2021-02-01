package stroom.dropwizard.common;

import stroom.util.shared.AutoLogged;
import stroom.util.shared.RestResource;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TestRestResources {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestRestResources.class);

    @TestFactory
    Stream<DynamicTest> buildQualityAssuranceTests() {
        try (ScanResult result = new ClassGraph()
                .whitelistPackages("stroom")
                .enableClassInfo()
                .ignoreClassVisibility()
                .enableAnnotationInfo()
                .scan()) {

            final List<? extends Class<? extends RestResource>> classes = result.getAllClasses()
                    .stream()
                    .filter(classInfo -> classInfo.implementsInterface(RestResource.class.getName()))
                    .map(classInfo -> (Class<? extends RestResource>) classInfo.loadClass())
                    .sorted(Comparator.comparing(Class::getName))
                    .collect(Collectors.toList());

            return classes.stream()
                    .map(resourceClass ->
                            DynamicTest.dynamicTest(resourceClass.getName(),
                                    () ->
                                            doResourceClassAsserts(resourceClass)));
        }
    }

    private void doResourceClassAsserts(final Class<? extends RestResource> resourceClass) {

        final boolean isInterface = resourceClass.isInterface();
        final String typeName = isInterface
                        ? "interface"
                        : "class";
        LOGGER.info("Inspecting {} {}", typeName, resourceClass.getName());

        final boolean superImplementsRestResource = Arrays.stream(resourceClass.getInterfaces())
                .filter(iface ->
                        !RestResource.class.equals(iface))
                .anyMatch(iface ->
                        Arrays.asList(iface.getInterfaces())
                                .contains(RestResource.class));

        SoftAssertions.assertSoftly(softAssertions -> {

            if (isInterface || !superImplementsRestResource) {
                // This is an iface or a class that implements RestResource with no iface of its own

                final boolean hasApiAnnotation = resourceClass.isAnnotationPresent(Api.class);
                final String[] apiAnnotationTags = hasApiAnnotation
                        ? resourceClass.getAnnotation(Api.class).tags()
                        : new String[0];

                softAssertions.assertThat(hasApiAnnotation)
                        .withFailMessage(() -> typeName + " must have class annotation like " +
                                "@Api(tags = \"Nodes\")")
                        .isTrue();

                if (hasApiAnnotation) {
                    softAssertions.assertThat(apiAnnotationTags.length)
                            .withFailMessage(() -> "@Api must have tags property set, e.g. @Api(tags = \"Nodes\")")
                            .isGreaterThanOrEqualTo(1);
                    if (apiAnnotationTags.length >= 1) {
                        softAssertions.assertThat(apiAnnotationTags[0])
                                .withFailMessage(() -> "@Api must have tags property set, e.g. @Api(tags = \"Nodes\")")
                                .isNotEmpty();
                    }
                }

                Arrays.stream(resourceClass.getMethods())
                        .filter(method -> !Modifier.isPrivate(method.getModifiers()))
                        .forEach(method -> {

                            softAssertions.assertThat(List.of(method.getAnnotations())
                                    .contains(ApiOperation.class))
                                    .withFailMessage(() -> "Method " + method.getName() + "(...) must be annotated " +
                                            "with @ApiOperation(value = \"Some description of what the method does\")")
                                    .isTrue();
                        });
            } else {
                LOGGER.info("Assertions handled by interface");
                // This is a class that implements an iface that extends RestResource so
                // that will be dealt with when it looks at that iface directly.
            }

            if (!isInterface) {
                // AutoLogged is only used on classes, not interfaces
                final boolean classIsAutoLogged = resourceClass.isAnnotationPresent(AutoLogged.class);

                Arrays.stream(resourceClass.getMethods())
                        .filter(method -> !Modifier.isPrivate(method.getModifiers()))
                        .forEach(method -> {
                            final boolean methodIsAutoLogged = method.isAnnotationPresent(AutoLogged.class);

                            softAssertions.assertThat(classIsAutoLogged || methodIsAutoLogged)
                                    .withFailMessage(() -> "Method " + method.getName() + "(...) or its class must be " +
                                            "annotated with @AutoLogged")
                                    .isTrue();
                        });
            }
        });
    }
}