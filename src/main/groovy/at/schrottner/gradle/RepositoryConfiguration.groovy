package at.schrottner.gradle

import org.gradle.api.Action
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.artifacts.BaseRepositoryFactory
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

/**
 * Representation of the actual configuration done within the gradle definition
 *
 * TODO:
 * 	- add ContentFiltering
 */
abstract class RepositoryConfiguration {
	String id
	GitLabEntityType entityType

	@Inject
	RepositoryConfiguration(String id, GitLabEntityType entityType) {
		this.id = id
		this.entityType = entityType
	}

	abstract public Property<String> getTokenSelector()

	abstract public SetProperty<String> getTokenSelectors()

	abstract public Property<String> getName()

}