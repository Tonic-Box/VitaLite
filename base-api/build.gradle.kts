import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URL

plugins {
    id("java")
    id("maven-publish")
}

group = "com.tonic"
version = rootProject.version

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.runelite.net")
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.20")
    compileOnly("com.intellij:annotations:12.0")          // org.intellij.lang.annotations.MagicConstant :contentReference[oaicite:0]{index=0}
    compileOnly("org.jetbrains:annotations:26.0.2")
    implementation("io.netty:netty-all:5.0.0.Alpha2")
    implementation("com.google.code.gson:gson:2.8.9")
    compileOnly("com.google.inject:guice:5.1.0")
    implementation(group = "com.fifesoft", name = "rsyntaxtextarea", version = "3.1.2")
    implementation(group = "com.fifesoft", name = "autocomplete", version = "3.1.1")
    implementation("org.antlr:antlr4:4.13.1")
    implementation("com.github.vlsi.mxgraph:jgraphx:4.2.2")
}

val apiFilePatterns = mapOf(
    "" to Regex("""
    AnimationID\.java|
    CollisionDataFlag\.java|
    EnumID\.java|
    FontID\.java|
    GraphicID\.java|
    HintArrowType\.java|
    HitsplatID\.java|
    ItemID\.java|
    KeyCode\.java|
    NpcID\.java|
    NullItemID\.java|
    NullNpcID\.java|
    NullObjectID\.java|
    ObjectID\.java|
    ObjectID1\.java|
    Opcodes\.java|
    ParamID\.java|
    SettingID\.java|
    SkullIcon\.java|
    SoundEffectID\.java|
    SoundEffectVolume\.java|
    SpriteID\.java|
    StructID\.java|
    Varbits\.java|
    VarClientInt\.java|
    VarClientStr\.java|
    VarPlayer\.java
  """.trimIndent().replace("\n","") )
    ,
    "annotations" to Regex(".*\\.java"),
    "clan"        to Regex("ClanID\\.java"),
    "dbtable"     to Regex("DBTableID.*\\.java"),
    "widgets"     to Regex("""
    ComponentID\.java|
    InterfaceID\.java|
    ItemQuantityMode\.java|
    WidgetID.*\.java|
    WidgetModalMode\.java|
    WidgetModelType\.java|
    WidgetPositionMode\.java|
    WidgetSizeMode\.java|
    WidgetTextAlignment\.java|
    WidgetType\.java
  """.trimIndent().replace("\n","")),
    "gameval"     to Regex(".*\\.java")
)

tasks.register("syncRuneliteApi") {
    group = "runelite"
    description = "Download selected runelite-api sources from GitHub raw and overwrite local files"

    doLast {
        val jsonSlurper = JsonSlurper()
        val apiBaseUrl = "https://api.github.com/repos/runelite/runelite/contents/" +
                "runelite-api/src/main/java/net/runelite/api"

        apiFilePatterns.forEach { (subdir, pattern) ->
            // build the API endpoint URL for this subdirectory
            val dirUrl = if (subdir.isEmpty()) {
                "$apiBaseUrl?ref=master"
            } else {
                "$apiBaseUrl/$subdir?ref=master"
            }

            // fetch directory listing as JSON
            val conn = URL(dirUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connect()
            val entries = jsonSlurper.parse(conn.inputStream) as List<Map<String, Any>>

            entries
                // only files whose name matches our pattern
                .filter { it["type"] == "file" && pattern.matches(it["name"] as String) }
                .forEach { entry ->
                    val name = entry["name"] as String
                    val downloadUrl = entry["download_url"] as String

                    // compute target file under src/main/java/net/runelite/api/...
                    val relativePath = buildList {
                        add("src")
                        add("main")
                        add("java")
                        add("net")
                        add("runelite")
                        add("api")
                        if (subdir.isNotEmpty()) add(subdir)
                        add(name)
                    }
                    val targetFile = file(projectDir.resolve(relativePath.joinToString(File.separator)))
                    targetFile.parentFile.mkdirs()

                    // download and write
                    URL(downloadUrl).openStream().use { input ->
                        targetFile.outputStream().use { out ->
                            input.copyTo(out)
                        }
                    }

                    println("Updated API source: ${relativePath.joinToString("/")}")
                }
        }
    }
}