import groovy.json.JsonSlurper
import java.net.URI
import java.net.URL

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
    id("maven-publish")
}

val vitaVersion by extra("1")
val runeliteVersion by extra("1.12.14.1")

group = "com.tonic"
version = runeliteVersion + "_" + vitaVersion

if (JavaVersion.current() != JavaVersion.VERSION_11) {
    throw GradleException("""
        
        Java 11 Required (Current: ${JavaVersion.current()})
        
        Fix in IntelliJ:
        1. Ctrl+Alt+S -> Build, Execution, Deployment -> Build Tools -> Gradle
        2. Gradle JVM -> Select JDK 11
        3. Apply -> OK
        4. File -> Reload Gradle Project
        
    """.trimIndent())
}

repositories {
    mavenCentral()
    maven {
        url = URI("https://repo.runelite.net")
    }
    maven {
        url = uri("https://maven.google.com")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "vitalite"
        }
    }
}

// Apply maven-publish to all subprojects
subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    group = "com.tonic"
    version = rootProject.version

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}

// Custom task to clean and publish everything
tasks.register("buildAndPublishAll") {
    description = "Cleans and publishes all projects to Maven Local"

    dependsOn(tasks.named("publishToMavenLocal"))
    subprojects.forEach {
        dependsOn(it.tasks.named("publishToMavenLocal"))
    }
}

tasks.register<Copy>("copySubmoduleJar") {
    dependsOn(":api:jar")
    from(project(":api").tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into("src/main/resources/com/tonic")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    rename {
        "api.jarData"
    }

    outputs.upToDateWhen { false }
}

tasks.register<Copy>("copySubmoduleJar2") {
    dependsOn(":plugins:jar")
    from(project(":plugins").tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into("src/main/resources/com/tonic")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    rename {
        "plugins.jarData"
    }

    outputs.upToDateWhen { false }
}

tasks.processResources {
    dependsOn("copySubmoduleJar")
    dependsOn("copySubmoduleJar2")
}

tasks {
    build {
        finalizedBy("shadowJar")
    }

    jar {
        manifest {
            attributes(mutableMapOf("Main-Class" to "com.tonic.VitaLite"))
        }
    }

    shadowJar {
        archiveClassifier.set("shaded")
        isZip64 = true

        manifest {
            attributes(
                "Main-Class" to "com.tonic.VitaLite",
                "Implementation-Version" to project.version,
                "Implementation-Title" to "VitaLite",
                "Implementation-Vendor" to "Tonic",
                "Multi-Release" to "true"
            )
        }

        mergeServiceFiles()

        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("module-info.class")

        // Dynamically exclude all constant classes in net/runelite/api and subpackages
        // These are compile-time constants that get inlined, so runtime doesn't need them
        exclude {
            val path = it.path

            // Check if it's anywhere in net/runelite/api/ or its subpackages
            val isInApiPackage = path.startsWith("net/runelite/api/") && path.endsWith(".class")

            // Whitelist: Classes that ARE needed at runtime (not just compile-time constants)
            val whitelist = setOf(
                "net/runelite/api/gameval/ItemID.class",
                "net/runelite/api/gameval/InterfaceID.class",
                "net/runelite/api/gameval/ObjectID.class",
                "net/runelite/api/gameval/ObjectID1.class"
            )

            // Exclude if it's in api package (any level) but NOT in whitelist
            isInApiPackage && path !in whitelist
        }

        transform(com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer::class.java) {
            resource = "META-INF/services/javax.swing.LookAndFeel"
        }

        transform(com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer::class.java) {
            resource = "META-INF/services/java.nio.file.spi.FileSystemProvider"
        }
    }
}

fun getRuneLiteArtifacts(): Map<String, String> {
    val json = URL("https://static.runelite.net/bootstrap.json").readText()
    val jsonSlurper = JsonSlurper()
    val bootstrap = jsonSlurper.parseText(json) as Map<*, *>
    val artifacts = bootstrap["artifacts"] as List<Map<*, *>>

    val versions = mutableMapOf<String, String>()

    artifacts.forEach { artifact ->
        val name = artifact["name"] as String

        when {
            name.startsWith("guava-") -> {
                val version = name.removePrefix("guava-").removeSuffix(".jar")
                versions["guava"] = version
            }
            name.startsWith("guice-") -> {
                val version = name.removePrefix("guice-").removeSuffix("-no_aop.jar")
                versions["guice"] = version
            }
            name.startsWith("javax.inject-") -> {
                versions["javax.inject"] = "1"
            }
            name.startsWith("slf4j-api-") -> {
                val version = name.removePrefix("slf4j-api-").removeSuffix(".jar")
                versions["slf4j"] = version
            }
            name.startsWith("logback-core-") -> {
                val version = name.removePrefix("logback-core-").removeSuffix(".jar")
                versions["logback.core"] = version
            }
            name.startsWith("logback-classic-") -> {
                val version = name.removePrefix("logback-classic-").removeSuffix(".jar")
                versions["logback.classic"] = version
            }
        }
    }

    return versions
}

val runeliteVersions by lazy { getRuneLiteArtifacts() }

dependencies {
    compileOnly("net.runelite:runelite-api:$runeliteVersion")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    compileOnly("org.jetbrains:annotations:24.1.0")
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")

    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-util:9.2")
    implementation("org.ow2.asm:asm-commons:9.2")

    implementation("com.google.code.gson:gson:2.8.9")

    implementation(project(":base-api"))

    implementation("net.sf.trove4j:trove4j:3.0.3")
    implementation("it.unimi.dsi:fastutil:8.5.11")

    implementation("com.google.guava:guava:${runeliteVersions["guava"]}")
    implementation("com.google.inject:guice:${runeliteVersions["guice"]}:no_aop")
    implementation("javax.inject:javax.inject:1")

    implementation("org.slf4j:slf4j-api:${runeliteVersions["slf4j"]}")
    implementation("ch.qos.logback:logback-core:${runeliteVersions["logback.core"]}")
    implementation("ch.qos.logback:logback-classic:${runeliteVersions["logback.classic"]}")
    implementation("org.apache.commons:commons-collections4:4.1")

    implementation("org.jboss.aerogear:aerogear-otp-java:1.0.0")
    implementation("com.apple:AppleJavaExtensions:1.4")

    implementation(group = "com.fifesoft", name = "rsyntaxtextarea", version = "3.1.2")
    implementation(group = "com.fifesoft", name = "autocomplete", version = "3.1.1")
    implementation("io.sigpipe:jbsdiff:1.0")

    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.5")
}

tasks.test {
    useJUnitPlatform()
}

// Release-specific shadow jar with additional exclusions
tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJarRelease") {
    group = "release-pipeline"
    dependsOn("copySubmoduleJar", "copySubmoduleJar2")

    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())

    archiveBaseName.set("VitaLite")
    archiveClassifier.set("release-shaded")
    isZip64 = true

    manifest {
        attributes(
            "Main-Class" to "com.tonic.VitaLite",
            "Implementation-Version" to project.version,
            "Implementation-Title" to "VitaLite",
            "Implementation-Vendor" to "Tonic",
            "Multi-Release" to "true"
        )
    }

    mergeServiceFiles()

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("module-info.class")

    // Existing API package exclusions
    exclude {
        val path = it.path
        val isInApiPackage = path.startsWith("net/runelite/api/") && path.endsWith(".class")
        val whitelist = setOf(
            "net/runelite/api/gameval/ItemID.class",
            "net/runelite/api/gameval/InterfaceID.class",
            "net/runelite/api/gameval/ObjectID.class",
            "net/runelite/api/gameval/ObjectID1.class"
        )
        isInApiPackage && path !in whitelist
    }

    exclude("com/tonic/services/profiler/**")
    exclude("com/tonic/services/pathfinder/ui/**")
    exclude("com/tonic/injector/**")
    exclude("com/tonic/mixin/**")
    exclude("com/tonic/rlmixin/**")
    exclude("**/mappings.json")

    transform(com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer::class.java) {
        resource = "META-INF/services/javax.swing.LookAndFeel"
    }

    transform(com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer::class.java) {
        resource = "META-INF/services/java.nio.file.spi.FileSystemProvider"
    }
}

// Package release using the release shadow jar
tasks.register<Zip>("packageRelease") {
    group = "release-pipeline"
    dependsOn("shadowJarRelease")

    val shadowJarTask = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJarRelease")

    archiveBaseName.set("VitaLite")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    from(shadowJarTask.flatMap { it.archiveFile }) {
        rename { "VitaLite.jar" }
    }

    // Unix shell scripts - convert to LF line endings for Linux/Mac compatibility
    from("scripts") {
        include("run-linux.sh")
        include("run-mac.sh")
        filter(mapOf("eol" to org.apache.tools.ant.filters.FixCrLfFilter.CrLf.newInstance("lf")),
            org.apache.tools.ant.filters.FixCrLfFilter::class.java)
    }

    // Windows batch file - keep as-is (CRLF is fine for Windows)
    from("scripts") {
        include("run-windows.bat")
    }
}

// Build release: publish all + package release
tasks.register("buildRelease") {
    group = "release-pipeline"
    description = "Builds and publishes all projects, then creates release package"

    dependsOn(tasks.named("publishToMavenLocal"))
    subprojects.forEach {
        dependsOn(it.tasks.named("publishToMavenLocal"))
    }

    finalizedBy("packageRelease")
}

tasks.register<Exec>("publishRelease") {
    group = "release-pipeline"
    description = "Creates a GitHub release with the packaged zip"
    dependsOn("buildRelease")

    val tag = "${runeliteVersion}_${vitaVersion}"
    val title = "${tag}-bug fix"
    val body = "# ${tag}\n" +
            "- Fixed issue with game val updates and dif patches"
    val zipFile = layout.buildDirectory.file("libs/VitaLite-${project.version}.zip").get().asFile

    doFirst {
        if (!zipFile.exists()) {
            throw GradleException("Release zip not found: ${zipFile.absolutePath}")
        }
    }

    commandLine(
        "C:\\Program Files\\GitHub CLI\\gh.exe", "release", "create", tag,
        zipFile.absolutePath,
        "--title", title,
        "--notes", body
    )
}