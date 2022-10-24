package at.schrottner.gradle

import at.schrottner.gradle.auths.GitLabTokenType
import at.schrottner.gradle.auths.Token
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.AuthenticationContainer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
class RepositoryActionHandler implements Action<MavenArtifactRepository> {

	public static final String REPOSITORY_PREFIX = "GitLab"
	private static final Logger logger = LoggerFactory.getLogger(RepositoryActionHandler)
	private String baseUrl
	private Map<String, Token> tokens
	private RepositoryConfiguration repositoryConfiguration

	RepositoryActionHandler(GitlabRepositoriesExtension extension, RepositoryConfiguration repositoryConfiguration) {
		this.baseUrl = extension.baseUrl
		this.tokens = extension.tokens
		this.repositoryConfiguration = repositoryConfiguration
	}

	@Override
	void execute(MavenArtifactRepository mavenArtifactRepository) {
		TokenInformation tokenInformation = computeTokenInformation(repositoryConfiguration)
		Token token = tokenInformation.token
		Set<String> tokenList = tokenInformation.tokenList
		if (!token) {
			handleInapplicableTokenCase(repositoryConfiguration, tokenList)
			token = new Token(GitLabTokenType.NO_VALUE)
		} else {
			logger.info("${logPrefix(repositoryConfiguration)} is using '${token.key}' '${token.type}'")
		}

		mavenArtifactRepository.url = buildUrl(repositoryConfiguration)
		mavenArtifactRepository.name = buildName(repositoryConfiguration)

		mavenArtifactRepository.credentials(HttpHeaderCredentials) {
			it.name = token?.type.toString()
			it.value = token?.value
		}
		mavenArtifactRepository.authentication(new Action<AuthenticationContainer>() {
			@Override
			@Override
			void execute(AuthenticationContainer authentications) {
				authentications.create('header', HttpHeaderAuthentication)
			}
		})
	}

	private TokenInformation computeTokenInformation(RepositoryConfiguration repositoryConfiguration) {
		Token token
		Set<String> tokenList
		if (repositoryConfiguration.tokenSelector.getOrNull()) {
			logger.info("${logPrefix(repositoryConfiguration)} is using Single Token Selector '${repositoryConfiguration.tokenSelector.get()}' " +
					"- other tokens will be ignored")

			def t = tokens.get(repositoryConfiguration.tokenSelector.get())
			token = t?.value ? t : null
			def str = repositoryConfiguration.tokenSelector.get()
			tokenList = new HashSet<>()
			tokenList.add(str)
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
		return new TokenInformation(token, tokenList)
	}

	private String logPrefix(RepositoryConfiguration repositoryConfiguration) {
		"$Config.LOG_PREFIX Maven Repository ${buildName(repositoryConfiguration)}"
	}

	private String buildName(RepositoryConfiguration repositoryConfiguration) {
		return repositoryConfiguration.name.getOrElse("$REPOSITORY_PREFIX-${repositoryConfiguration.entityType}-${repositoryConfiguration.id}".toString())
	}

	private String buildUrl(RepositoryConfiguration repositoryConfiguration) {
		switch (repositoryConfiguration.entityType) {
			case GitLabEntityType.PROJECT:
				"https://$baseUrl/api/v4/${GitLabEntityType.PROJECT.endpoint}/${repositoryConfiguration.id}/packages/maven"
				break
			case Config.GROUP:
				"https://$baseUrl/api/v4/${GitLabEntityType.GROUP.endpoint}/${repositoryConfiguration.id}/-/packages/maven"
				break
		}
	}

	private void handleInapplicableTokenCase(RepositoryConfiguration repositoryConfiguration, Set<String> applicableTokens) {
		logger.error("${logPrefix(repositoryConfiguration)} was not added, as no token could be applied!\n\t" +
				"\n\t" +
				"#################################################################################### \n\t" +
				"#################################################################################### \n\t" +
				"#################################################################################### \n\t" +
				"Currently you have configured following tokens, but none seem to resolve to a value: \n\t" +
				"\t- ${applicableTokens.join("\n\t\t- ")} \n\t" +
				"\n\t" +
				"				Please verify your configuration - Thank you! \n\t" +
				"\n\t" +
				"#################################################################################### \n\t" +
				"#################################################################################### \n\t" +
				"#################################################################################### \n\t" +
				"")
	}


	private class TokenInformation {
		Token token
		Set<String> tokenList

		TokenInformation(Token token, Set<String> tokenList) {
			this.token = token
			this.tokenList = tokenList
		}
	}
}
