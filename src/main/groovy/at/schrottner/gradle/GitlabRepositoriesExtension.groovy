package at.schrottner.gradle

import at.schrottner.gradle.auths.GitLabTokenType
import at.schrottner.gradle.auths.Token
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * GitLabRepositoriesExtension is the main entry point to configure the plugin
 *
 * It provides additional methods to automatically add repositories based on GitLab Groups
 * or Projects.
 */
@CompileStatic
class GitlabRepositoriesExtension {

	private static final Logger logger = LoggerFactory.getLogger(RepositoryHandler)
	public static final String NAME = "gitLab"
	private final ObjectFactory objects

	String baseUrl = "gitlab.com"
	boolean applyToProject = true
	Map<String, Token> tokens = [:]

	public final List<Action<MavenArtifactRepository>> artifactActionStorage = []

	GitlabRepositoriesExtension(Settings settings, ObjectFactory objects) {
		this.objects = objects
		setup()
	}

	GitlabRepositoriesExtension(Project project, ObjectFactory objects, GitlabRepositoriesExtension parent = null) {
		this.objects = objects
		if (parent) {
			this.baseUrl = parent.baseUrl
		}
		if (!migrateSettingsTokens(project)) {
			setup()
		}
	}

	private boolean migrateSettingsTokens(Project project) {
		if (project.extensions.extraProperties.has('gitLabTokens')) {
			def passedOnTokens = (project.extensions.extraProperties['gitLabTokens'] ?: [:]) as Map<String, Token>
			passedOnTokens.each { key, value ->
				def token = value
				logger.info("$Config.LOG_PREFIX readding Token from Parent $token.type: $token.key")
				tokens.put(token.key, token)
			}
			return true
		}
		return false
	}

	void setup() {
		logger.info("$Config.LOG_PREFIX initializing")
		token(GitLabTokenType.JOB, {
			it.key = 'jobToken'
			it.value = System.getenv("CI_JOB_TOKEN")
		})
	}

	void token(String tokenType, Action<Token> action) {
		token(GitLabTokenType.valueOf(tokenType.toUpperCase()), action)
	}

	void token(GitLabTokenType tokenType, Action<Token> action) {
		if (!tokenType) {
			throw new IllegalArgumentException('no token')
		}
		def token = new Token(tokenType)
		action.execute(token)

		logger.info("$Config.LOG_PREFIX ${tokens.containsKey(token.key) ? "replaced" : "added"} $token.type: $token.key")
		tokens.put(token.key, token)
	}

	/**
	 * Special endpoint for uploading as GitLab only supports uploads for project, this is using the project endpoint.
	 * it allows to be directly added to a DefaultRepositoryHandler, as the ID might be some env variable only
	 *  	available during CI builds etc. and this might cause on wanted side-effects if it is not resolving to a
	 *  	usable endpoint
	 *
	 * @param id
	 * @param configAction
	 * @return
	 */
	Action<MavenArtifactRepository> upload(String projectId, Action<? super RepositoryConfiguration> configAction = null) {
		RepositoryConfiguration repositoryConfiguration = generateRepositoryConfiguration(projectId, GitLabEntityType.PROJECT)
		mavenInternal(repositoryConfiguration, configAction)
	}


	private RepositoryConfiguration generateRepositoryConfiguration(String id, GitLabEntityType entityType) {
		RepositoryConfiguration repositoryConfiguration = objects.newInstance(RepositoryConfiguration.class, id ?: "NOT_PROPERLY_SET", entityType)
		repositoryConfiguration
	}

	/**
	 * @deprecated use{@link #group(java.lang.String, org.gradle.api.Action)} or {@link #project(java.lang.String, org.gradle.api.Action)}
	 */
	@Deprecated
	Action<MavenArtifactRepository> maven(String id, Action<? super RepositoryConfiguration> configAction = null) {
		group(id, configAction)
	}

	Action<MavenArtifactRepository> group(String id, Action<? super RepositoryConfiguration> configAction = null) {
		RepositoryConfiguration repositoryConfiguration = generateRepositoryConfiguration(id, GitLabEntityType.GROUP)
		mavenInternal(repositoryConfiguration, configAction)
	}

	Action<MavenArtifactRepository> project(String id, Action<? super RepositoryConfiguration> configAction = null) {
		RepositoryConfiguration repositoryConfiguration = generateRepositoryConfiguration(id, GitLabEntityType.PROJECT)
		mavenInternal(repositoryConfiguration, configAction)
	}

	Action<MavenArtifactRepository> mavenInternal(RepositoryConfiguration repositoryConfiguration,
					  Action<? super RepositoryConfiguration> configAction = null) {

		if (!repositoryConfiguration.id) {
			logger.info("$Config.LOG_PREFIX: No ID provided - project will be added anyways, but will not be used")
		}

		configAction?.execute(repositoryConfiguration)

		Action<MavenArtifactRepository> artifactRepo = new RepositoryActionHandler(this, repositoryConfiguration)

		artifactActionStorage.add artifactRepo

		return artifactRepo
	}
}