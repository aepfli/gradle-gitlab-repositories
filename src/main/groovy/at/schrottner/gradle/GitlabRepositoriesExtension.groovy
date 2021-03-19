package at.schrottner.gradle

import at.schrottner.gradle.auths.JobToken
import at.schrottner.gradle.auths.Token
import groovy.transform.CompileStatic
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
	private final RepositoryHandler repositories
	private final ObjectFactory objects
	private final RepositoryActionHandler handler

	String baseUrl = "gitlab.com"
	boolean applyToProject = true
	Map<String, Token> tokens = [:]

	public final List<Action<MavenArtifactRepository>> artifactActionStorage = []

	GitlabRepositoriesExtension(Settings settings, ObjectFactory objects) {
		this.objects = objects
		this.repositories = settings.pluginManagement.repositories
		handler = new RepositoryActionHandler(this)
		setup()
	}

	GitlabRepositoriesExtension(Project project, ObjectFactory objects, GitlabRepositoriesExtension parent = null) {
		this.objects = objects
		this.repositories = project.repositories
		handler = new RepositoryActionHandler(this)
		if (parent) {
			this.baseUrl = parent.baseUrl
		}
		if (!migrateSettingsTokens(project)) {
			setup()
		}
	}

	private boolean migrateSettingsTokens(Project project) {
		if (project.extensions.extraProperties.has('gitLabTokens')) {
			def passedOnTokens = (project.extensions.extraProperties['gitLabTokens'] ?: [:]) as Map<String, Object>
			passedOnTokens.each { key, value ->
				def token = (Class.forName(value.getClass().name) as Class<? extends Token>).newInstance()
				token.key = key
				token.value = value['value']
				logger.info("$Config.LOG_PREFIX readding Token from Parent $token.name: $token.key")
				tokens.put(token.key, token)
			}
			return true
		}
		return false
	}

	void setup() {
		logger.info("$Config.LOG_PREFIX initializing")
		token(JobToken, {
			it.key = 'jobToken'
			it.value = System.getenv("CI_JOB_TOKEN")
		})
	}

	void token(Class<? extends Token> tokenClass, Action<Token> action) {
		def token = tokenClass.newInstance();
		action.execute(token)

		logger.info("$Config.LOG_PREFIX ${tokens.containsKey(token.key) ? "replaced" : "added"} $token.name: $token.key")
		tokens.put(token.key, token)
	}

	def upload(String id, Action<? super RepositoryConfiguration> configAction = null) {
		RepositoryConfiguration repositoryConfiguration = generateRepositoryConfiguration(id, GitLabEntityType.PROJECT)
		mavenInternal(repositoryConfiguration, configAction)
	}

	private RepositoryConfiguration generateRepositoryConfiguration(String id, GitLabEntityType entityType) {
		RepositoryConfiguration repositoryConfiguration = objects.newInstance(RepositoryConfiguration.class, id, entityType)
		repositoryConfiguration
	}

	/**
	 * @deprecated use{@link #group(java.lang.String, org.gradle.api.Action)} or {@link #project(java.lang.String, org.gradle.api.Action)}
	 */
	@Deprecated
	def maven(String id, Action<? super RepositoryConfiguration> configAction = null) {
		group(id, configAction)
	}

	def group(String id, Action<? super RepositoryConfiguration> configAction = null) {
		def repositoryConfiguration = generateRepositoryConfiguration(id, GitLabEntityType.GROUP)
		mavenInternal(repositoryConfiguration, configAction)
	}

	def project(String id, Action<? super RepositoryConfiguration> configAction = null) {
		def repositoryConfiguration = generateRepositoryConfiguration(id, GitLabEntityType.PROJECT)
		mavenInternal(repositoryConfiguration, configAction)
	}

	def mavenInternal(RepositoryConfiguration repositoryConfiguration,
					  Action<? super RepositoryConfiguration> configAction = null) {

		if (!repositoryConfiguration.id) {
			logger.info("$Config.LOG_PREFIX: No ID provided - project will be added anyways, but will not be used")
		}

		configAction?.execute(repositoryConfiguration)

		Action<MavenArtifactRepository> artifactRepo = handler.generate(repositoryConfiguration)

		artifactActionStorage.add artifactRepo

		return artifactRepo
	}
}