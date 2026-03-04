import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localIdePathProvider = providers.gradleProperty("localIdePath")
val targetAndroidStudioVersionProvider = providers.gradleProperty("androidStudioVersion")
    .orElse(libs.versions.androidStudioTarget.get())

plugins {
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.jvm)
    alias(libs.plugins.compose.plugin)
    alias(libs.plugins.compose)
}

group = "com.wonddak"
version = "2.1.1"

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

configurations.configureEach {
    exclude(group = "org.jetbrains.runtime", module = "jbr-api")
    exclude(group = "org.slf4j", module = "slf4j-api")
}

dependencies {
    compileOnly(compose.desktop.currentOs)
    implementation(libs.compose.desktop.jvm)
    implementation(libs.compose.desktop.jvm.linux.arm64)
    implementation(libs.compose.desktop.jvm.linux.x64)
    implementation(libs.compose.desktop.jvm.macos.arm64)
    implementation(libs.compose.desktop.jvm.macos.x64)
    implementation(libs.compose.desktop.jvm.windows.x64)
    implementation(libs.compose.material.icons.extended.desktop)

//    implementation(libs.android.build.tools)
    implementation(libs.mpfilepicker)
    implementation(libs.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    intellijPlatform {
        // And Read : https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html#2024
        if (localIdePathProvider.isPresent) {
            local(localIdePathProvider.get())
        } else {
            androidStudio(targetAndroidStudioVersionProvider.get())
        }

//        intellijIdeaCommunity("2024.2")

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
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2025.1") {}
            create(IntelliJPlatformType.AndroidStudio, libs.versions.androidStudioTarget.get()) {}
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
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
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
