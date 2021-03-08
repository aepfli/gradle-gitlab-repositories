package at.schrottner.gradle.mavenConfigs

import at.schrottner.gradle.MavenConfig

class GroupMavenConfig implements MavenConfig {

	GroupMavenConfig(String id) {
		this.id = id
		this.endpoint = "groups"
	}
}