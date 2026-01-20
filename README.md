# VitaLite

VitaLite is a launcher and SDK for RuneLite that provides additional features, game pack functionality access, and a comprehensive API for plugin development.

![VitaLite Screenshot](img.png)

## Features

- Access to additional GamePack functionalities
- Robust built-in SDK for plugin development
- Built-in plugins including Profiles for Jagex Account management
- Dual-layered mixin system for modifying RuneLite and GamePack classes
- External plugin side-loading support
- Advanced pathfinding and world walker
- Trajectory-based mouse movement system
- Inter-process communication between clients
- JVM profiler with flame graphs and leak detection

## Quick Start

### For Users

Download the latest release from the [VitaLite Launcher](https://github.com/Tonic-Box/VitaLauncher/releases).

### For Developers

**Requirements:** JDK 11

1. Clone and enter the repository:
   ```bash
   git clone https://github.com/Tonic-Box/VitaLite.git
   cd VitaLite
   ```

2. Sync the RuneLite API (required once per rev update):
   ```bash
   ./gradlew SyncRuneliteApi
   ```

3. Build all artifacts:
   ```bash
   ./gradlew buildAndPublishAll
   ```

4. Run the client:
   ```bash
   ./gradlew run
   ```

## Documentation

Full documentation is available in the [docs/](./docs/) directory:

| Document | Description |
|----------|-------------|
| [Documentation Index](./docs/index.md) | Main documentation hub |
| [Getting Started](./docs/getting-started.md) | Installation and setup guide |
| [Configuration](./docs/configuration.md) | All configuration options |
| [CLI Reference](./docs/cli-reference.md) | Command line arguments |
| [SDK Overview](./docs/SDK-DOCS.md) | API structure overview |
| [Plugin Development](./docs/EXTERNALPLUGIN.md) | External plugin guide |
| [Common Mistakes](./docs/common-mistakes.md) | Frequent errors and fixes |

### API Reference

| Document | Description |
|----------|-------------|
| [Query System](./docs/api/queries.md) | NPC, Item, Object queries |
| [Interactions](./docs/api/interactions.md) | Click and dialogue handling |
| [Pathfinding](./docs/api/pathfinding.md) | Walker and navigation |
| [IPC](./docs/api/ipc.md) | Inter-client communication |
| [Static](./docs/api/static.md) | Core access class |
| [Logger](./docs/api/logger.md) | Logging utilities |

### Feature Documentation

| Document | Description |
|----------|-------------|
| [Click Manager](./docs/CLICKMANAGER.md) | Click strategy configuration |
| [Mouse Movement](./docs/MOUSE-MOVEMENT.md) | Trajectory-based movement |
| [Script DSL](./docs/SCRIPT-DSL.md) | Coroutine-style scripting |
| [Profiler](./docs/PROFILER.md) | JVM profiling tools |
| [Features](./docs/FEATURES.md) | Built-in client features |

## Command Line Options

| Option | Type | Description |
|--------|------|-------------|
| `-runInjector` | Boolean | Run injector on startup (mixin development) |
| `--rsdump` | String | Path to dump gamepack |
| `-noPlugins` | Boolean | Disable core plugins |
| `-min` | Boolean | Minimal memory mode |
| `-noMusic` | Boolean | Disable music loading |
| `-incognito` | Boolean | Display as 'RuneLite' |
| `--legacyLogin` | String | Legacy login (user:pass) |
| `--jagexLogin` | String | Jagex login (sessionID:characterID:displayName) |
| `--proxy` | String | Proxy server (ip:port or ip:port:user:pass) |
| `--world` | Integer | Set login world |
| `-disableMouseHook` | Boolean | Disable mousehook DLL |

See [CLI Reference](./docs/cli-reference.md) for complete details.

## Side-Loading Plugins

Place external plugin JARs in `~/.runelite/sideloaded-plugins/` for automatic loading.

See [Plugin Development](./docs/EXTERNALPLUGIN.md) for creating plugins.

## Plugin Development Quick Start

Add dependencies to your `build.gradle.kts`:

```kotlin
val apiVersion = "latest.release"

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.runelite.net")
        content { includeGroupByRegex("net\\.runelite.*") }
    }
    mavenCentral()
}

dependencies {
    compileOnly("net.runelite:client:$apiVersion")
    compileOnly("com.tonic:base-api:$apiVersion")
    compileOnly("com.tonic:api:$apiVersion")
}
```

Create a basic plugin:

```java
@PluginDescriptor(name = "My Plugin", description = "Description")
public class MyPlugin extends VitaPlugin {
    @Override
    public void loop() throws Exception {
        // Called each game tick - safe to sleep/block
        NpcEx target = new NpcQuery()
            .withName("Goblin")
            .withinDistance(10)
            .nearest();

        if (target != null) {
            NpcAPI.interact(target, "Attack");
            Delays.waitUntil(() -> target.isDead(), 5000);
        }
    }
}
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Support

- [GitHub Issues](https://github.com/Tonic-Box/VitaLite/issues) - Bug reports and feature requests
- [Documentation](./docs/index.md) - Full SDK documentation

## Disclaimer

VitaLite is a third-party loader for RuneLite. Use at your own risk. The developers are not responsible for any consequences resulting from the use of this software.

## License

See [LICENSE](./LICENSE) for details.

---

[Buy me a coffee](https://ko-fi.com/tonicbox)
