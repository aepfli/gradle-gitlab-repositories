import at.schrottner.gradle.auths.*
import at.schrottner.gradle.*

buildscript {
    val pluginClasspath: String by project
    dependencies {
        classpath(files(pluginClasspath.split(',')))
    }
}

apply(plugin = "at.schrottner.gitlab-repositories")

configure<GitlabRepositoriesExtension> {
    token(PrivateToken::class.javaObjectType, {
        key = "tokenIgnoredNoValue"
        value = ""
    })
    token(PrivateToken::class.javaObjectType, {
        key = "token0"
        value = "test"
    })
    token(PrivateToken::class.javaObjectType, {
        key = "token1"
        value = "test"
    })
    token(DeployToken::class.javaObjectType, {
        key = "tokenAdded"
        value = "test"
    })
}

repositories {
    val realms: String by project
    val existingId: String by project
    val renamedId: String by project
    val gitLab = the<GitlabRepositoriesExtension>()

    maven(gitLab.group("$existingId"))
    maven(gitLab.project("$existingId"))
    maven(gitLab.group("$renamedId") { name.set("group-renamed") })
    maven(gitLab.project("$renamedId") { name.set("project-renamed") })
    maven(gitLab.group("specialToken") { tokenSelector.set("token0") })
    maven(gitLab.project("specialToken") { tokenSelector.set("token0") })
    maven(gitLab.group("specialToken1") { tokenSelector.set("token1") })
    maven(gitLab.project("specialToken1") { tokenSelector.set("token1") })
    maven(gitLab.group("specialTokenSelection") { tokenSelectors.addAll("jobToken", "token1") })
    maven(gitLab.project("specialTokenSelection") { tokenSelectors.addAll("jobToken", "token1") })
    maven(gitLab.group("ignoredNoValue") { tokenSelector.set("tokenIgnoredNoValue") })
    maven(gitLab.project("ignoredNoValue") { tokenSelector.set("tokenIgnoredNoValue") })
}