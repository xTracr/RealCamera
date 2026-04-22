plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("maven-publish")
}

val modId = project.property("mod_id") as String
val javaVersion = project.property("java_version") as String
val minecraftVersion = project.property("minecraft_version") as String
val fabricLoaderVersion = project.property("fabric_loader_version") as String
val fabricApiVersion = project.property("fabric_api_version") as String

val clothConfigVersion = project.property("cloth_config_version") as String
val modMenuVersion = project.property("modmenu_version") as String

base {
    archivesName = "$modId-$minecraftVersion-fabric"
}

val commonProject = project(":common")

project.evaluationDependsOn(commonProject.path)

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

    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // Cloth Config
    modRuntimeOnly("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion")
    // Modmenu
    modImplementation("com.terraformersmc:modmenu:$modMenuVersion") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    implementation(commonProject)
}

loom {
    accessWidenerPath.set(project(":common").file("src/main/resources/realcamera.accesswidener"))

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets {
    named("main") {
        resources {
            srcDir(commonProject.sourceSets.main.get().resources)
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.jar {
    from(sourceSets.main.get().output)
    from(commonProject.sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val sourcesJarTask = tasks.named<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
    from(commonProject.sourceSets.main.get().allSource)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("sources")
}

artifacts {
    archives(tasks.jar)
    archives(sourcesJarTask)
}

publishing {
    publications {
        register<MavenPublication>("fabricJar") {
            artifactId = base.archivesName.get()
            artifact(tasks.jar)
            artifact(sourcesJarTask)
        }
    }
}
