package at.schrottner.gradle

import at.schrottner.gradle.auths.JobToken
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
public class GitlabRepositoriesExtension {

	private static final Logger logger = LoggerFactory.getLogger(RepositoryHandler)
	public static final String NAME = "gitLab"
	private final RepositoryHandler repositories
	private final ObjectFactory objects
	private final RepositoryActionHandler handler

	private int afterPosition
	private String logPrefix

	String baseUrl = "gitlab.com"
	String afterRepository = 'MavenLocal'
	boolean applyToProject = true
	Map<String, Token> tokens = [:]

	public final List<Action<MavenArtifactRepository>> artifactActionStorage = []

	GitlabRepositoriesExtension(Settings settings, ObjectFactory objects) {
		this.objects = objects
		this.logPrefix = "$Config.LOG_PREFIX Settings"
		this.repositories = settings.pluginManagement.repositories
		handler = new RepositoryActionHandler(this)
		setup()
	}

	GitlabRepositoriesExtension(Project project, ObjectFactory objects) {
		this.objects = objects
		this.logPrefix = "$Config.LOG_PREFIX Project ($project.name)"
		this.repositories = project.repositories
		handler = new RepositoryActionHandler(this)

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
				token.value = value.value
				logger.info("$Config.LOG_PREFIX Project ($project.name) readding Token from Parent $token.name: $token.key")
				tokens.put(token.key, token)
			}
			return true
		}
	}

	void setup() {
		logger.info("$logPrefix initializing")
		token(JobToken, {
			it.key = 'jobToken'
			it.value = System.getenv("CI_JOB_TOKEN")
		})
		afterPosition = repositories.indexOf(repositories.findByName(afterRepository))
	}

	void token(Class<? extends Token> tokenClass, Action<Token> action) {
		def token = tokenClass.newInstance();
		action.execute(token)

		logger.info("$logPrefix ${tokens.containsKey(token.key) ? "replaced" : "added"} $token.name: $token.key")
		tokens.put(token.key, token)
	}

	/**
	 * TODO:
	 * 		Improve this logic, currently we are only allowed to set this once, and all will be added consecutively.
	 * 		This is not ideal, and also not really a nice solution. We also want to be able to add overwrite this per
	 * 		repository.
	 *
	 * @param afterRepository
	 */
	void setAfterRepository(String afterRepository) {
		this.afterRepository = afterRepository
		afterPosition = repositories.indexOf(repositories.findByName(afterRepository))
	}

	def upload(def delegate, String id, Action<? super RepositoryConfiguration> configAction = null) {
		RepositoryConfiguration repositoryConfiguration = generateRepositoryConfiguration(id, GitLabEntityType.PROJECT)
		mavenInternal(repositoryConfiguration, configAction) { repo ->
			delegate.maven(repo)
		}
	}

	private RepositoryConfiguration generateRepositoryConfiguration(String id, GitLabEntityType entityType) {
		RepositoryConfiguration repositoryConfiguration = objects.newInstance(RepositoryConfiguration.class)
		repositoryConfiguration.id = id
		repositoryConfiguration.type = entityType
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
					  Action<? super RepositoryConfiguration> configAction = null,
					  Closure<Action<MavenArtifactRepository>> action = null) {

		if (!repositoryConfiguration.id) {
			logger.info("$logPrefix: No ID provided nothing will happen here :)")
			return null
		}

		configAction?.execute(repositoryConfiguration)

		Action<MavenArtifactRepository> artifactRepo = handler.generate(repositoryConfiguration)

		if (artifactRepo) {
			if (action)
				action(artifactRepo)
			else
				applyMaven(artifactRepo)

			artifactActionStorage.add artifactRepo
			artifactRepo
		}
	}

	private MavenArtifactRepository applyMaven(Action<MavenArtifactRepository> artifactRepo) {
		def repo = repositories.maven(artifactRepo)

		repositories.remove(repo)

		repositories.add(++afterPosition, repo)
		repo
	}
}