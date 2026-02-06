import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    java
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.game"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://packages.jetbrains.team/maven/p/kpm/public")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs) {
        exclude(group = compose.material)
    }
    implementation(libs.kotlinx.serilization)
    implementation(libs.okhttp3)
    implementation(libs.nicehttp)
    implementation(libs.net.jna)
    implementation(libs.jewel.int)
    implementation(libs.jewel)
    implementation(libs.java.seleniumhq)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.jsoup)
    implementation(libs.google.gson)
    implementation(libs.xerial.sqlite)
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

}

val jdkLevel = "21"

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(jdkLevel)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "game"
            packageVersion = "1.0.0"
        }
    }
}

tasks {
    withType<JavaExec> {
        afterEvaluate {
            javaLauncher = project.javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(jdkLevel) }
            setExecutable(javaLauncher.map { it.executablePath.asFile.absolutePath }.get())
        }
    }

    jar {
        archiveFileName.set("The_Guide.jar")
    }
}