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

import static org.assertj.core.api.Assertions.assertThat

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.junit.jupiter.api.io.TempDir
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// TODO: check if we can parameterize this somehow
abstract class AbstractGitlabRepositoriesPluginFunctionalTests {

	private static final Logger logger = LoggerFactory.getLogger(AbstractGitlabRepositoriesPluginFunctionalTests.class)
	private static def existingId = "1234"
	private static def renamedId = "123"
	private static def realms = ["group", "project"]
	public static final String SETTINGS = "settings"
	public static final String BUILD = "build"
	private pluginClasspath = getClass().classLoader.findResource("plugin-classpath.txt")
	.readLines()
	.collect { it.replace('\\\\', '\\\\\\\\') } // escape backslashes in Windows paths
	.collect { "$it" }
	.join(",")

	File projectDir
	File settingsGradle
	File buildGradle
	File gradleProperties

	abstract String getFileEnding()

	String getFileNameForDSL(String name) {
		return "$name.$fileEnding"
	}

	@BeforeEach
	void setup(@TempDir File projectDir) {
		this.projectDir = projectDir
		gradleProperties = new File(projectDir, "gradle.properties")
		gradleProperties << """
			existingId=$existingId
			renamedId=$renamedId
			pluginClasspath=$pluginClasspath
			realms=${realms.join(',')}
		"""
	}

	@Test
	void "only used in settings"() {
		//given:
		settingsGradle = TestFileUtils.getTestResource(new File(projectDir, getFileNameForDSL(SETTINGS)), getFileNameForDSL(SETTINGS))
		buildGradle = new File(projectDir, getFileNameForDSL(BUILD))

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

	@Test
	void "only used in project"() {
		//given:
		buildGradle = TestFileUtils.getTestResource(new File(projectDir, getFileNameForDSL(BUILD)), getFileNameForDSL(BUILD))

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

	@Test
	void "used in settings and project"() {
		//given:
		settingsGradle = TestFileUtils.getTestResource(new File(projectDir, getFileNameForDSL(SETTINGS)), getFileNameForDSL(SETTINGS))
		buildGradle = TestFileUtils.getTestResource(new File(projectDir, getFileNameForDSL(BUILD)), getFileNameForDSL(BUILD))

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

	@Test
	void "used in settings and project without applying"() {
		//given:
		settingsGradle = TestFileUtils.getTestResource(new File(projectDir, getFileNameForDSL(SETTINGS)), getFileNameForDSL(SETTINGS))
		buildGradle = TestFileUtils.getTestResource(new File(projectDir, getFileNameForDSL(BUILD)), getFileNameForDSL("build-withoutApplying"))

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

	@Test
	@DisabledIfEnvironmentVariable(
	named = 'TEST_UPLOAD_TOKEN',
	matches = '^$',
	disabledReason = 'Upload deactivated due to missing TEST_UPLOAD_TOKEN'
	)
	void "uploadTest"() {
		def testFile = TestFileUtils.getTestResource(new File(projectDir, 'test.xml'), 'test.xml')
		settingsGradle = TestFileUtils.getTestResource(new File(projectDir, getFileNameForDSL(SETTINGS)), getFileNameForDSL(SETTINGS))
		buildGradle = TestFileUtils.getTestResource(new File(projectDir, getFileNameForDSL(BUILD)), getFileNameForDSL("build-upload"))

		def uploadResult = runTest("publishTestPublicationToGitLabRepository", "-i", "-s")
		def repoPrefix = "GitLab-Project"
		assertThat(uploadResult.output)
				.contains("BUILD SUCCESSFUL")
				.containsSubsequence(
				"added Job-Token: jobToken",
				"added Private-Token: tokenIgnoredNoValue",
				"added Deploy-Token: token0",
				"added Deploy-Token: token1",
				"Settings evaluated",
				"added Private-Token: testToken"
				)
				.containsSubsequence("Maven Repository $repoPrefix-$existingId is using 'token0'",
				"Maven Repository $repoPrefix-specialToken is using 'token0'",
				"Maven Repository $repoPrefix-specialToken1 is using 'token1'",
				"Maven Repository $repoPrefix-specialTokenSelection is using 'token1'",
				"Maven Repository $repoPrefix-ignoredNoValue was not added, as no token could be applied!",
				"Maven Repository GitLab is using 'testToken'",
				)
				.doesNotContain("Maven Repository $repoPrefix-ignoredNoValue is using '")
				.contains("Publishing to repository 'GitLab'")

	}

	@Test
	void "subprojectTest"() {
		//given:
		FileUtils.copyDirectory(new File(ClassLoader.getSystemClassLoader().getResource('subprojectTest').toURI()), projectDir)

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

	private BuildResult runTest(String[] args = ["tasks", "-i", "-s"]) {
		def runner = GradleRunner.create()
		runner.forwardOutput()
		runner.withPluginClasspath()
		runner.withArguments(args)
		runner.withProjectDir(projectDir)
		runner.build()
	}

}
