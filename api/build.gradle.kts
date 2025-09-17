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
}

tasks.test {
    useJUnitPlatform()
}