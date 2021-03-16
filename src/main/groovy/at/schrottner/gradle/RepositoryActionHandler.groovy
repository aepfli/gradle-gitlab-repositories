package at.schrottner.gradle

import at.schrottner.gradle.auths.Token
import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.AuthenticationContainer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RepositoryActionHandler {

	public static final String REPOSITORY_PREFIX = "GitLab"
	private static final Logger logger = LoggerFactory.getLogger(RepositoryActionHandler)
	private String baseUrl
	private Map<String, Token> tokens

	RepositoryActionHandler(GitlabRepositoriesExtension extension) {
		this.baseUrl = extension.baseUrl
		this.tokens = extension.tokens
	}

	Action<MavenArtifactRepository> generate(RepositoryConfiguration repositoryConfiguration) {
		Token token
		Set<String> tokenList
		(token, tokenList) = computeTokenInformation(repositoryConfiguration)

		if (token) {
			logger.info("${logPrefix(repositoryConfiguration)} is using '${token.key}' '${token.value}'")
			Action<MavenArtifactRepository> artifactRepo = generateArtifactRepositoryAction(repositoryConfiguration, token)
			return artifactRepo
		} else {
			return handleInapplicableTokenCase(repositoryConfiguration, tokenList)
		}
	}

	private List computeTokenInformation(RepositoryConfiguration repositoryConfiguration) {
		Token token
		Set<String> tokenList
		if (repositoryConfiguration.tokenSelector.getOrNull()) {
			logger.info("${logPrefix(repositoryConfiguration)} is using Single Token Selector '${repositoryConfiguration.tokenSelector.get()}' " +
					"- other tokens will be ignored")

			def t = tokens.get(repositoryConfiguration.tokenSelector.get())
			token = t?.value ? t : null
			tokenList = [repositoryConfiguration.tokenSelector.get()]
		} else if (repositoryConfiguration.tokenSelectors.getOrNull()) {
			token = repositoryConfiguration.tokenSelectors.get().findResult {
				def t = tokens.get(it)
				return t?.value ? t : null
			}
			tokenList = repositoryConfiguration.tokenSelectors.get()
		} else {
			token = tokens.values().find {
				it.value
			}
			tokenList = tokens.keySet()
		}
		[token, tokenList]
	}

	private String logPrefix(RepositoryConfiguration repositoryConfiguration) {
		"$Config.LOG_PREFIX Maven Repository ${buildName(repositoryConfiguration)}"
	}

	private Action<MavenArtifactRepository> generateArtifactRepositoryAction(RepositoryConfiguration repositoryConfiguration, Token token) {
		new Action<MavenArtifactRepository>() {
			@Override
			void execute(MavenArtifactRepository mvn) {
				mvn.url = buildUrl(repositoryConfiguration)
				mvn.name = buildName(repositoryConfiguration)

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

	private String buildName(RepositoryConfiguration repositoryConfiguration) {
		return repositoryConfiguration.name.getOrElse("$REPOSITORY_PREFIX-${repositoryConfiguration.type.get()}-${repositoryConfiguration.id.get()}")
	}

	private String buildUrl(RepositoryConfiguration repositoryConfiguration) {
		switch (repositoryConfiguration.type.get()) {
			case GitLabEntityType.PROJECT:
				"https://$baseUrl/api/v4/${GitLabEntityType.PROJECT.endpoint}/${repositoryConfiguration.id.get()}/packages/maven"
				break
			case Config.GROUP:
				"https://$baseUrl/api/v4/${GitLabEntityType.GROUP.endpoint}/${repositoryConfiguration.id.get()}/-/packages/maven"
				break
		}
	}

	private handleInapplicableTokenCase(RepositoryConfiguration repositoryConfiguration, Set<String> applicableTokens) {
		logger.error("${logPrefix(repositoryConfiguration)} was not added, as no token could be applied!\n\t" +
				"\n\t" +
				"#################################################################################### \n\t" +
				"#################################################################################### \n\t" +
				"#################################################################################### \n\t" +
				"Currently you have configured following tokens, but non seem to resolve to an value: \n\t" +
				"\t- ${applicableTokens.join("\n\t\t- ")} \n\t" +
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
