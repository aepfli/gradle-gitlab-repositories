package at.schrottner.gradle

import at.schrottner.gradle.auths.JobToken
import at.schrottner.gradle.auths.Token
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.AuthenticationContainer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.initialization.Settings
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class GitlabRepositoriesExtension {
	public static final String NAME = "gitLab"
	public static final String REPOSITORY_PREFIX = "GITLAB-"
	public static final String LOG_PREFIX = "GitLab Repositories"
	private final Logger logger
	private final RepositoryHandler repositories
	private int afterPosition
	protected String logPrefix

	String baseUrl = "gitlab.com"
	String afterRepository = 'MavenLocal'
	boolean applyToProject = true
	boolean applySettingTokens = true
	Map<String, Token> tokens = [:]

	public static final def artifacts = [:]

	GitlabRepositoriesExtension(Settings settings) {
		logPrefix = "$LOG_PREFIX :: Settings ::"
		this.logger = LoggerFactory.getLogger(GitlabRepositoriesExtension.class)
		this.repositories = settings.pluginManagement.repositories
		setup()
	}

	GitlabRepositoriesExtension(Project project) {
		logPrefix = "$LOG_PREFIX :: Project ($project.name) ::"
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

	def getLogger() {
		logger ?: LoggerFactory.getLogger(GitlabRepositoriesExtension.class)
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

		logger.info("$logPrefix added $token.name: $token.key")
		tokens.put(token.key, token)
	}

	void setAfterRepository(String afterRepository) {
		this.afterRepository = afterRepository
		afterPosition = repositories.indexOf(repositories.findByName(afterRepository))
	}

	MavenArtifactRepository upload(def delegate, String id, Action<MavenConfig> configAction = null) {
		if (!id) {
			logger.info("$logPrefix: No ID provided nothing will happen here :)")
			return null
		}
		def mavenConfig = new MavenConfig(id)

		configAction?.execute(mavenConfig)

		Map<String, Token> applicableTokens = getApplicableTokens(mavenConfig)

		def artifactRepo = generateMavenArtifactRepositoryAction(
				mavenConfig.name,
				id,
				applicableTokens)
		def repo = (delegate as RepositoryHandler).maven(artifactRepo)
		return repo
	}
	/**
	 * Generates a MavenArtifactRepository and adds it to the maven repositories.
	 *
	 * Additionally the generated Repository will be stored in a static variable,
	 * which can later be used to be applied to evaluated projects.
	 *
	 * @param id id of the GitLab Group or Project, where you want to fetch from
	 * @param configAction to configure the MavenConfiguration
	 * @return
	 */
	MavenArtifactRepository maven(String id, Action<MavenConfig> configAction = null) {
		if (!id) {
			logger.info("$logPrefix: No ID provided nothing will happen here :)")
			return null
		}
		def mavenConfig = new MavenConfig(id)

		configAction?.execute(mavenConfig)

		def repoName = mavenConfig.name
		if (!repositories.findByName(repoName)) {

			Map<String, Token> applicableTokens = getApplicableTokens(mavenConfig)

			def artifactRepo = generateMavenArtifactRepositoryAction(
					repoName,
					id,
					applicableTokens)

			return applyMaven(artifactRepo)
		} else {
			def existingRepo = repositories.getByName(repoName)
			if (existingRepo instanceof MavenArtifactRepository) {
				logger.info("$logPrefix: Maven Repository with $repoName already exists!")
				return existingRepo
			} else {
				logger.info("$logPrefix: Repository with $repoName already exists - but it is not a Maven Repository!")
				return null
			}
		}
	}

	private MavenArtifactRepository applyMaven(Action<MavenArtifactRepository> artifactRepo) {
		if (!artifactRepo) return null
		def repo = repositories.maven(artifactRepo)

		repositories.remove(repo)

		repositories.add(++afterPosition, repo)
		repo
	}

	private Map<String, Token> getApplicableTokens(MavenConfig mavenConfig) {
		Map<String, Token> applicableTokens = mavenConfig.tokenSelectors.collectEntries {
			def token = tokens.get(it)
			if (token)
				[{ it }: token]
		}
		logger.debug("$logPrefix: Maven Repository with $mavenConfig.name will try to use following tokens ${applicableTokens.keySet()}")

		applicableTokens
	}

	private Action<MavenArtifactRepository> generateMavenArtifactRepositoryAction(repoName, id, Map<String, Token> tokens) {
		Token token = tokens.values().find { token ->
			token.value
		}
		if (token) {
			logger.info("$logPrefix: Maven Repository $repoName is using '${token.key}'")
			def artifactRepo = new Action<MavenArtifactRepository>() {
				@Override
				void execute(MavenArtifactRepository mvn) {
					mvn.url = "https://$baseUrl/api/v4/groups/$id/-/packages/maven"
					mvn.name = repoName

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

			artifacts[repoName] = artifactRepo
			return artifactRepo

		} else {
			logger.error("$LOG_PREFIX: Maven Repository $repoName was not added, as no token could be applied!\n\t"
					+ "\n\t"
					+ "#################################################################################### \n\t"
					+ "#################################################################################### \n\t"
					+ "#################################################################################### \n\t"
					+ "Currently you have configured following tokens, but non seem to resolve to an value: \n\t"
					+ "\t- ${tokens.keySet().join("\n\t- ")} \n\t"
					+ "\n\t"
					+ "				Please verify your configuration - Thank you! \n\t"
					+ "\n\t"
					+ "#################################################################################### \n\t"
					+ "#################################################################################### \n\t"
					+ "#################################################################################### \n\t"
					+ "")
			return null
		}
	}

	private class MavenConfig {
		String tokenSelector
		Set<String> tokenSelectors
		String name
		String id

		MavenConfig(String id) {
			this.id = id
			tokenSelectors = tokens.keySet()
		}

		String getName() {
			name ? "$name" : "$REPOSITORY_PREFIX$id"
		}

		Set<String> getTokenSelectors() {
			if (tokenSelector) {
				logger.info("$logPrefix: Maven Repository $name is using Single Token Selector '$tokenSelector' - other tokens will be ignored")

				[tokenSelector].toSet()
			} else {
				tokenSelectors
			}
		}
	}
}