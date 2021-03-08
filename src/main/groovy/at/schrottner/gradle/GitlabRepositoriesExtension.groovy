package at.schrottner.gradle

import at.schrottner.gradle.auths.JobToken
import at.schrottner.gradle.auths.Token
import at.schrottner.gradle.mavenConfigs.GroupMavenConfig
import at.schrottner.gradle.mavenConfigs.ProjectMavenConfig
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.AuthenticationContainer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.initialization.Settings
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.slf4j.LoggerFactory

public class GitlabRepositoriesExtension implements LogHandler {
	public static final String NAME = "gitLab"
	public static final String REPOSITORY_PREFIX = "GITLAB-"
	private final RepositoryHandler repositories
	private int afterPosition

	String baseUrl = "gitlab.com"
	String afterRepository = 'MavenLocal'
	boolean applyToProject = true
	Map<String, Token> tokens = [:]

	public static final def artifacts = [:]

	GitlabRepositoriesExtension(Settings settings) {
		this.logPrefix = "Settings"
		this.logger = LoggerFactory.getLogger(GitlabRepositoriesExtension.class)
		this.repositories = settings.pluginManagement.repositories
		setup()
	}

	GitlabRepositoriesExtension(Project project) {
		this.logPrefix = "Project ($project.name)"
		this.logger = project.logger
		this.repositories = project.repositories
		migrateSettingsTokens(project)
		setup()
	}

	private void migrateSettingsTokens(Project project) {
		if (project.extensions.extraProperties.has('gitLabTokens')) {
			def passedOnTokens = (project.extensions.extraProperties['gitLabTokens'] ?: [:]) as Map<String, Object>
			passedOnTokens.each { key, value ->
				token(Class.forName(value.getClass().name) as Class<? extends Token>) {
					it.key = value['key']
					it.value = value['value']
				}
			}
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

	void setAfterRepository(String afterRepository) {
		this.afterRepository = afterRepository
		afterPosition = repositories.indexOf(repositories.findByName(afterRepository))
	}

	MavenArtifactRepository upload(def delegate, String id, Action<MavenConfig> configAction = null) {
		mavenInternal(new ProjectMavenConfig(id), configAction) { repo ->
			delegate.maven(repo)
		}
	}

	MavenArtifactRepository group(String id, Action<MavenConfig> configAction = null) {
		mavenInternal(new GroupMavenConfig(id), configAction)
	}

	MavenArtifactRepository project(String id, Action<MavenConfig> configAction = null) {

		mavenInternal(new ProjectMavenConfig(id), configAction)
	}

	/**
	 * @deprecated use{@link #group(id, configAction)} or {@link #project(id, configAction)}
	 */
	@Deprecated
	MavenArtifactRepository maven(String id, Action<MavenConfig> configAction = null) {
		group(id, configAction)
	}

	MavenArtifactRepository mavenInternal(MavenConfig mavenConfig,
										  Action<MavenConfig> configAction = null,
										  Closure<MavenArtifactRepository> action = null) {

		if (!mavenConfig.id) {
			logger.info("$logPrefix: No ID provided nothing will happen here :)")
			return null
		}

		configAction?.execute(mavenConfig)

		Map<String, Token> applicableTokens = getApplicableTokens(mavenConfig)

		def artifactRepo = generateMavenArtifactRepositoryAction(
				mavenConfig,
				applicableTokens)

		if (artifactRepo)
			if (action)
				action(artifactRepo)
			else
				applyMaven(artifactRepo)
	}

	private MavenArtifactRepository applyMaven(Action<MavenArtifactRepository> artifactRepo) {
		def repo = repositories.maven(artifactRepo)

		repositories.remove(repo)

		repositories.add(++afterPosition, repo)
		repo
	}

	private Map<String, Token> getApplicableTokens(MavenConfig mavenConfig) {
		Map<String, Token> applicableTokens = (mavenConfig.tokenSelectors ?: tokens.keySet()).collectEntries {
			def token = tokens.get(it)
			if (token)
				[{ it }: token]
		}
		logger.debug("$logPrefix: Maven Repository with $mavenConfig.name will try to use following tokens ${applicableTokens.keySet()}")

		applicableTokens
	}

	private Action<MavenArtifactRepository> generateMavenArtifactRepositoryAction(mavenConfig, Map<String, Token> tokens) {
		Token token = tokens.values().find { token ->
			token.value
		}
		if (token) {
			logger.info("$logPrefix: Maven Repository $mavenConfig.name is using '${token.key}'")
			def artifactRepo = new Action<MavenArtifactRepository>() {
				@Override
				void execute(MavenArtifactRepository mvn) {
					mvn.url = mavenConfig.buildUrl(baseUrl)
					mvn.name = mavenConfig.name

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

			artifacts[mavenConfig] = artifactRepo
			return artifactRepo

		} else {
			logger.error("$logPrefix Maven Repository $mavenConfig.name was not added, as no token could be applied!\n\t" +
					"\n\t" +
					"#################################################################################### \n\t" +
					"#################################################################################### \n\t" +
					"#################################################################################### \n\t" +
					"Currently you have configured following tokens, but non seem to resolve to an value: \n\t" +
					"\t- ${tokens.keySet().join("\n\t\t- ")} \n\t" +
					"\n\t" +
					"				Please verify your configuration - Thank you! \n\t" +
					"\n\t" +
					"#################################################################################### \n\t" +
					"#################################################################################### \n\t" +
					"#################################################################################### \n\t" +
					"")
			return null
		}
	}
}