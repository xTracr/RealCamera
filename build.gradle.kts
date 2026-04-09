plugins {
    id("java-library")
    id("maven-publish")
}

allprojects {
    val commitSHA = findProperty("commitSHA")?.toString()?.take(7)
    group = property("mod_group_id") as String
    version = "${property("mod_version")}$commitSHA"

    repositories {
        mavenCentral()
        listOf(
            "https://maven.shedaniel.me/",
            "https://maven.terraformersmc.com/releases/",
            "https://maven.fabricmc.net/",
            "https://maven.neoforged.net/releases"
        ).forEach(::maven)
    }
}
