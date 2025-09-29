import groovy.json.JsonSlurper
import java.net.URL

plugins {
    id("java")
}

group = "com.tonic"
version = rootProject.version

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.runelite.net")
        content {
            includeGroupByRegex("net\\.runelite.*")
        }
    }
    mavenCentral()
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
val runeLiteVersion = "latest.release"

dependencies {
    compileOnly(project(":base-api"))
    compileOnly("net.runelite:client:$runeLiteVersion")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    compileOnly("net.sf.trove4j:trove4j:3.0.3")
    compileOnly("it.unimi.dsi:fastutil:8.5.11")

    compileOnly(group = "com.google.code.findbugs", name = "jsr305", version = "3.0.2")
    compileOnly(group = "com.fifesoft", name = "rsyntaxtextarea", version = "3.1.2")
    compileOnly(group = "com.fifesoft", name = "autocomplete", version = "3.1.1")
    implementation("com.google.inject:guice:${runeliteVersions["guice"]}:no_aop")
    implementation("org.slf4j:slf4j-api:${runeliteVersions["slf4j"]}")
}

tasks.test {
    useJUnitPlatform()
}