package at.schrottner.gradle.auths

abstract class Token {
	String name
	String value
	String key

	Token(String name, String value, String key) {
		this.name = name
		this.value = value
		this.key = key
	}

	Token(String name) {
		this.name = name
	}
}