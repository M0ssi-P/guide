import org.gradle.kotlin.dsl.maven
import java.util.Properties

pluginManagement {
    repositories {
        maven("https://jitpack.io")
        maven("https://packages.jetbrains.team/maven/p/kpm/public")
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
        kotlin("plugin.serialization").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
        id("org.jetbrains.kotlin.plugin.compose").version(extra["kotlin.version"] as String)
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    val localProps = Properties()
    val localPropsFile = File(rootDir, "local.properties")

    if (localPropsFile.exists()) {
        localProps.load(localPropsFile.inputStream())
    }

    repositories {
        maven("https://jitpack.io")
        maven("https://packages.jetbrains.team/maven/p/kpm/public")
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/M0ssi-P/AuraPlayer")
            credentials {
                username = "M0ssi-P"
                password = localProps.getProperty("githubToken") ?: System.getenv("MY_GITHUB_TOKEN")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "The_Guide"
