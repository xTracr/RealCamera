pluginManagement {
    repositories {
        maven { 
          name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Forge"
            url = uri("https://maven.minecraftforge.net/")
        }
        maven {
            name = "Minecraft"
            url = uri("https://libraries.minecraft.net/")
        }
        gradlePluginPortal()
    }

    plugins {
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version")
        id("net.fabricmc.fabric-loom-remap") version providers.gradleProperty("loom_version")
        id("net.minecraftforge.gradle") version providers.gradleProperty("forge_gradle_version")
        id("net.minecraftforge.renamer") version providers.gradleProperty("forge_renamer_version")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "RealCamera"

include("common")
include("fabric")
include("forge")
