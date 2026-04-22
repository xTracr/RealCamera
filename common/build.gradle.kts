plugins {
    id("net.fabricmc.fabric-loom-remap")
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
    mappings(loom.officialMojangMappings())

    compileOnly("org.spongepowered:mixin:0.8.5")
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.3")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.3")
    // We depend on Fabric Loader here to use the Fabric @Environment annotations,
    modCompileOnly("net.fabricmc:fabric-loader:$fabricLoaderVersion")

    // Cloth Config
    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion") {
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
