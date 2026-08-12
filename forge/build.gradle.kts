buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("org.ow2.asm:asm:9.5") }
}

plugins {
    id("java-library")
    id("net.minecraftforge.gradle")
    id("net.minecraftforge.renamer")
    id("maven-publish")
}

val modId = project.property("mod_id") as String
val javaVersion = project.property("java_version") as String
val minecraftVersion = project.property("minecraft_version") as String
val forgeVersion = project.property("forge_version") as String

val clothConfigVersion = project.property("cloth_config_version") as String

val mapFile = file("libs/mappings_official-1.20.1-20230612.114412-map2srg.tsrg.gz")
renamer.mappings.setFrom(mapFile)

base {
    archivesName = "$modId-$minecraftVersion-forge"
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
    implementation(minecraft.dependency("net.minecraftforge:forge:$forgeVersion"))
    compileOnly("org.spongepowered:mixin:0.8.5")
    compileOnly("io.github.llamalad7:mixinextras-forge:0.5.0")
    annotationProcessor("io.github.llamalad7:mixinextras-forge:0.5.0")
    
    // Cloth Config
    implementation(renamer.dependency("me.shedaniel.cloth:cloth-config-forge:$clothConfigVersion"))

    implementation(commonProject.sourceSets.main.get().output)
}

configurations.named("annotationProcessor") {
    extendsFrom(configurations.named("implementation").get())
}

// Merge mappings from TSRG into the refmap.
// Multi-module Mixin AP combined with Forge's SRG mappings necessitates these workarounds.
// If FG7 resolves these issues in the future, they may no longer be needed
tasks.named("compileJava", JavaCompile::class).configure {
    doLast {
        val tsrg = layout.buildDirectory.file("tmp/compileJava/compileJava-mappings.tsrg").get().asFile
        val refmap = layout.buildDirectory.file("tmp/compileJava/compileJava-refmap.json").get().asFile
        if (!tsrg.exists() || !refmap.exists()) return@doLast
        val lines = tsrg.readLines().map { it.trim().split(" ") }
        val fieldsByClass = mutableMapOf<String, MutableMap<String, String>>()
        lines.filter { it.size == 3 && "(" !in it.joinToString() }
            .forEach { fieldsByClass.getOrPut(it[0]) { mutableMapOf() }[it[1]] = it[2] }
        val methodsByClass = mutableMapOf<String, MutableMap<String, String>>()
        lines.filter { it.size == 4 }
            .forEach { methodsByClass.getOrPut(it[0]) { mutableMapOf() }[it[1]] = it[3] }
        if (fieldsByClass.isEmpty() && methodsByClass.isEmpty()) return@doLast
        var text = refmap.readText()
        val allClasses = (fieldsByClass.keys + methodsByClass.keys).toSet()

        fun buildInsert(fields: Map<String, String>, methods: Map<String, String>): String {
            val entries = mutableListOf<String>()
            fields.forEach { (name, srg) -> entries.add(""""$name": "$srg"""") }
            methods.forEach { (name, srg) -> entries.add(""""$name": "$srg"""") }
            return entries.joinToString(",\n")
        }

        allClasses.forEach { cls ->
            val fields = fieldsByClass[cls] ?: emptyMap()
            val methods = methodsByClass[cls] ?: emptyMap()
            val insert = buildInsert(fields, methods)
            if (insert.isEmpty()) return@forEach
            text = text.replace(Regex(""""\Q$cls\E":\s*\{""")) { "${it.value}\n$insert," }
        }

        val seargeKey = """"searge": {"""
        val seargeIdx = text.indexOf(seargeKey)
        if (seargeIdx >= 0) {
            allClasses.forEach { cls ->
                val seargeSection = text.substring(seargeIdx)
                if (!seargeSection.contains(""""$cls":""")) {
                    val fields = fieldsByClass[cls] ?: emptyMap()
                    val methods = methodsByClass[cls] ?: emptyMap()
                    val entries = mutableListOf<String>()
                    fields.forEach { (name, srg) -> entries.add(""""$name": "$srg"""") }
                    methods.forEach { (name, srg) -> entries.add(""""$name": "$srg"""") }
                    if (entries.isNotEmpty()) {
                        val newEntry = """"$cls": {${entries.joinToString(",\n")}},"""
                        text = text.substring(0, seargeIdx + seargeKey.length) + newEntry + text.substring(seargeIdx + seargeKey.length)
                    }
                }
            }
        }
        refmap.writeText(text)        
    }
}

// Never use Forge for debugging modifications unless absolutely necessary!!!
minecraft {
    mavenizer(repositories)
    mappings("official", minecraftVersion)
    useDefaultAccessTransformer()
    accessTransformers.setFrom(file("src/main/resources/META-INF/accesstransformer.cfg"))
    
    runs {
        configureEach {
            workingDir.convention(layout.projectDirectory.dir("run"))
            //systemProperty("forge.logging.console.level", "debug")
            systemProperty("eventbus.api.strictRuntimeChecks", "true")
        }
        register("client") {
            systemProperty("forge.enabledGameTestNamespaces", modId)
            workingDir.convention(layout.projectDirectory.dir("run/client"))
        }
        register("server") {
            systemProperty("forge.enabledGameTestNamespaces", modId)
            workingDir.convention(layout.projectDirectory.dir("run/server"))
            args("--nogui")
        }
    }
}

sourceSets.named("main") {
    java { srcDir(commonProject.sourceSets.main.get().java.srcDirs) }
    resources { srcDir(commonProject.sourceSets.main.get().resources.srcDirs) }
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("META-INF/mods.toml") {
        expand("version" to project.version)
    }
}

tasks.named("build") {
    dependsOn(tasks.named("renameJar"))
}

tasks.named("assemble") {
    dependsOn(tasks.named("renameJar"))
}

tasks.named<Jar>("jar") {
    from(sourceSets.main.get().output)
    from(commonProject.sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    doFirst {
        // Same root cause as above — fixes the same category of issue, but not the same problem.
        // Rename @Shadow fields/methods in compiled classes to SRG names via ASM
        val tsrg = layout.buildDirectory.file("tmp/compileJava/compileJava-mappings.tsrg").get().asFile
        if (!tsrg.exists()) return@doFirst
        val lines = tsrg.readLines().map { it.trim().split(" ") }
        val fieldRenames = mutableMapOf<String, MutableMap<String, String>>()
        val methodRenames = mutableMapOf<String, MutableMap<String, MutableMap<String, String>>>()
        for (parts in lines) {
            when (parts.size) {
                3 if "(" !in parts.joinToString() -> fieldRenames.getOrPut(parts[0]) { mutableMapOf() }[parts[1]] = parts[2]
                4 -> methodRenames.getOrPut(parts[0]) { mutableMapOf() }
                        .getOrPut(parts[1]) { mutableMapOf() }[parts[2]] = parts[3]
            }
        }
        for (cls in (fieldRenames.keys + methodRenames.keys)) {
            val classFile = File(tasks.named<JavaCompile>("compileJava").get().destinationDirectory.get().asFile, "$cls.class")
            if (!classFile.exists()) continue
            val fMap = fieldRenames[cls] ?: emptyMap()
            val mMap = methodRenames[cls] ?: emptyMap()
            val cr = org.objectweb.asm.ClassReader(classFile.readBytes())
            val cw = org.objectweb.asm.ClassWriter(cr, org.objectweb.asm.ClassWriter.COMPUTE_MAXS)
            cr.accept(object : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9, cw) {
                override fun visitField(acc: Int, name: String, desc: String, sig: String?, value: Any?) =
                    super.visitField(acc, fMap[name] ?: name, desc, sig, value)
                override fun visitMethod(acc: Int, name: String, desc: String, sig: String?, exs: Array<out String>?): org.objectweb.asm.MethodVisitor {
                    val dMap = mMap[name] ?: emptyMap()
                    val mv = super.visitMethod(acc, dMap[desc] ?: name, desc, sig, exs)
                    return object : org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9, mv) {
                        override fun visitFieldInsn(op: Int, owner: String, name: String, desc: String) =
                            super.visitFieldInsn(op, owner, fMap[name] ?: name, desc)
                        override fun visitMethodInsn(op: Int, owner: String, name: String, desc: String, itf: Boolean) {
                            val dm = mMap[name] ?: emptyMap()
                            super.visitMethodInsn(op, owner, dm[desc] ?: name, desc, itf)
                        }
                    }
                }
            }, 0)
            classFile.writeBytes(cw.toByteArray())
        }
    }
}

renamer.classes(tasks.named("jar", Jar::class)) {
    archiveClassifier.set("renamed")
    accessTransformers.set(true)
}

tasks.named("renameJar") {
    doLast {
        // Replace the original jar with the renamed jar and delete the renamed file.
        val jarTask = tasks.named<Jar>("jar").get()
        val jarFile = jarTask.archiveFile.get().asFile
        val renamedFile = File(jarFile.parentFile, jarFile.name.replace(".jar", "-renamed.jar"))
        if (renamedFile.exists()) {
            renamedFile.copyTo(jarFile, overwrite = true)
            renamedFile.delete()
        }
    }
}

renamer.enableMixinRefmaps {
    config("realcamera-common.mixins.json")
    refMap.set("realcamera-common.refmap.json")
}

val sourcesJarTask = tasks.named<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
    from(commonProject.sourceSets.main.get().allSource)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("sources")
}

artifacts {
    archives(tasks.named("jar"))
    archives(sourcesJarTask)
}

publishing {
    publications {
        register<MavenPublication>("forgeJar") {
            artifactId = base.archivesName.get()
            artifact(tasks.named("jar"))
            artifact(sourcesJarTask)
        }
    }
}