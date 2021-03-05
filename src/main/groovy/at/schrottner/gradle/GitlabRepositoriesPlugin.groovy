package at.schrottner.gradle

import at.schrottner.gradle.auths.DeployToken
import at.schrottner.gradle.auths.JobToken
import at.schrottner.gradle.auths.PrivateToken
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension

class GitlabRepositoriesPlugin implements Plugin<ExtensionAware> {

	void apply(ExtensionAware extensionAware) {

		extensionAware.extensions.extraProperties.set('DeployToken', DeployToken.class)
		extensionAware.extensions.extraProperties.set('PrivateToken', PrivateToken.class)
		extensionAware.extensions.extraProperties.set('JobToken', JobToken.class)

		if (extensionAware instanceof Project) {
			apply(extensionAware)
		} else if (extensionAware instanceof Settings) {
			apply(extensionAware)
		}
	}

	void apply(Settings extensionAware) {
		GitlabRepositoriesExtension extension = extensionAware.extensions.create(GitlabRepositoriesExtension.NAME, GitlabRepositoriesExtension, (Settings) extensionAware)
		extensionAware.gradle.beforeProject { project ->
			if (extension.applyToProject)
				applyProjects(project)
		}
		extensionAware.gradle.beforeProject { project ->
			def ext = project.extensions.findByName(ExtraPropertiesExtension.EXTENSION_NAME)
			ext.gitLabTokens = extension.tokens
		}
	}

	void apply(Project extensionAware) {
		def extension = extensionAware.extensions.findByName(GitlabRepositoriesExtension.NAME) ?:
				extensionAware.extensions.create(
						GitlabRepositoriesExtension.NAME,
						GitlabRepositoriesExtension,
						(Project) extensionAware
				)

		def task = extensionAware.tasks.maybeCreate('gitLabTask', DefaultTask)
		task.doLast {
			println "GitLab Repository tokens:"
			extension.tokens.each { key, value ->
				println "- $key: ${value.getClass().simpleName}"
			}
		}
	}

	private static void remapTokens(GitlabRepositoriesExtension extension, pExt) {
		extension.tokens.each {
			pExt.tokens.put it.key, it.value
		}
	}

	private void applyProjects(project) {
		GitlabRepositoriesExtension.artifacts.each { key, value ->
			project.repositories.maven value
		}
	}

}
