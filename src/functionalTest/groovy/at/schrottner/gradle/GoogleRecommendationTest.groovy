package at.schrottner.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GoogleRecommendationTest extends AbstractFunctionalTests {

    @ParameterizedTest
    @ValueSource(strings = ["groovy"])
    void "test"(String primer) {
        //given:

        //when:
        BuildResult result = runTest("allDeps","-i","--refresh-dependencies")

    }
}
