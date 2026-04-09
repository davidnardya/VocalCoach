import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildkonfig)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}
val geminiApiKey =
    (localProperties.getProperty("GEMINI_API_KEY") ?: "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        version = "1.0.0"
        summary = "VocalCoach"
        homepage = "https://example.com"
        ios.deploymentTarget = "15.0"
        framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.tensorflow.lite.task.audio)
            implementation(libs.androidx.work.runtime.ktx)
        }
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.datastore)
            implementation(libs.kotlinx.datetime)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.material.icons.extended)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val iosArm64Main by getting {
            dependencies {
                implementation(project(":tfliteInterop"))
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation(project(":tfliteInterop"))
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

buildkonfig {
    packageName = "org.davidnardya.vocalcoach"
    defaultConfigs {
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "GEMINI_API_KEY",
            geminiApiKey,
        )
    }
}

android {
    namespace = "org.davidnardya.vocalcoach"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "org.davidnardya.vocalcoach"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

/**
 * [composeApp.podspec] vendores `build/cocoapods/framework/ComposeApp.framework`. Gradle often leaves a
 * dummy framework there (placeholder only) until Xcode runs `syncFramework`. Mirror the linked framework
 * here so Swift/Xcode see the real `ComposeApp.h` (e.g. `ComposeUIViewFactory`).
 */
// Sync must include the "ComposeApp.framework" segment in the copy; pointing `from` at the
// .framework directory alone copies its *contents* into `into`, leaving the dummy bundle
// (placeholder.h) in place — Swift then sees no Kotlin exports.
tasks.register<Sync>("syncCocoapodsFrameworkIosSimulatorDebug") {
    dependsOn("linkPodDebugFrameworkIosSimulatorArm64")
    from(layout.buildDirectory.dir("bin/iosSimulatorArm64/podDebugFramework")) {
        include("ComposeApp.framework/**")
    }
    into(layout.projectDirectory.dir("build/cocoapods/framework"))
}
tasks.register<Sync>("syncCocoapodsFrameworkIosSimulatorRelease") {
    dependsOn("linkPodReleaseFrameworkIosSimulatorArm64")
    from(layout.buildDirectory.dir("bin/iosSimulatorArm64/podReleaseFramework")) {
        include("ComposeApp.framework/**")
    }
    into(layout.projectDirectory.dir("build/cocoapods/framework"))
}
tasks.register<Sync>("syncCocoapodsFrameworkIosDeviceDebug") {
    dependsOn("linkPodDebugFrameworkIosArm64")
    from(layout.buildDirectory.dir("bin/iosArm64/podDebugFramework")) {
        include("ComposeApp.framework/**")
    }
    into(layout.projectDirectory.dir("build/cocoapods/framework"))
}
tasks.register<Sync>("syncCocoapodsFrameworkIosDeviceRelease") {
    dependsOn("linkPodReleaseFrameworkIosArm64")
    from(layout.buildDirectory.dir("bin/iosArm64/podReleaseFramework")) {
        include("ComposeApp.framework/**")
    }
    into(layout.projectDirectory.dir("build/cocoapods/framework"))
}
tasks.named("linkPodDebugFrameworkIosSimulatorArm64") { finalizedBy("syncCocoapodsFrameworkIosSimulatorDebug") }
tasks.named("linkPodReleaseFrameworkIosSimulatorArm64") { finalizedBy("syncCocoapodsFrameworkIosSimulatorRelease") }
tasks.named("linkPodDebugFrameworkIosArm64") { finalizedBy("syncCocoapodsFrameworkIosDeviceDebug") }
tasks.named("linkPodReleaseFrameworkIosArm64") { finalizedBy("syncCocoapodsFrameworkIosDeviceRelease") }
