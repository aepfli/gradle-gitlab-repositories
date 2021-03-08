package at.schrottner.gradle.mavenConfigs

import at.schrottner.gradle.MavenConfig

class ProjectMavenConfig implements MavenConfig {

	ProjectMavenConfig(String id) {
		this.id = id
		this.endpoint = "projects"
	}

	@Override
	String buildUrl(String baseUrl) {
		"https://$baseUrl/api/v4/$endpoint/$id/packages/maven"
	}
}