package at.schrottner.gradle.configurations

import at.schrottner.gradle.RepositoryConfiguration
import at.schrottner.gradle.GitLabEntityType

class GroupConfiguration extends RepositoryConfiguration {
	GitLabEntityType type = GitLabEntityType.GROUP
}