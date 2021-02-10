package at.schrottner.gradle

import at.schrottner.gradle.auths.DeployToken
import at.schrottner.gradle.auths.JobToken
import at.schrottner.gradle.auths.PrivateToken
import at.schrottner.gradle.auths.Token
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.AuthenticationContainer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.Credentials
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
public class GitlabRepositoriesExtension {
	public static final String NAME = "gitLab"
	public static final String REPOSITORY_PREFIX = "GITLAB-"
	private final Logger logger
	private final ExtensionContainer extensions
	private final RepositoryHandler repositories
	private int afterPosition

	String afterRepository = 'MavenLocal'
	boolean applyToProject = true
	Map<String, Token> tokens = [:]

	public static final def artifacts = [:]

	GitlabRepositoriesExtension(Settings settings) {
		this.logger = LoggerFactory.getLogger(GitlabRepositoriesExtension.class)
		this.extensions = settings.extensions
		this.repositories = settings.pluginManagement.repositories
		setup()
	}

	GitlabRepositoriesExtension(Project project) {
		this.logger = project.logger
		this.extensions = project.extensions
		this.repositories = project.repositories
		setup()
	}

	void setup() {
		tokens = [:]
		token(JobToken, {
			it.key = 'jobToken'
			it.value = System.getenv("CI_JOB_TOKEN")
		})
		afterPosition = repositories.indexOf(repositories.findByName(afterRepository))
	}

	void token(Class<? extends Token> tokenClass, Action< Token> action) {

		def token = tokenClass.newInstance();
		action.execute(token)
		tokens.put(token.key, token)
		logger.info("GitLab-Repositories added Token: $token.key for $token.name")
	}

	void setAfterRepository(String afterRepository) {
		this.afterRepository = afterRepository
		afterPosition = repositories.indexOf(repositories.findByName(afterRepository))
	}

	ArtifactRepository maven(String id, Set<String> tokenSelector = tokens.keySet()) {
		def repoName = "$REPOSITORY_PREFIX-$id"
		if (!repositories.findByName(repoName)) {

			def artifactRepo = generateMavenArtifactRepository(
					repoName,
					id,
					tokens.findAll { key, value ->
						tokenSelector.contains(key)
					})
			artifacts[repoName] = artifactRepo
			def repo = repositories.maven(artifactRepo)
			// TODO:  rethink this approach do we really need a action? is the removing and readding really a good idea?
			repositories.remove(repo)
			repositories.add(++afterPosition, repo)
		} else {
			logger.info("GitLab-Repositories: $repoName already exists, i will not reapply it!")
			repositories.getByName(repoName)
		}
	}

	private Action<MavenArtifactRepository> generateMavenArtifactRepository(repoName, id, Map<String, Token> tokens) {
		Token token = tokens.values().find { token ->
			token.value
		}
		if (token) {
			logger.info("GitLab-Repositories: $repoName is using ${token['name']}")
			new Action<MavenArtifactRepository>() {
				@Override
				void execute(MavenArtifactRepository mvn) {
					mvn.url = "https://gitlab.com/api/v4/groups/$id/-/packages/maven"
					mvn.name = repoName
					// on GitLab CI

					mvn.credentials(HttpHeaderCredentials) {
						it.name = token.name
						it.value = token.value
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
}