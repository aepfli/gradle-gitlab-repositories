package at.schrottner.gradle.configurations

import at.schrottner.gradle.RepositoryConfiguration
import at.schrottner.gradle.GitLabEntityType

class ProjectConfiguration extends RepositoryConfiguration {
	GitLabEntityType type = GitLabEntityType.PROJECT
}