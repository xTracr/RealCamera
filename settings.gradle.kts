pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForge"
            url = uri("https://maven.neoforged.net/releases")
        }
        gradlePluginPortal()
    }

    plugins {
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version")
        id("net.neoforged.moddev") version providers.gradleProperty("moddev_version")
    }
}

rootProject.name = "RealCamera"

include("common")
include("fabric")
include("neoforge")
