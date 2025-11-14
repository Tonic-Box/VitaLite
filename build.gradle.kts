import groovy.json.JsonSlurper
import java.net.URI
import java.net.URL

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
    id("maven-publish")
}

val vitaVersion = "0"
val runeliteVersion = "1.12.4"

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

    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
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

//    implementation("com.lmax:disruptor:3.4.4")
//    implementation("org.jctools:jctools-core:4.0.5")
//    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
//    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
//    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
//    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2")
//    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
//    implementation("com.sun.mail:javax.mail:1.6.2")
//    implementation("org.osgi:org.osgi.framework:1.10.0")
//    implementation("org.conscrypt:conscrypt-openjdk-uber:2.5.2")
}

tasks.test {
    useJUnitPlatform()
}