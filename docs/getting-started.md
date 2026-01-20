# Getting Started with VitaLite

This guide covers installation, first run, and basic configuration of VitaLite.

## Requirements

- **JDK 11** - Required for building and running
- **Gradle** - Included via wrapper (gradlew)
- **Git** - For cloning the repository

## Installation

### For Users (Pre-built Release)

Download the latest release from the [VitaLite Launcher](https://github.com/Tonic-Box/VitaLauncher/releases).

### For Developers (Building from Source)

1. Clone the repository:
   ```bash
   git clone https://github.com/Tonic-Box/VitaLite.git
   cd VitaLite
   ```

2. Run the RuneLite API sync task (required once per rev update and on first build):
   ```bash
   ./gradlew SyncRuneliteApi
   ```

3. Build and publish all artifacts:
   ```bash
   ./gradlew buildAndPublishAll
   ```

4. Run the client:
   ```bash
   ./gradlew run
   ```

   Or run the main class directly:
   ```
   com.tonic.vitalite.Main
   ```

Source: `README.md`, `build.gradle.kts`

---

## Directory Structure

VitaLite uses the following directories:

| Directory | Purpose |
|-----------|---------|
| `~/.runelite/` | RuneLite base directory |
| `~/.runelite/vitalite/` | VitaLite configuration files |
| `~/.runelite/sideloaded-plugins/` | External plugin JARs |

Source: `src/main/java/com/tonic/Static.java`

---

## Command Line Options

VitaLite supports the following command line options:

| Option | Type | Description |
|--------|------|-------------|
| `-runInjector` | Boolean | Run the injector on startup (for mixin development) |
| `--rsdump` | String | Path to dump the gamepack to |
| `-noPlugins` | Boolean | Disable loading of core plugins |
| `-min` | Boolean | Run with minimal memory allocation |
| `-noMusic` | Boolean | Prevent loading of music tracks |
| `-incognito` | Boolean | Display as 'RuneLite' instead of 'VitaLite' |
| `-help` | Boolean | Display help information |
| `--legacyLogin` | String | Legacy login credentials (user:pass) |
| `--jagexLogin` | String | Jagex login (sessionID:characterID:displayName or path to credentials file) |
| `--proxy` | String | Proxy server (ip:port or ip:port:username:password) |
| `-disableMouseHook` | Boolean | Disable RuneLite's mousehook DLL |
| `--world` | Integer | Set specific game world on login |
| `--targetBootstrap` | String | Force specific RuneLite version |

Source: `src/main/java/com/tonic/VitaLiteOptions.java`, `src/main/java/com/tonic/vitalite/Main.java:39-69`

### Example Usage

```bash
# Run with proxy
java -jar vitalite.jar --proxy 192.168.1.100:1080

# Run with authenticated proxy
java -jar vitalite.jar --proxy 192.168.1.100:1080:user:password

# Run in incognito mode with specific world
java -jar vitalite.jar -incognito --world 301

# Run with legacy login
java -jar vitalite.jar --legacyLogin "username:password"

# Run with Jagex account
java -jar vitalite.jar --jagexLogin "sessionId:characterId:DisplayName"
```

---

## First Run

On first launch, VitaLite will:

1. Create necessary directories under `~/.runelite/vitalite/`
2. Download and sync the RuneLite API if not already present
3. Apply bytecode patches (or generate them in `-runInjector` mode)
4. Launch the modified RuneLite client

### Common First-Run Issues

**Issue: "Safe launch not satisfied"**
- Cause: Security check failed
- Fix: Ensure you have write permissions to the VitaLite directories

**Issue: Missing dependencies**
- Cause: API sync not run
- Fix: Run `./gradlew SyncRuneliteApi` before building

**Issue: Class not found errors**
- Cause: Build artifacts missing
- Fix: Run `./gradlew buildAndPublishAll` to rebuild all modules

---

## Configuration

VitaLite settings are accessible through the client's configuration panel under the VitaLite section.

Key configuration areas:
- **Click Strategy** - Control how automated clicks are generated
- **Mouse Movement** - Configure trajectory-based mouse movement
- **Headless Mode** - Toggle rendering for performance
- **Logging** - Enable packet and menu option logging

See [Configuration Reference](configuration.md) for detailed options.

---

## Side-Loading Plugins

To load external plugins:

1. Build your plugin JAR
2. Place it in `~/.runelite/sideloaded-plugins/`
3. Restart VitaLite

Plugins will be automatically loaded on startup.

See [External Plugin Development](EXTERNALPLUGIN.md) for plugin development guide.

---

## Next Steps

- [SDK Overview](SDK-DOCS.md) - Learn about the API structure
- [External Plugin Development](EXTERNALPLUGIN.md) - Start building plugins
- [Query System](api/queries.md) - Learn to search for game entities
