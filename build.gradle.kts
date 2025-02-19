plugins {
    alias(libs.plugins.intellij.platform)
    java
    alias(libs.plugins.jvm)
    alias(libs.plugins.compose.plugin)
    alias(libs.plugins.compose)
}

group = "com.wonddak"
version = "2.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://packages.jetbrains.team/maven/p/kpm/public")
    google()
    intellijPlatform {
        defaultRepositories()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://packages.jetbrains.team/maven/p/kpm/public")
        mavenCentral()
        google()
    }

}


dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.darkrockstudios:mpfilepicker:3.1.0")
    intellijPlatform {
        local("/Applications/Android Studio.app/Contents")
//        androidStudio("2024.2.2.13")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
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
        ideaVersion.sinceBuild.set("242")
        ideaVersion.untilBuild.set(provider { null })
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