plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.21"
  id("org.jetbrains.intellij") version "1.17.2"
}

group = "com.wonddak"
version = "1.1.1"

repositories {
  mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
// And Read : https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html#2023
intellij {
  version.set("2023.1.5")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf("android"))
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
  }

  patchPluginXml {
    sinceBuild.set("222")
    untilBuild.set("233.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}

tasks {
  runIde {
    // Absolute path to installed target 3.5 Android Studio to use as
    // IDE Development Instance (the "Contents" directory is macOS specific):
    ideDir.set(file("/Applications/Android Studio.app/Contents"))
  }
}