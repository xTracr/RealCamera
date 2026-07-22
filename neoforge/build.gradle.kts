plugins {
    id("java-library")
    id("net.neoforged.moddev")
    id("maven-publish")
}

val modId = project.property("mod_id") as String
val javaVersion = project.property("java_version") as String
val minecraftVersion = project.property("minecraft_version") as String
val neoforgeVersion = project.property("neoforge_version") as String

val clothConfigVersion = project.property("cloth_config_version") as String

base {
    archivesName = "$modId-$minecraftVersion-neoforge"
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
    // Cloth Config
    runtimeOnly("me.shedaniel.cloth:cloth-config-neoforge:$clothConfigVersion")

    implementation(commonProject.sourceSets.main.get().output)
}

neoForge {
    version = neoforgeVersion

    runs {
        register("neoforgeClient") {
            client()
            systemProperty("forge.logging.console.level", "debug")
            gameDirectory.set(file("run/client"))
        }
        register("neoforgeServer") {
            server()
            systemProperty("forge.logging.console.level", "debug")
            gameDirectory.set(file("run/server"))
            programArguments.addAll("nogui")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
            sourceSet(commonProject.sourceSets.main.get())
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
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
        register<MavenPublication>("neoforgeJar") {
            artifactId = base.archivesName.get()
            artifact(tasks.jar)
            artifact(sourcesJarTask)
        }
    }
}
