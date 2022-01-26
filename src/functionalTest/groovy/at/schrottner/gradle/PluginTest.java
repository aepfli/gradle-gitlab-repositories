package at.schrottner.gradle;

import org.apiguardian.api.API;
import org.gradle.plugin.devel.tasks.internal.ValidateAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.platform.commons.annotation.Testable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.apiguardian.api.API.Status.STABLE;

@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = STABLE, since = "5.0")
@ParameterizedTest(name = "{index} Running test for *.{0}")
@ArgumentsSource(PluginTestParams.class)
public @interface PluginTest {

}
