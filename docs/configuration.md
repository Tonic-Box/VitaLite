# Configuration Reference

This document covers all configurable options in VitaLite.

## Configuration Storage

Configuration files are stored in `~/.runelite/vitalite/`. The `ConfigManager` class handles persistence.

Source: `base-api/src/main/java/com/tonic/services/ConfigManager.java`

---

## VitaLite Settings Panel

Access via the VitaLite configuration panel in the client sidebar.

### Click Strategy

Controls how automated clicks are generated.

| Strategy | Description |
|----------|-------------|
| `STATIC` | Clicks at a fixed point (configurable X/Y) |
| `RANDOM` | Clicks at random points within the viewport |
| `CONTROLLED` | Clicks within a developer-defined shape/area |

When set to `STATIC`, a panel appears to configure the static X/Y coordinates.

When set to `CONTROLLED` but no click box is set by the plugin, falls back to `STATIC`.

Source: `base-api/src/main/java/com/tonic/services/ClickStrategy.java`, `base-api/src/main/java/com/tonic/services/ClickManager.java`

### Mouse Movement Spoofing

Toggle realistic mouse movement generation. When enabled, movements are generated using trajectory-based algorithms that learn from your manual play.

| Setting | Default | Description |
|---------|---------|-------------|
| Spoof Mouse Movement | false | Enable/disable movement generation |

See [Mouse Movement System](MOUSE-MOVEMENT.md) for detailed configuration.

Source: `base-api/src/main/java/com/tonic/services/ClickManager.java:310,362`

### Headless Mode

Toggle pseudo-headless mode to disable game rendering and save CPU.

| Setting | Default | Description |
|---------|---------|-------------|
| Headless Mode | false | Disable game rendering |

Note: Coming out of headless mode may cause the GPU plugin to shut off (known issue).

Source: `docs/FEATURES.md`

### Logging

| Setting | Default | Description |
|---------|---------|-------------|
| Log Packets | false | Log sent/received packets to console |
| Log Menu Options | false | Log menu interactions |

---

## Logger Configuration

The Logger class provides console output with colored messages.

### Log Levels

| Level | Color | Method |
|-------|-------|--------|
| Normal | Green | `Logger.norm()` |
| Info | Light Blue | `Logger.info()` |
| Warning | Yellow | `Logger.warn()` |
| Error | Red | `Logger.error()` |

### Logger Settings

| Setting | Default | Description |
|---------|---------|-------------|
| Max Messages | 50 | Maximum messages in console buffer |
| Console Height | 150px | Height of the console panel |
| Normal Enabled | true | Show normal log messages |
| Info Enabled | true | Show info messages |
| Warning Enabled | true | Show warning messages |
| Error Enabled | true | Show error messages |

### Programmatic Configuration

```java
// Toggle log levels
Logger.setNormal(true);
Logger.setInfo(true);
Logger.setWarning(true);
Logger.setError(true);

// Set message limit
Logger.setMaxMessages(100);

// Toggle visibility (persists across sessions since v1.12.11)
Logger.setLoggerVisible(true);
```

Source: `src/main/java/com/tonic/Logger.java`

---

## Walker Configuration

The Walker has static settings that can be modified at runtime.

### Run Toggle Settings

| Setting | Default | Description |
|---------|---------|-------------|
| toggleRunRange | 25-35 | Energy range to toggle run on |
| consumeStaminaRange | 50-60 | Energy range to consume stamina |

```java
// Customize walker behavior
Walker.Setting.toggleRunRange = new IntPair(20, 30);
Walker.Setting.consumeStaminaRange = new IntPair(40, 50);
```

Source: `api/src/main/java/com/tonic/services/pathfinder/Walker.java:62-68`

---

## Proxy Configuration

Proxy settings are applied at JVM startup via command line.

### Formats

```
ip:port                      # No authentication
ip:port:username:password    # With authentication
```

### Behavior

- Sets JVM-wide SOCKS proxy (since v1.12.12.1_0)
- Proxy address displayed in client title bar (since v1.12.12.1_2)
- Supports SOCKS5 proxies

Source: `base-api/src/main/java/com/tonic/services/proxy/ProxyManager.java`, `base-api/src/main/java/com/tonic/services/proxy/SocksProxyUtil.java`

---

## IPC Channel Configuration

Inter-process communication between clients.

### Builder Options

| Option | Default | Description |
|--------|---------|-------------|
| port | 5000 | Multicast UDP port |
| group | "230.0.0.1" | Multicast group address |
| ttl | 1 | Time-to-live for packets |
| networkInterface | null | Specific network interface |

### Example

```java
Channel channel = new Channel.Builder("MyClient")
    .port(5000)
    .group("230.0.0.1")
    .ttl(1)
    .build();
```

### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| BUFFER_SIZE | 65536 | Maximum packet size |
| DUPLICATE_WINDOW_MS | 5000 | Window for duplicate detection |

Source: `base-api/src/main/java/com/tonic/services/ipc/Channel.java`, `base-api/src/main/java/com/tonic/services/ipc/ChannelBuilder.java`

---

## ConfigManager API

For plugin developers to persist configuration.

### Creating a Config Manager

```java
// Initialize with config file name (stored in ~/.runelite/vitalite/)
ConfigManager config = new ConfigManager("myplugin");
```

### Setting and Getting Values

```java
// Set values
config.setProperty("key", "value");
config.setProperty("group", "key", "value");

// Get values
String value = config.getString("key");
int number = config.getIntOrDefault("count", 0);
boolean flag = config.getBooleanOrDefault("enabled", false);

// Check existence
if (config.hasProperty("key")) {
    // ...
}
```

### Ensuring Defaults

```java
// Ensure default values exist
config.ensure(Map.of(
    "setting1", "default1",
    "setting2", 100,
    "setting3", true
));
```

### Saving

```java
// Save configuration to disk
config.saveConfig();

// Reset to defaults
config.reset();
```

Source: `base-api/src/main/java/com/tonic/services/ConfigManager.java`

---

## Environment Variables

VitaLite checks for these system properties:

| Property | Description |
|----------|-------------|
| `forced.runelite.version` | Force specific RuneLite version |

Set via `--targetBootstrap` command line option.

Source: `src/main/java/com/tonic/vitalite/Main.java:62-65`
