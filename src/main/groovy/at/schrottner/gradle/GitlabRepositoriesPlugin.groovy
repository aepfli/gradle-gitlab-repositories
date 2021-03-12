package at.schrottner.gradle

import at.schrottner.gradle.auths.DeployToken
import at.schrottner.gradle.auths.JobToken
import at.schrottner.gradle.auths.PrivateToken
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension

import javax.inject.Inject

class GitlabRepositoriesPlugin implements Plugin<ExtensionAware> {

	ObjectFactory objects

	@Inject
	GitlabRepositoriesPlugin(ObjectFactory objectFactory) {
		this.objects = objectFactory
	}

	void apply(ExtensionAware extensionAware) {
		addProps(extensionAware)

		if (extensionAware instanceof Project) {
			apply(extensionAware)
		} else if (extensionAware instanceof Settings) {
			apply(extensionAware)
		}
	}

	void addProps(ExtensionAware extensionAware) {
		extensionAware.extensions.extraProperties.set('DeployToken', DeployToken.class)
		extensionAware.extensions.extraProperties.set('PrivateToken', PrivateToken.class)
		extensionAware.extensions.extraProperties.set('JobToken', JobToken.class)
	}

	void apply(Settings extensionAware) {
		GitlabRepositoriesExtension extension = extensionAware.extensions.create(
				GitlabRepositoriesExtension.NAME,
				GitlabRepositoriesExtension,
				(Settings) extensionAware,
				objects
		)
		extensionAware.gradle.beforeProject { Project project ->
			if (extension.applyToProject) {
				applyProjects(extension, project)
				addProps(project)
				project.extensions.add(
						GitlabRepositoriesExtension.NAME,
						extension
				)
			}

			def ext = project.extensions.findByName(ExtraPropertiesExtension.EXTENSION_NAME)
			ext.gitLabTokens = extension.tokens
		}
	}

	void apply(Project extensionAware) {
		def extension = extensionAware.extensions.findByName(GitlabRepositoriesExtension.NAME) ?:
				extensionAware.extensions.create(
						GitlabRepositoriesExtension.NAME,
						GitlabRepositoriesExtension,
						(Project) extensionAware,
						objects
				)

		def task = extensionAware.tasks.maybeCreate('gitLabTask', DefaultTask)
		task.doLast {
			println "GitLab Repository tokens:"
			extension.tokens.each { key, value ->
				println "- $key: ${value.getClass().simpleName}"
			}
		}
	}

	private void applyProjects(extension, project) {
		extension.artifactActionStorage.each { value ->
			project.repositories.maven value
		}
	}

}
