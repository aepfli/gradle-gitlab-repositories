import at.schrottner.gradle.auths.*
import at.schrottner.gradle.*

buildscript {
    val pluginClasspath: String by project
    dependencies {
        classpath(files(pluginClasspath.split(',')))
    }
}
plugins {
    `maven-publish`
}
apply(plugin = "at.schrottner.gitlab-repositories")

configure<GitlabRepositoriesExtension> {
    token("private", {
        key = "testToken"
        value = System.getenv("TEST_UPLOAD_TOKEN")
    })
}

publishing {
    repositories {
        val existingId: String by project
        val gitLab = the<GitlabRepositoriesExtension>()
        maven(gitLab.upload("$existingId"))
        maven(gitLab.upload("specialToken") { tokenSelector.set("token0") })
        maven(gitLab.upload("specialToken1") { tokenSelector.set("token1") })
        maven(gitLab.upload("specialTokenSelection") { tokenSelectors.addAll("jobToken", "token1") })
        maven(gitLab.upload("ignoredNoValue") { tokenSelector.set("tokenIgnoredNoValue") })
        maven(gitLab.upload("24974077") {
            name.set("GitLab")
            tokenSelector.set("testToken")
        })
    }

    publications {
        create<MavenPublication>("test") {
            artifactId = "test-file"
            groupId = "at.schrottner.test.gitlab-repositories"
            version = "test-kotlin-SNAPSHOT"
            artifact("test.xml", {
                classifier = "features"
            })
        }
    }
}
