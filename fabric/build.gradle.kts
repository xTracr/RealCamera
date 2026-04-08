plugins {
    id("net.fabricmc.fabric-loom")
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
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    // Cloth Config
    runtimeOnly("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion")
    // Modmenu
    implementation("com.terraformersmc:modmenu:$modMenuVersion") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    dependencyProjects.forEach {
        implementation(it)
    }
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
            for (p in dependencyProjects) {
                srcDir(p.sourceSets.main.get().resources)
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

artifacts {
    archives(tasks.jar)
}

publishing {
    publications {
        register<MavenPublication>("fabricJar") {
            artifactId = base.archivesName.get()
            artifact(tasks.jar)
        }
    }
}
