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

import at.schrottner.gradle.testhelper.AfterBeforeParameterResolver
import org.apache.commons.text.CaseUtils
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// TODO: check if we can parameterize this somehow

@ExtendWith(AfterBeforeParameterResolver.class)
class AbstractFunctionalTests {

	private static final Logger logger = LoggerFactory.getLogger(AbstractFunctionalTests.class)
	protected static def existingId = "1234"
	protected static def renamedId = "123"
	protected static def realms = ["group", "project"]
	protected pluginClasspath = getClass().classLoader.findResource("plugin-classpath.txt")
	.readLines()
	.collect { it.replace('\\\\', '\\\\\\\\') } // escape backslashes in Windows paths
	.collect { "$it" }
	.join(",")

	File projectDir
	File gradleProperties

	@BeforeEach
	void setup(String primer, @TempDir File projectDir, TestInfo testInfo) {
		this.projectDir = projectDir
		gradleProperties = new File(projectDir, "gradle.properties")
		gradleProperties << """
			existingId=$existingId
			renamedId=$renamedId
			pluginClasspath=$pluginClasspath
			realms=${realms.join(',')}
		"""

		testInfo.testMethod.ifPresent {
			FileUtils.copyDirectory(
					new File(
					ClassLoader
					.getSystemClassLoader()
					.getResource("${it.declaringClass.simpleName}/${CaseUtils.toCamelCase(it.name, false)}/$primer")
					.toURI()), projectDir)
		}
	}

	protected BuildResult runTest(String[] args = ["tasks", "-i", "-s"]) {
		def runner = GradleRunner.create()
		runner.forwardOutput()
		runner.withPluginClasspath()
		runner.withArguments(args)
		runner.withProjectDir(projectDir)
		runner.build()
	}
}
