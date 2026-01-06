pluginManagement {
    repositories {
        maven("https://maven.kikugie.dev/releases") // Stonecutter
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.5.1"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions("1.21.4", "1.21.5", "1.21.8", "1.21.11")
        vcsVersion = "1.21.11"
    }
}
