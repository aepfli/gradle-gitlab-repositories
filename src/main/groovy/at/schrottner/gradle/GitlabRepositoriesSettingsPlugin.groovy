package at.schrottner.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtensionAware

public class GitlabRepositoriesSettingsPlugin implements Plugin<Settings>{

    public void apply(Settings settings) {
        new GitlabRepositoriesExtension(settings)
    }
}
