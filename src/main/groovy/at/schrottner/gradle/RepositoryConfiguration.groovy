package at.schrottner.gradle

/**
 * Representation of the actual configuration done within the gradle definition
 *
 * TODO:
 * 	- add ContentFiltering
 * 	- add possibility to override positioning
 */
abstract class RepositoryConfiguration {
	String tokenSelector
	Set<String> tokenSelectors
	String name
	String id
	String endpoint

}