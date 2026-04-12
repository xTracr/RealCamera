plugins {
    id("net.fabricmc.fabric-loom")
    id("maven-publish")
}

val modId = project.property("mod_id") as String
val javaVersion = project.property("java_version") as String
val minecraftVersion = project.property("minecraft_version") as String
val fabricLoaderVersion = project.property("fabric_loader_version") as String

val clothConfigVersion = project.property("cloth_config_version") as String

base {
    archivesName = "$modId-$minecraftVersion-fabric"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(javaVersion.toInt())
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    // We depend on Fabric Loader here to use mixin and the Fabric @Environment annotations,
    // compileOnly so it doesn't leak into neoforge's runtime classpath
    compileOnly("net.fabricmc:fabric-loader:$fabricLoaderVersion")

    // Cloth Config
    compileOnly("me.shedaniel.cloth:cloth-config:$clothConfigVersion") {
        exclude(group = "net.fabricmc.fabric-api")
    }
}

loom {
    accessWidenerPath.set(file("src/main/resources/realcamera.accesswidener"))
}

publishing {
    publications {
        register<MavenPublication>("commonJar") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
}
