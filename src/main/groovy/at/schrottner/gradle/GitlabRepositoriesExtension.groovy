package at.schrottner.gradle


import at.schrottner.gradle.auths.JobToken
import at.schrottner.gradle.auths.Token
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.AuthenticationContainer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
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
	public static final String LOG_PREFIX = "GitLab Repositories"
	private final Logger logger
	private final ExtensionContainer extensions
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
		this.extensions = settings.extensions
		this.repositories = settings.pluginManagement.repositories
		setup()
	}

	GitlabRepositoriesExtension(Project project) {
		logPrefix = "$LOG_PREFIX :: Project ($project.name) ::"
		this.logger = project.logger
		this.extensions = project.extensions
		this.repositories = project.repositories
		if (project.extensions.extraProperties.hasProperty('gitLabTokens')) {
			def passedOnTokens = (project.extensions.extraProperties['gitLabTokens'] ?: [:]) as Map<String, Object>
			passedOnTokens.each { key, value ->
				token(Class.forName(value.getClass().name) as Class<? extends Token>) {
					it.key = value['key']
					it.value = value['value']
				}
			}
		}
		setup()
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
	//ArtifactRepository maven(String id, String tokenSelector = "", Set<String> tokenSelectors = tokens.keySet(), boolean addToRepositories = true, String name = "") {
	ArtifactRepository maven(String id, Action<MavenConfig> configAction = null) {
		if (!id) {
			logger.info("$logPrefix: No ID provided nothing will happen here :)")
			return null
		}
		def mavenConfig = new MavenConfig(id)

		configAction?.execute(mavenConfig)

		def repoName = mavenConfig.name
		if (!repositories.findByName(repoName)) {

			Map<String, Token> applicableTokens = getApplicableTokens(mavenConfig)

			def artifactRepo = generateMavenArtifactRepository(
					repoName,
					id,
					applicableTokens)

			if (!artifactRepo) {
				logger.error(
						"""$LOG_PREFIX: Maven Repository $repoName was not added, as no token could be applied!

####################################################################################
####################################################################################
####################################################################################
Currently you have configured following tokens, but non seem to resolve to an value:
\t- ${tokens.keySet().join("\n\t- ")}

				Thank you!

####################################################################################
####################################################################################
####################################################################################
						""")
				return null
			}
			artifacts[repoName] = artifactRepo

			def repo = repositories.maven(artifactRepo)

			repositories.remove(repo)

			/*
			TODO:
				revisit this approach. Maybe this whole approach with using the maven method of repositories can be
				or should be reworked. It was a fasty working hacky solution
			*/
			if (mavenConfig.addToRepositories) {
				repositories.add(++afterPosition, repo)
			}
			return repo
		} else {
			logger.info("$logPrefix: Maven Repository with $repoName already exists!")
			repositories.getByName(repoName)
		}
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

	private Action<MavenArtifactRepository> generateMavenArtifactRepository(repoName, id, Map<String, Token> tokens) {
		Token token = tokens.values().find { token ->
			token.value
		}
		if (token) {
			logger.info("$logPrefix: Maven Repository $repoName is using '${token.key}'")
			new Action<MavenArtifactRepository>() {
				@Override
				void execute(MavenArtifactRepository mvn) {
					/*
					TODO:
						Make this url configurable, so it can be also used for self hosted gitLab instances.
						Additionally it would be cool, if we could provide a parameter, to select form a range of templates.
						But this is currently out of scope.
					 */
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
		}
	}

	private class MavenConfig {
		String tokenSelector
		Set<String> tokenSelectors
		boolean addToRepositories = true
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