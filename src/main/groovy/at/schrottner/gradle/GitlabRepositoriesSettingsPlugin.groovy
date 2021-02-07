package at.schrottner.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtensionAware

public class GitlabRepositoriesSettingsPlugin implements Plugin<Settings>{

    public void apply(Settings settings) {
        GitlabRepositoriesExtension extension = settings.extensions.create(GitlabRepositoriesExtension.NAME, GitlabRepositoriesExtension, settings)

        if( extension.applyToProject) {
            settings.gradle.beforeProject { project ->
                GitlabRepositoriesExtension.artifacts.each { key, value ->
                    project.repositories.maven value
                }
            }
        }
    }
}
