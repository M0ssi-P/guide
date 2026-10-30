plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
}

// Logic shared by the desktop app, the Android phone/TV app and (next) the iPhone and Apple TV apps: no UI, no audio.
kotlin {
    jvmToolchain(21)
    jvm()
    androidTarget { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    // iPhone: Kotlin/Native only builds these on a Mac; on Windows Gradle skips them with a warning.
    iosArm64()
    iosSimulatorArm64()
    // Apple TV
    tvosArm64()
    tvosSimulatorArm64()

    applyDefaultHierarchyTemplate()
    sourceSets {
        // desktop + Android: the network, parsers and files code, built on OkHttp, Jsoup and java.io
        val jvmCommonMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api("com.squareup.okhttp3:okhttp:4.12.0")
                api("com.github.Blatzar:NiceHttp:0.4.13")
                api("org.jsoup:jsoup:1.20.1")
                api("com.google.code.gson:gson:2.13.1")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            }
        }
        jvmMain.get().dependsOn(jvmCommonMain)
        androidMain.get().dependsOn(jvmCommonMain)
        commonTest.dependencies { implementation(kotlin("test")) }
        // the Bible database tests read a small SQLite fixture
        jvmTest.dependencies { implementation(libs.xerial.sqlite) }
    }
}

android {
    namespace = "org.theguide.shared"
    compileSdk = 35
    buildToolsVersion = "35.0.1"
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<Test> { useJUnitPlatform() }
