package at.schrottner.gradle;

import java.util.Set;

trait MavenConfig implements LogHandler {
	String tokenSelector
	Set<String> tokenSelectors
	String name
	String id
	String endpoint

	String getName() {
		name ? "$name" : "$GitlabRepositoriesExtension.REPOSITORY_PREFIX$id"
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