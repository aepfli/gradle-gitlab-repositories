package at.schrottner.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.AuthenticationContainer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.authentication.http.HttpHeaderAuthentication


@CompileStatic
public class GitlabRepositoriesExtension {

	public static final String NAME = "gitLab"
	public static final String REPOSITORY_PREFIX = "GITLAB-"
	private final Project project
	private final ExtensionContainer extensions
	private final RepositoryHandler repositories

	private static def ADDED_REPOSITORIES = [:]

	GitlabRepositoriesExtension(Project project) {
		this.project = project
		this.extensions = project.extensions
		this.repositories = project.repositories

		project.logger.error("INITIALIZED")
	}

	ArtifactRepository maven(String id) {
		def repoName = "$REPOSITORY_PREFIX-$id"
		if (!repositories.getByName(repoName)) {
			def artifactRepo = generateMavenArtifactRepository(repoName)
			ADDED_REPOSITORIES[repoName] = artifactRepo
			repositories.maven(artifactRepo)
		} else {
			project.logger.info("$repoName already exists")
			repositories.getByName(repoName)
		}
	}

	private Action<MavenArtifactRepository> generateMavenArtifactRepository(repoName) {
		new Action<MavenArtifactRepository>() {
			@Override
			void execute(MavenArtifactRepository mvn) {
				mvn.url = "url"
				mvn.name = repoName
				def credentialName
				def credentialValue
				// on GitLab CI
				if (System.getenv("CI_JOB_TOKEN")) {
					project.logger.info("$repoName is using CI_JOB_TOKEN")
					credentialName = 'Job-Token'
					credentialValue = System.getenv("CI_JOB_TOKEN")
				}
				// on jenkins
				else if (System.getenv("GITLAB_DEPLOY_TOKEN")) {
					project.logger.info("$repoName is using GITLAB_DEPLOY_TOKEN")
					// This is actually an Private Token, but deploy keys do not work on group level for now
					// when gitlab fixes this, we should change this to a Deploy token
					credentialName = 'Deploy-Token'
					credentialValue = System.getenv("GITLAB_DEPLOY_TOKEN")
				}
				// local development
				else if (project.hasProperty('gitLabPrivateToken')) {
					project.logger.info("$repoName is using GitLab Private Token")
					credentialName = 'Private-Token'
					credentialValue = project.properties.gitLabPrivateToken
				}
				mvn.credentials(HttpHeaderCredentials) {
					it.name = credentialName
					it.value = credentialValue
				}
				mvn.authentication(new Action<AuthenticationContainer>() {
					@Override
					void execute(AuthenticationContainer authentications) {
						authentications.create('header', HttpHeaderAuthentication)
					}
				})
			}
		}
	}
}