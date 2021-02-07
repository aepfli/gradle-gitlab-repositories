# gradle-gitlab-repositories-plugin
Handling GitLab repositories made easy. Easy setup for GitLab dependencies, for gradle plugins and others

Adding a GitLab repository to your project is tiresome, based on the documentation you need to have different Tokens in place for different systems.
The [GitLab Documentation](https://docs.gitlab.com/ee/user/packages/maven_repository/#authenticate-to-the-package-registry-with-gradle) shows the different types of Tokens, which can be used.

There are:
- Job-Tokens for CI
- Private-Tokens for Users
- Deploy-Tokens for external systems

Additionally to the different Tokens, they all use a different name for the HttpHeaderCredentials.

Sometimes, there is even the need for gradle-plugin specific repositories and for project specific ones,
which can also overlap.

This plugin tries to cover this, or hopefully will do in the near future.

# Usage

The plugin offers you a nice helper method inspired by `gradle-jruby-plugin` to easily add repositories.

    gitLab.maven(<project or group id>) 

This will create a maven repository entry for the GitLab entity with said ID.

    gitLab.maven(<project or group id>, <List of tokens to apply>)


## in settings.gradle
```groovy
buildscript {
    // ...
    dependencies {
        classpath 'at.schrottner.gradle.gitlab-plugin:gitlab-repositories:<version>'
    }
}

apply plugin: 'at.schrottner.gitlab-settings-repositories'

gitLab {
    // private token to be used
    privateToken = gitLabPrivateToken
    
    /*
    only relevant for the usage within settings.gradle
    if the gitLab repositories should be also automatically applied to the project
    */
    applyToProject = true 
    
    /*
    Token configuration also the order in which we try to apply them.
    The key is the name of the header, and the Closure will be evaluated when applied.
     */
    token = [
        'Job-Token'    : { System.getenv("CI_JOB_TOKEN") },
        'Deploy-Token' : { System.getenv("GITLAB_DEPLOY_TOKEN") },
        'Private-Token': { privateToken }
    ]

    /*
    After the repository with this name, the repositories will be added
     */
	afterRepository = 'MavenLocal'
}
    
gitLab.maven('1')
```

## in build.gradle

```groovy
    plugins {
    id 'maven'
    id 'at.schrottner.gitlab-repositories' version '<version>'
}

gitLab {
// settings
}
repositories {
    // ...
    gitLab.maven('<id>')
}
```