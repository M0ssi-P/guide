import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    java
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.game"
version = "1.0-SNAPSHOT"

// The one app version: the installers, APP_VERSION in the app (generated below) and the GitHub release tag "v$appVersion".
// Raise it for every release; MSI upgrades need it to go up.
val appVersion = "1.0.1"

val generateAppVersion by tasks.registering {
    val out = layout.buildDirectory.dir("generated/appVersion")
    inputs.property("appVersion", appVersion)
    outputs.dir(out)
    doLast { out.get().file("AppVersion.kt").asFile.apply { parentFile.mkdirs() }.writeText("const val APP_VERSION = \"v$appVersion\"\n") }
}
kotlin.sourceSets["main"].kotlin.srcDir(generateAppVersion)

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
    implementation("org.apache.pdfbox:pdfbox:3.0.3")
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
    implementation("io.github.m0ssi-p:auraplayer-compose:0.1.9")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation("org.commonmark:commonmark:0.21.0")

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
            packageVersion = appVersion
            includeAllModules = true
            val os = org.gradle.internal.os.OperatingSystem.current()
            javaHome = when {
                os.isWindows -> "C:/jbr/win/jbrsdk-21.0.8"
                os.isMacOsX -> "C:/jbr/mac/jbrsdk-21.0.8/Contents/Home"
                os.isLinux -> "C:/jbr/lin/jbrsdk-21.0.8"
                else -> throw GradleException("Unsupported OS")
            }

            windows {
                // Never change this: Windows uses it to recognise newer MSIs of The Guide as upgrades of the same app.
                upgradeUuid = "60391b7f-76ce-46af-a103-5198370d3c10"
                iconFile.set(
                    project.file("src/main/resources/src/logo.ico")
                )
            }
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