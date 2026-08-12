plugins {
    id("java-library")
    id("maven-publish")
}

allprojects {
    val commitSHA = findProperty("commitSHA")?.let { "-${it.toString().take(7)}" }.orEmpty()
    group = property("mod_group_id") as String
    version = "${property("mod_version")}$commitSHA"

    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net/")
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://api.modrinth.com/maven/")
        // maven("https://cursemaven.com")
        // maven("https://www.jitpack.io"
    }
}
