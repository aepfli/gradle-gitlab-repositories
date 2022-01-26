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
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable


class UploadTest extends AbstractFunctionalTests {

	@PluginTest
	@DisabledIfEnvironmentVariable(
	named = 'TEST_UPLOAD_TOKEN',
	matches = '^$',
	disabledReason = 'Upload deactivated due to missing TEST_UPLOAD_TOKEN'
	)
	void "uploadTest"(String primer) {
		def testFile = TestFileUtils.getTestResource(new File(projectDir, 'test.xml'), 'test.xml')

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
}
