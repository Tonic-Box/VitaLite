# External Plugins and Sideloading

This page covers how VitaLite discovers external plugin jars, how to build against the VitaLite SDK, and how hot reload works.

## Where to put plugin jars

VitaLite scans for plugin jars in two directories under the RuneLite home directory:

- `${user.home}/.runelite/sideloaded-plugins`
- `${user.home}/.runelite/externalplugins`

Source: `api/src/main/java/com/tonic/services/hotswapper/PluginReloader.java:findJars`
Source: `base-api/src/main/java/com/tonic/Static.java` (`RUNELITE_DIR`)

## Building against the VitaLite SDK

VitaLite publishes and consumes two SDK modules:

- `com.tonic:base-api`
- `com.tonic:api`

Source: `settings.gradle.kts` (includes `base-api` and `api`)
Source: `build.gradle.kts` (publishing configuration)

### Recommended Gradle setup (Kotlin DSL)

This example assumes you published the VitaLite modules to Maven Local using `buildAndPublishAll`.
Source: `build.gradle.kts:buildAndPublishAll`

```kotlin
repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.runelite.net")
        content { includeGroupByRegex("net\\.runelite.*") }
    }
    mavenCentral()
}

dependencies {
    val vitaVersion = "<match your VitaLite version>"

    compileOnly("net.runelite:client:$vitaVersion")
    compileOnly("com.tonic:base-api:$vitaVersion")
    compileOnly("com.tonic:api:$vitaVersion")
}
```

The VitaLite version string is derived from `runeliteVersion + "_" + vitaVersion` in the root Gradle build.
Source: `build.gradle.kts` (`runeliteVersion`, `vitaVersion`, and `version = ...`)

## Plugin structure

External plugins are standard RuneLite plugins. At minimum, you need:

- A class that extends `net.runelite.client.plugins.Plugin`
- A `@PluginDescriptor` annotation

Source: `plugins/src/main/java/com/tonic/plugins/profiles/ProfilesPlugin.java` (built in example)

### Using `VitaPlugin` for looped automation

VitaLite provides an optional base class `com.tonic.util.VitaPlugin` that adds a `loop()` method invoked from `GameTick` events in a background thread.
Source: `api/src/main/java/com/tonic/util/VitaPlugin.java`

Example:

```java
import com.tonic.util.VitaPlugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
    name = "Sample VitaPlugin"
)
public class SamplePlugin extends VitaPlugin
{
    @Override
    public void loop() throws Exception
    {
        // Do not block the client thread.
        // Use Static.invoke(...) when you need to read or mutate client state safely.
    }
}
```

## Hot reload behavior

VitaLite includes a hot reload service that can reload an external plugin jar in place:

- It stops and removes existing plugin instances that came from the jar.
- It creates a new `PluginClassLoader` for the jar.
- It calls `PluginManager.loadPlugins(...)` with the new classes, loads configuration, and starts the plugins.

Source: `api/src/main/java/com/tonic/services/hotswapper/PluginReloader.java:reloadPlugin`

### UI integration

The client includes a UI panel that lists local plugin jars and provides a reload button.
Source: `api/src/main/java/com/tonic/ui/sdn/VitaExternalsPanel.java`

The plugin list UI can also display a cycle button for reloading, inserted into plugin list items.
Source: `api/src/main/java/com/tonic/services/hotswapper/PluginReloader.java:addRedButtonAfterPin`
