package at.schrottner.gradle

import at.schrottner.gradle.auths.GitLabTokenType
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.extensibility.DefaultExtraPropertiesExtension

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
		extensionAware.extensions.extraProperties.set('DeployToken', GitLabTokenType.DEPLOY)
		extensionAware.extensions.extraProperties.set('PrivateToken', GitLabTokenType.PRIVATE)
		extensionAware.extensions.extraProperties.set('JobToken', GitLabTokenType.JOB)
	}

	void apply(Settings extensionAware) {
		GitlabRepositoriesExtension extension = extensionAware.extensions.create(
				GitlabRepositoriesExtension.NAME,
				GitlabRepositoriesExtension,
				extensionAware,
				objects
		)

		extensionAware.gradle.beforeProject { Project project ->
			def ext = project.extensions.findByName(DefaultExtraPropertiesExtension.EXTENSION_NAME)
			ext.gitLabTokens = extension.tokens
			if (extension.applyToProject) {
				applyProjects(extension, project)
				addProps(project)
				project.extensions.create(
						GitlabRepositoriesExtension.NAME,
						GitlabRepositoriesExtension,
						project,
						objects,
						extension
				)
			}
		}
	}

	void apply(Project extensionAware) {
		def extension = extensionAware.extensions.findByName(GitlabRepositoriesExtension.NAME) ?:
				extensionAware.extensions.create(
						GitlabRepositoriesExtension.NAME,
						GitlabRepositoriesExtension,
						extensionAware,
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
