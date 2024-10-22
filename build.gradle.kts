plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.wonddak"
version = "1.3.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}
dependencies {
    intellijPlatform {
        instrumentationTools()
//        local("/Applications/Android Studio.app")
        androidStudio("2024.1.3.1")
        bundledPlugin("org.jetbrains.android")

    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
// And Read : https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html#2024
intellijPlatform {
    pluginConfiguration {
        name = "FontHelper"
        ideaVersion.sinceBuild.set("223")
        ideaVersion.untilBuild.set("242.*")
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
    instrumentCode = true
}