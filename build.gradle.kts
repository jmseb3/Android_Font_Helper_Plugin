import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

plugins {
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.jvm)
    alias(libs.plugins.compose.plugin)
    alias(libs.plugins.compose)
}

group = "com.wonddak"
version = "2.0.0"

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    compileOnly(compose.desktop.currentOs)
    implementation(compose.desktop.common)
    implementation(compose.desktop.linux_arm64)
    implementation(compose.desktop.linux_x64)
    implementation(compose.desktop.macos_arm64)
    implementation(compose.desktop.macos_x64)
    implementation(compose.desktop.windows_x64)
    implementation(compose.materialIconsExtended)

//    implementation(libs.android.build.tools)
    implementation("com.darkrockstudios:mpfilepicker:3.1.0")

    intellijPlatform {
        // And Read : https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html#2024
        androidStudio("2024.3.1.13")

//        intellijIdeaCommunity("2024.2")

        bundledPlugin("org.jetbrains.kotlin")
        zipSigner()
    }
}


// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        name = "FontHelper"
        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        failureLevel = listOf(
            FailureLevel.COMPATIBILITY_WARNINGS,
            FailureLevel.COMPATIBILITY_PROBLEMS,
            FailureLevel.DEPRECATED_API_USAGES,
            FailureLevel.OVERRIDE_ONLY_API_USAGES,
            FailureLevel.NON_EXTENDABLE_API_USAGES,
            FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
            FailureLevel.MISSING_DEPENDENCIES,
            FailureLevel.INVALID_PLUGIN,
            FailureLevel.NOT_DYNAMIC,
        )
        ides {
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2.4")
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
        }
    }
    signing {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = System.getenv("PUBLISH_TOKEN")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    // workaround for https://youtrack.jetbrains.com/issue/IDEA-285839/Classpath-clash-when-using-coroutines-in-an-unbundled-IntelliJ-plugin
    buildPlugin {
        exclude { "coroutines" in it.name }
    }
    prepareSandbox {
        exclude { "coroutines" in it.name }
    }
}