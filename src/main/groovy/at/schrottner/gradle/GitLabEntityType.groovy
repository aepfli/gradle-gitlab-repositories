package at.schrottner.gradle

enum GitLabEntityType {
	GROUP("Group", "groups"),
	PROJECT("Project", "projects")

	String name
	String endpoint

	GitLabEntityType(String name, String endpoint) {
		this.name = name
		this.endpoint = endpoint
	}

	@Override
	String toString() {
		return name
	}
}