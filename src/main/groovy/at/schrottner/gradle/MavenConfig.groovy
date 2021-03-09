package at.schrottner.gradle;

import java.util.Set;

trait MavenConfig implements LogHandler {
	public static final String REPOSITORY_PREFIX = "GitLab"
	String tokenSelector
	Set<String> tokenSelectors
	String name
	String id
	String endpoint

	String getName() {
		name ? "$name" : "$REPOSITORY_PREFIX-${(endpoint - 's').capitalize()}-$id"
	}

	Set<String> getTokenSelectors() {
		if (tokenSelector) {
			logger.info("$logPrefix: Maven Repository $name is using Single Token Selector '$tokenSelector' - other tokens will be ignored")

			[tokenSelector].toSet()
		} else {
			tokenSelectors
		}
	}

	String buildUrl(String baseUrl) {
		"https://$baseUrl/api/v4/$endpoint/$id/-/packages/maven"
	}
}