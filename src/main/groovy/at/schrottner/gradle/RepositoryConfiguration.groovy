package at.schrottner.gradle

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Representation of the actual configuration done within the gradle definition
 *
 * TODO:
 * 	- add ContentFiltering
 * 	- add possibility to override positioning
 */
abstract class RepositoryConfiguration {
	abstract public Property<String> getTokenSelector()

	abstract public SetProperty<String> getTokenSelectors()

	abstract public Property<String> getName()

	abstract public Property<String> getId()

	abstract public Property<GitLabEntityType> getType()
}