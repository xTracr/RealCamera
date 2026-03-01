plugins {
    id("java-library")
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.140" apply false
    id("maven-publish")
}

allprojects {
    group = project.property("mod_group_id") as String
    version = project.property("mod_version") as String

    repositories {
        mavenCentral()
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/releases/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
    }
}
