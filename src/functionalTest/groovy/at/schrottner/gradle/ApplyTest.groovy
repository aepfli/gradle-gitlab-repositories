/*
 * Copyright 2016-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */
package at.schrottner.gradle

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.assertj.core.api.Assertions.assertThat

// TODO: check if we can parameterize this somehow

class ApplyTest extends AbstractFunctionalTests {

	@PluginTest
	void "only used in settings"(String primer) {
		//given:

		//when:
		BuildResult result = runTest()

		//then:
		realms.each {
			def capitalized = it.capitalize()
			def repoPrefix = "GitLab-${capitalized}"
			assertThat(result.output)
					.contains("BUILD SUCCESSFUL")
					.containsSubsequence(
					"added Job-Token: jobToken",
					"added Private-Token: tokenIgnoredNoValue",
					"added Deploy-Token: token0",
					"added Deploy-Token: token1"
					)
					.containsSubsequence("Maven Repository $repoPrefix-$existingId is using 'token0'",
					"Maven Repository $it-renamed is using 'token0'",
					"Maven Repository $repoPrefix-specialToken is using 'token0'",
					"Maven Repository $repoPrefix-specialToken1 is using 'token1'",
					"Maven Repository $repoPrefix-specialTokenSelection is using 'token1'",
					"Maven Repository $repoPrefix-ignoredNoValue was not added, as no token could be applied!"
					)
					.doesNotContain("Maven Repository $repoPrefix-ignoredNoValue is using '")
		}
	}

	@PluginTest
	void "only used in project"(String primer) {
		//given:

		//when:
		BuildResult result = runTest()

		//then:
		realms.each {
			def capitalized = it.capitalize()
			def repoPrefix = "GitLab-${capitalized}"
			assertThat(result.output)
					.contains("BUILD SUCCESSFUL")
					.containsSubsequence(
					"added Job-Token: jobToken",
					"added Private-Token: tokenIgnoredNoValue",
					"added Private-Token: token0",
					"added Private-Token: token1"
					)
					.containsSubsequence("Maven Repository $repoPrefix-$existingId is using 'token0'",
					"Maven Repository $it-renamed is using 'token0'",
					"Maven Repository $repoPrefix-specialToken is using 'token0'",
					"Maven Repository $repoPrefix-specialToken1 is using 'token1'",
					"Maven Repository $repoPrefix-specialTokenSelection is using 'token1'",
					"Maven Repository $repoPrefix-ignoredNoValue was not added, as no token could be applied!"
					)
					.doesNotContain("Maven Repository $repoPrefix-ignoredNoValue is using '")
		}
	}

	@PluginTest
	void "used in settings and project"(String primer) {
		//given:

		//when:
		BuildResult result = runTest()

		//then:
		realms.each {
			def capitalized = it.capitalize()
			def repoPrefix = "GitLab-${capitalized}"
			assertThat(result.output)
					.contains("BUILD SUCCESSFUL")
					.containsSubsequence(
					"added Job-Token: jobToken",
					"added Private-Token: tokenIgnoredNoValue",
					"added Deploy-Token: token0",
					"added Deploy-Token: token1",
					"Settings evaluated",
					"replaced Private-Token: tokenIgnoredNoValue",
					"replaced Private-Token: token0",
					"replaced Private-Token: token1",
					"added Deploy-Token: tokenAdded"
					)
					.containsSubsequence("Maven Repository $repoPrefix-$existingId is using 'token0'",
					"Maven Repository $it-renamed is using 'token0'",
					"Maven Repository $repoPrefix-specialToken is using 'token0'",
					"Maven Repository $repoPrefix-specialToken1 is using 'token1'",
					"Maven Repository $repoPrefix-specialTokenSelection is using 'token1'",
					"Maven Repository $repoPrefix-ignoredNoValue was not added, as no token could be applied!"
					)
					.doesNotContain("Maven Repository $repoPrefix-ignoredNoValue is using '")
		}
	}

	@PluginTest
	void "used in settings and project without applying"(String primer) {
		//given:

		//when:
		BuildResult result = runTest()

		//then:
		realms.each {
			def capitalized = it.capitalize()
			def repoPrefix = "GitLab-${capitalized}"
			assertThat(result.output)
					.contains("BUILD SUCCESSFUL")
					.containsSubsequence(
					"added Job-Token: jobToken",
					"added Private-Token: tokenIgnoredNoValue",
					"added Deploy-Token: token0",
					"added Deploy-Token: token1",
					"Settings evaluated",
					"replaced Private-Token: tokenIgnoredNoValue",
					"replaced Private-Token: token0",
					"replaced Private-Token: token1",
					"added Deploy-Token: tokenAdded"
					)
					.containsSubsequence("Maven Repository $repoPrefix-$existingId is using 'token0'",
					"Maven Repository $it-renamed is using 'token0'",
					"Maven Repository $repoPrefix-specialToken is using 'token0'",
					"Maven Repository $repoPrefix-specialToken1 is using 'token1'",
					"Maven Repository $repoPrefix-specialTokenSelection is using 'token1'",
					"Maven Repository $repoPrefix-ignoredNoValue was not added, as no token could be applied!"
					)
					.doesNotContain("Maven Repository $repoPrefix-ignoredNoValue is using '")
		}
	}

	@PluginTest
	void "subproject test"(String primer) {
		//given:

		//when:
		BuildResult result = runTest()
		assertThat(result.output)
				.contains("BUILD SUCCESSFUL")
				.containsSubsequence(
				"added Job-Token: jobToken",
				"added Private-Token: tokenIgnoredNoValue",
				"added Deploy-Token: token0",
				"added Deploy-Token: token1",
				"Settings evaluated",
				"Configure project :",
				"readding Token from Parent Job-Token: jobToken",
				"readding Token from Parent Private-Token: tokenIgnoredNoValue",
				"readding Token from Parent Deploy-Token: token0",
				"readding Token from Parent Deploy-Token: token1",
				"replaced Private-Token: tokenIgnoredNoValue",
				"replaced Private-Token: token0",
				"replaced Private-Token: token1",
				"Configure project :subproject1",
				"readding Token from Parent Job-Token: jobToken",
				"readding Token from Parent Private-Token: tokenIgnoredNoValue",
				"readding Token from Parent Deploy-Token: token0",
				"readding Token from Parent Deploy-Token: token1",
				"added Deploy-Token: tokenAdded",
				"added Deploy-Token: tokenAdded1",
				"replaced Deploy-Token: token0",
				"Configure project :subproject2",
				"readding Token from Parent Job-Token: jobToken",
				"readding Token from Parent Private-Token: tokenIgnoredNoValue",
				"readding Token from Parent Deploy-Token: token0",
				"readding Token from Parent Deploy-Token: token1",
				"added Deploy-Token: tokenAdded",
				"added Deploy-Token: tokenAdded1",
				"replaced Deploy-Token: token0",
				)
	}
}
