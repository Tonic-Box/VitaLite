plugins {
    id("java")
}

group = "com.tonic.plugins"
version = "1.11.19.1"

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

val apiVersion = "latest.release"

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":base-api"))
    compileOnly("net.runelite:client:$apiVersion")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    compileOnly("org.jboss.aerogear:aerogear-otp-java:1.0.0")
    implementation(group = "com.fifesoft", name = "rsyntaxtextarea", version = "3.1.2")
    implementation(group = "com.fifesoft", name = "autocomplete", version = "3.1.1")
}

tasks.test {
    useJUnitPlatform()
}