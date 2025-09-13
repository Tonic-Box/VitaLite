import groovy.json.JsonSlurper
import java.net.URI
import java.net.URL

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
    id("maven-publish")
}

group = "com.tonic"
version = "1.11.16"

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
    version = "1.0-SNAPSHOT"

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                // artifactId defaults to the subproject name (api, remapper, utilities)
            }
        }
    }
}

// Custom task to clean and publish everything
tasks.register("buildAndPublishAll") {
    description = "Cleans and publishes all projects to Maven Local"

    dependsOn(tasks.publishToMavenLocal)
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

tasks.processResources {
    dependsOn("copySubmoduleJar")
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

val TaskContainer.publishToMavenLocal: TaskProvider<DefaultTask>
    get() = named<DefaultTask>("publishToMavenLocal")

dependencies {
    compileOnly("net.runelite:api:latest.release")

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

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("com.google.guava:guava:${runeliteVersions["guava"]}")
    implementation("com.google.inject:guice:${runeliteVersions["guice"]}:no_aop")
    implementation("javax.inject:javax.inject:1")

    implementation("org.slf4j:slf4j-api:${runeliteVersions["slf4j"]}")
    implementation("ch.qos.logback:logback-core:${runeliteVersions["logback.core"]}")
    implementation("ch.qos.logback:logback-classic:${runeliteVersions["logback.classic"]}")
}

tasks.test {
    useJUnitPlatform()
}