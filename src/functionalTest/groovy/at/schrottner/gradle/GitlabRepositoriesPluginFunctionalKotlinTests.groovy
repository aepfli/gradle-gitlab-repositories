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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GitlabRepositoriesPluginFunctionalKotlinTests {
	private pluginClasspath = getClass().classLoader.findResource("plugin-classpath.txt")
	.readLines()
	.collect { it.replace("\\\\", "\\\\\\\\") } // escape backslashes in Windows paths
	.collect { "\"$it\"" }
	.join(", ")

	File projectDir
	File settingsGradle
	File buildGradle

	@BeforeEach
	void setup(@TempDir File projectDir) {
		this.projectDir = projectDir

		settingsGradle = new File(projectDir, "settings.gradle.kts")
		buildGradle = new File(projectDir, "build.gradle.kts")
	}

	@Test
	void "only used in settings"() {
		//given:
		settingsGradle << """
            $apply    
			configure<GitlabRepositoriesExtension> {
				${generateToken("DeployToken", "DeployToken")}
            }
        """
		buildGradle << """
			tasks.register("gitLabTask") {}
		"""

		//when:
		BuildResult result = runTest()

		//then:
		assertThat(result.output)
				.contains("BUILD SUCCESSFUL")
				.containsSubsequence(
				"added Job-Token: jobToken",
				"added Deploy-Token: token0",
				"added Deploy-Token: token1"
				)
	}

	@Test
	void "only used in project"() {
		//given:
		buildGradle << """
            $apply    
			configure<GitlabRepositoriesExtension> {
				${generateToken("DeployToken", "DeployToken")}
            }
        """

		//when:
		BuildResult result = runTest()

		//then:
		assertThat(result.output)
				.contains("BUILD SUCCESSFUL")
				.containsSubsequence(
				"added Job-Token: jobToken",
				"added Deploy-Token: token0",
				"added Deploy-Token: token1"
				)
	}

	@Test
	void "used in settings and project"() {
		//given:
		settingsGradle << """ 
            $apply
			configure<GitlabRepositoriesExtension> {
				${generateToken("DeployToken", "DeployToken", "DeployToken")}
            }
        """

		buildGradle << """
            $apply
            configure<GitlabRepositoriesExtension> {
				${generateToken("PrivateToken", "PrivateToken")}
            }
        """
		//when:
		BuildResult result = runTest()
		//then:
		assertThat(result.output)
				.contains("BUILD SUCCESSFUL")
				.containsSubsequence(
				"added Job-Token: jobToken",
				"added Deploy-Token: token0",
				"added Deploy-Token: token1",
				"Settings evaluated",
				"replaced Private-Token: token0",
				"replaced Private-Token: token1"
				)
	}

	def getApply() {
		""" 
            import at.schrottner.gradle.auths.*      
            import at.schrottner.gradle.*      
 			buildscript {
				dependencies {
					classpath(files($pluginClasspath))
				}
			}
			
			apply(plugin = "at.schrottner.gitlab-repositories")
		"""
	}

	private BuildResult runTest() {
		def runner = GradleRunner.create()
		runner.forwardOutput()
		runner.withPluginClasspath()
		runner.withArguments("gitLabTask", "-i", "-s")
		runner.withProjectDir(projectDir)
		//		runner. {
		//
		//			// gradle testkit jacoco support
		//			File("./build/testkit/testkit-gradle.properties")
		//					.copyTo(File(projectDir, "gradle.properties"))
		//
		//		}
		runner.build()
	}

	private String generateToken(String... tokenTypes) {
		def output = ""
		tokenTypes.eachWithIndex { it, index ->
			output += """
			token(${it}::class.javaObjectType, Action<Token>({
				key = "token${index}"
				value = "test"
			}))
			"""
		}
		return output
	}
}
