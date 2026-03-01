plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":base-api"))
    implementation(project(":api"))
    
    // HTTP server (Javalin - lightweight REST framework)
    implementation("io.javalin:javalin:5.6.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("org.eclipse.jetty.toolchain:jetty-jakarta-servlet-api:5.0.2")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
