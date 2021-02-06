package at.schrottner.gradle

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.ExtensionAware

public class GitlabRepositoriesPlugin implements Plugin<Project> {
    public void apply(Project project) {
        ((ExtensionAware) project.repositories).extensions.create(
                GitlabRepositoriesExtension.NAME,
                GitlabRepositoriesPlugin,
                project
        )
    }
}
