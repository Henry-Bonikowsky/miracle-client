plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.13-SNAPSHOT"
    id("maven-publish")
}

stonecutter {
    const("MC_1_21_4", stonecutter.current.version == "1.21.4")
    const("MC_1_21_5", stonecutter.current.version == "1.21.5")
    const("MC_1_21_8", stonecutter.current.version == "1.21.8")
    const("MC_1_21_11", stonecutter.current.version == "1.21.11")
}

version = property("mod_version") as String
group = "gg.miracle"

base {
    archivesName.set("miracle-client")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.terraformersmc.com/")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // ModMenu API for config screen integration
    modCompileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")

    // WATERMeDIA for video playback (clip backgrounds)
    modImplementation("maven.modrinth:watermedia:2.1.2")
}

loom {
    // Use root source directory for access widener
    accessWidenerPath.set(rootProject.file("src/main/resources/miracle.accesswidener"))
}

// Get current Stonecutter version
val currentVersion = stonecutter.current.version

// Determine Minecraft version dependency range
val minecraftDep = when (currentVersion) {
    "1.21.11" -> ">=1.21.9"         // Covers 1.21.9+
    "1.21.8" -> ">=1.21.8 <1.21.9"  // Covers 1.21.8 only
    "1.21.5" -> ">=1.21.5 <1.21.8"  // Covers 1.21.5 - 1.21.7
    else -> ">=1.21 <1.21.5"        // Covers 1.21.0 - 1.21.4
}

// Get minecraft_version from properties or use current version as fallback
val mcVersion = findProperty("minecraft_version")?.toString() ?: currentVersion

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", mcVersion)
    inputs.property("minecraft_dep", minecraftDep)

    filesMatching("fabric.mod.json") {
        expand(mapOf(
            "version" to project.version,
            "minecraft_version" to mcVersion,
            "minecraft_dep" to minecraftDep
        ))
    }
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}
