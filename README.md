# Gradle GitLab Repositories Plugin

Handling Maven GitLab dependencies easy. Define multiple tokens and selectively apply them to repositories.

Adding a GitLab repository to your project is tiresome, based on the documentation you need to have different Tokens in
place for different systems.
The [GitLab Documentation](https://docs.gitlab.com/ee/user/packages/maven_repository/#authenticate-to-the-package-registry-with-gradle)
shows the different types of Tokens, which can be used.

There are:

- Job-Tokens for CI
- Private-Tokens for Users
- Deploy-Tokens for external systems

Additionally, all tokens use a different name for the HttpHeaderCredentials.

Sometimes, there is even the need for gradle-plugin specific repositories and for project specific ones, which can also
overlap.

This plugin tries to cover this by allowing to reapply the same plugins, even to each project.

## Background

We were in the migration phase from self-hosted development infrastructure (SCM, Nexus, Jenkins, SonarQube) to GitLab
and Sonarcloud.io. During the migration phase we needed to support all 3 types of Tokens:

- Job Token for GitLab CI
- Deploy Token for 3rd Party systems like Jenkins
- Private Token for local development

Configuring multiple repositories with repeated configuration was bloating our gradle files. Additionally, minor
mistakes like forgetting some parts in the configuration, made it really hard to migrate and follow up.

In the end we investigated and come up with this solution. It will for sure fit not all needs, but it is a starting
point. It might help others to easily setup this configuration and if there is demand, there is also room for
improvements.

# Usage

## Configuration

```groovy
gitLab {
	/**
	 * only relevant for the usage within settings.gradle 
	 * if the gitLab repositories should be also automatically applied to the project
	 * */
	applyToProject = true

	/**
	 * Token configuration also the order in which we try to apply them. 
	 * The key is the name of the token, and the value will be used for application. 
	 * Currently we do have 3 different token classes: 
	 * - PrivateToken 
	 * - DeployToken 
	 * - JobToken (will be always added by default, based on CI_JOB_TOKEN)
	 * */
	token(PrivateToken) {
		it.key = 'private'
		it.value = gitLabPrivateToken // assumed variable in gradle.properties
	}
	token(DeployToken) {
		it.key = 'deploy'
		it.value = System.getenv("GITLAB_DEPLOY_TOKEN")
	}
}
```

## Applying the plugin

The plugin can be used within `build.gradle` and within `settings.gradle`.

If there is no need to apply special repositories to the `build.gradle` it might be enough, to just apply it to the
settings.

### Groovy DSL

#### build.gradle

```groovy
plugins {
	id 'at.schrottner.gitlab-repositories' version '<version>'
}
```

#### settings.gradle

```groovy
buildscript {
	// ...
	dependencies {
		classpath 'at.schrottner.gradle.gitlab-plugin:gitlab-repositories:<version>'
	}
}

apply plugin: 'at.schrottner.gitlab-repositories'
```

### Kotlin DSL

#### build.gradle.kts

```kotlin
plugins {
    id("at.schrottner.gitlab-repositories") version "<version>"
}
```

#### settings.gradle.kts

```kotlin
buildscript {
    // ..
    dependencies {
        classpath("at.schrottner.gradle.gitlab-plugin:gitlab-repositories:<version>")
    }
}

apply(plugin = "at.schrottner.gitlab-repositories")
```

## Adding repositories

The plugin offers you a nice helper method inspired by `gradle-jruby-plugin` to easily add repositories.

```groovy
repositories {
	maven gitLab.project(projectId)
	maven gitLab.project(projectId) {
		name = "custom name"
		tokenSelektor = "" // a name of a configured token
		tokenSelectors = [] // a list of configured tokens, which will be checked based on their order in this set
	}
	maven gitLab.group(groupId)
	maven gitLab.group(groupId) {
		name = "custom name"
		tokenSelektor = "" // a name of a configured token
		tokenSelectors = [] // a list of configured tokens, which will be checked based on their order in this set
	}
}
```

For adding a repository to the maven-publish repositories please use following method. The `owner` needs to be provided
and is a reference to the `repositories`. Be aware that this has to be a groupId

```groovy
publishing {
	repositories {
		gitLab.upload(owner, projectId)
		gitLab.upload(owner, projectId) {
			name = "custom name"
			tokenSelektor = "" // a name of a configured token
			tokenSelectors = [] // a list of configured tokens, which will be checked based on their order in this set
		}

		maven gitLab.upload(projectId)
		maven gitLab.upload(projectId) {
			name = "custom name"
			tokenSelektor = "" // a name of a configured token
			tokenSelectors = [] // a list of configured tokens, which will be checked based on their order in this set
		}
	}
}
```

### Adding a repository

This will add a repository and will apply conditions for the first token matching, and not being empty.

```groovy
// pluginManagment.repositories { // when in settings.gradle
repositories {
	maven gitLab.project(1)
	maven gitLab.group(1)
}

```

### Adding a repository with specific tokens

We can define which tokens should be taken into account (currently order of parameter is ignored)

```groovy
gitLab.project(1) {
	tokenSelectors = ['private', 'deploy']
}
gitLab.group(1) {
	tokenSelectors = ['private', 'deploy']
}
```

Additionally, we can provide one specific token to be used, if the token is not set, or empty, nothing will be done.

```groovy
gitLab.project(1) { tokenSelector = 'deploy' }
gitLab.group(1) { tokenSelector = 'deploy' }
```

## Comparison

with the plugin

```groovy
plugins {
	id 'maven'
	id 'maven-publish'
	id 'at.schrotter.gitlab-repositories' version '<version>'
}

gitLab {
	// jobToken will be applied automatically
	token(DeployToken) {
		name = "deployToken"
		value = System.getenv("GITLAB_DEPLOY_TOKEN")
	}
	token(PrivateToken) {
		name = "privateToken"
		value = gitLabPrivateToken
	}
}

repositories {
	maven gitLab.group("ID")
}

publishing {
	repositories {
		maven gitLab.upload(owner, "ID")
	}
}
```

without this plugin

```groovy
plugins {
	id 'maven'
	id 'maven-publish'
}

repositories {
	maven {
		url 'GitLab Url with ID'
		name "GitLab"
		if (System.getenv("CI_JOB_TOKEN")) {
			credentials(HttpHeaderCredentials) {
				name = 'Job-Token'
				value = System.getenv("CI_JOB_TOKEN")
			}
		} else if (System.getenv("GITLAB_DEPLOY_TOKEN")) {
			credentials(HttpHeaderCredentials) {
				name = 'Deploy-Token'
				value = System.getenv("GITLAB_DEPLOY_TOKEN")
			}
		} else {
			credentials(HttpHeaderCredentials) {
				name = 'Private-Token'
				value = gitLabPrivateToken
			}
		}
		authentication {
			header(HttpHeaderAuthentication)
		}
	}
}

publishing {
	repositories {
		maven {
			url 'GitLab Url with ID'
			name "GitLab"
			if (System.getenv("CI_JOB_TOKEN")) {
				credentials(HttpHeaderCredentials) {
					name = 'Job-Token'
					value = System.getenv("CI_JOB_TOKEN")
				}
			} else if (System.getenv("GITLAB_DEPLOY_TOKEN")) {
				credentials(HttpHeaderCredentials) {
					name = 'Deploy-Token'
					value = System.getenv("GITLAB_DEPLOY_TOKEN")
				}
			} else {
				credentials(HttpHeaderCredentials) {
					name = 'Private-Token'
					value = gitLabPrivateToken
				}
			}
			authentication {
				header(HttpHeaderAuthentication)
			}
		}
	}
}
```
