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

val dependencyProjects: List<Project> = listOf(
    project(":common")
)

dependencyProjects.forEach {
    project.evaluationDependsOn(it.path)
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
    // Cloth Config
    // runtimeOnly("me.shedaniel.cloth:cloth-config-neoforge:$clothConfigVersion")

    dependencyProjects.forEach {
        implementation(it)
    }
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
            for (dependencyProject in dependencyProjects) {
                sourceSet(dependencyProject.sourceSets.main.get())
            }
        }
    }
}

tasks.jar {
    from(sourceSets.main.get().output)
    for (p in dependencyProjects) {
        from(p.sourceSets.main.get().output)
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val sourcesJarTask = tasks.named<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
    for (p in dependencyProjects) {
        from(p.sourceSets.main.get().allSource)
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("sources")
}

artifacts {
    archives(tasks.jar.get())
    archives(sourcesJarTask)
}

publishing {
    publications {
        register<MavenPublication>("neoforgeJar") {
            artifactId = base.archivesName.get()
            artifact(tasks.jar.get())
            artifact(sourcesJarTask)
        }
    }
}
