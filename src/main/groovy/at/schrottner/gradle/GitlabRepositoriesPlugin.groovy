package at.schrottner.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtensionAware

public class GitlabRepositoriesPlugin implements Plugin<ExtensionAware> {

	public void apply(ExtensionAware extensionAware) {

		if (extensionAware instanceof Project) {
			extensionAware.extensions.create(
					GitlabRepositoriesExtension.NAME,
					GitlabRepositoriesExtension,
					(Project) extensionAware
			)

			def task = extensionAware.tasks.create('gitLabTask', DefaultTask)
			task.doLast {
				println 'Welcome to GitLab'
			}
		} else if (extensionAware instanceof Settings) {
			GitlabRepositoriesExtension extension = extensionAware.extensions.create(GitlabRepositoriesExtension.NAME, GitlabRepositoriesExtension, (Settings) extensionAware)

			if (extension.applyToProject) {
				extensionAware.gradle.beforeProject { project ->
					GitlabRepositoriesExtension.artifacts.each { key, value ->
						project.repositories.maven value
					}
				}
			}
		}
	}
}
