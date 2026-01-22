# Configuration

VitaLite stores configuration in plain files under the VitaLite directory:

- VitaLite directory: `${user.home}/.runelite/vitalite`
  Source: `base-api/src/main/java/com/tonic/Static.java` (`VITA_DIR`)

## Config System Overview

The SDK uses proxy backed interfaces annotated with `@ConfigGroup` and `@ConfigKey`.
At runtime, a `ConfigManager` reads and writes a file (Apache Commons Configuration properties format) and the proxy handler maps getter and setter calls to config keys.

Source: `base-api/src/main/java/com/tonic/util/config/ConfigFactory.java:create`
Source: `base-api/src/main/java/com/tonic/util/config/ConfigProxyHandler.java:invoke`
Source: `base-api/src/main/java/com/tonic/services/ConfigManager.java`

## Client Config (`ClientConfig`)

`ClientConfig` is the main user facing VitaLite config interface.

- Config group name: `VitaLiteOptions`
  Source: `base-api/src/main/java/com/tonic/util/ClientConfig.java` (`@ConfigGroup`)
- Backing file: `${user.home}/.runelite/vitalite/VitaLiteOptions`
  Source: `base-api/src/main/java/com/tonic/util/config/ConfigFactory.java:create`
  Source: `base-api/src/main/java/com/tonic/services/ConfigManager.java:ConfigManager`

## VitaLite Settings Panel

Most users change settings through the VitaLite configuration panel in the client sidebar. These settings map to `ClientConfig` keys and are persisted to the backing file.

### Click Strategy

Controls how automated clicks are generated.

| Strategy | Description |
|----------|-------------|
| `STATIC` | Clicks at a fixed point (`clickPointX`/`clickPointY`). |
| `RANDOM` | Clicks at random points within the viewport. |
| `CONTROLLED` | Clicks within a plugin-provided click box; falls back to `STATIC` if no click box is available. |

Source: `base-api/src/main/java/com/tonic/services/ClickStrategy.java`
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java`

See `docs/CLICKMANAGER.md`.

### Mouse Movement Spoofing

When enabled, the click manager uses the mouse trajectory generator to move the cursor between clicks.

See `docs/MOUSE-MOVEMENT.md`.

### Headless Mode

Headless-related options are implemented as features and may be surfaced by built-in plugins.

See `docs/FEATURES.md`.

### Logger Panel

The embedded logger panel visibility and message history are controlled by `showLogger` and `logHistoryLimit`.

Source: `base-api/src/main/java/com/tonic/Logger.java`

### Keys and defaults

The table below lists the keys and defaults defined in code.
Source: `base-api/src/main/java/com/tonic/util/ClientConfig.java`

| Key | Type | Default | Meaning |
|-----|------|---------|---------|
| `clickStrategy` | enum `ClickStrategy` | `STATIC` | Click strategy used by the click manager. |
| `clickPointX` | int | `-1` | Static click X position. |
| `clickPointY` | int | `-1` | Static click Y position. |
| `cachedRandomDat` | boolean | `true` | Cache per account `random.dat` bytes. |
| `cachedDeviceID` | boolean | `true` | Cache per account device ID (UUID). |
| `cachedBank` | boolean | `true` | Persist bank cache across sessions. |
| `pathfinderImpl` | enum `PathfinderAlgo` | `HYBRID_BFS` | Pathfinder implementation. |
| `drawWalkerPath` | boolean | `true` | Draw the current Walker path overlays. |
| `drawCollision` | boolean | `false` | Draw collision overlays. |
| `drawInteractable` | boolean | `false` | Draw interactable overlays. |
| `logNames` | boolean | `true` | Enable name logging. |
| `neverLog` | boolean | `true` | Prevent AFK logout by blocking idle cycle updates. |
| `mouseMovements` | boolean | `false` | Enable synthetic mouse movement generation in the click manager. |
| `warning` | boolean | `false` | Tracks a warning acknowledgement state. |
| `visualizeMovements` | boolean | `false` | Enable mouse movement visualization overlays. |
| `visualizeClicks` | boolean | `false` | Enable click visualization overlays. |
| `logHistoryLimit` | int | `50` | UI log history limit. |
| `drawStratPath` | boolean | `false` | Draw strat path overlays. |
| `boatHull` | boolean | `false` | Draw boat hull overlay (sailing). |
| `boatDeck` | boolean | `false` | Draw boat deck overlay (sailing). |
| `boatDebug` | boolean | `false` | Draw additional boat debug overlay (sailing). |
| `headlessMapView` | boolean | `true` | Show headless map view when in headless mode. |
| `showLogger` | boolean | `true` | Show the embedded logger panel. |

## Mouse Trajectory Generator Config (`TrajectoryGeneratorConfig`)

Trajectory generation and training uses a separate config group:

- Config group name: `TrajectoryGenerator`
  Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/TrajectoryGeneratorConfig.java` (`@ConfigGroup`)
- Backing file: `${user.home}/.runelite/vitalite/TrajectoryGenerator`
  Source: `base-api/src/main/java/com/tonic/util/config/ConfigFactory.java:create`

The keys and defaults are defined here:
Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/TrajectoryGeneratorConfig.java`

## ConfigManager API (plugin storage)

For plugin developers that want to persist simple key/value state, `ConfigManager` provides a wrapper over an Apache Commons Configuration properties file.

### Create a ConfigManager

```java
ConfigManager config = new ConfigManager("myplugin");
```

### Read and write values

```java
config.setProperty("key", "value");
config.setProperty("group", "key", "value");

String value = config.getString("key");
int number = config.getIntOrDefault("count", 0);
boolean flag = config.getBooleanOrDefault("enabled", false);

if (config.hasProperty("key"))
{
    // ...
}
```

### Ensure defaults and save

```java
config.ensure(Map.of(
    "setting1", "default1",
    "setting2", 100,
    "setting3", true
));

config.saveConfig();
```

Source: `base-api/src/main/java/com/tonic/services/ConfigManager.java`

## Related CLI and System Properties

Some behavior is configured at launch time via command line options, not via `ClientConfig`.

- `--proxy <spec>` configures a JVM-wide SOCKS proxy.
- `--targetBootstrap <runeliteVersion>` sets the system property `forced.runelite.version`.

Proxy spec formats:

- `ip:port`
- `ip:port:username:password`

See `docs/CLI.md` and `docs/cli-reference.md`.

## Walker Runtime Settings

The walker exposes a few static tuning values (these are not persisted through `ClientConfig`):

| Setting | Default | Description |
|---------|---------|-------------|
| `toggleRunRange` | `25-35` | Energy range to toggle run on. |
| `consumeStaminaRange` | `50-60` | Energy range to consume stamina. |

Source: `api/src/main/java/com/tonic/services/pathfinder/Walker.java:Setting`

## IPC Channel Defaults

VitaLite uses a UDP multicast based IPC channel for inter-client communication. Defaults are defined in `ChannelBuilder`.

Source: `base-api/src/main/java/com/tonic/services/ipc/Channel.java`
Source: `base-api/src/main/java/com/tonic/services/ipc/ChannelBuilder.java`

See `docs/api/ipc.md`.

## Persistent Data Files

### Bank cache persistence

If enabled, bank cache snapshots are stored per player name in:

- Backing file: `${user.home}/.runelite/vitalite/CachedBanks`
  Source: `api/src/main/java/com/tonic/services/BankCache.java` (`new ConfigManager("CachedBanks")`)

### Cached RandomDat

Cached `random.dat` bytes are stored as Base64 by identifier in:

- Backing file: `${user.home}/.runelite/vitalite/CachedRandomDat`
  Source: `base-api/src/main/java/com/tonic/model/RandomDat.java`

### Cached Device ID

Cached device UUIDs are stored by identifier in:

- Backing file: `${user.home}/.runelite/vitalite/CachedUUID`
  Source: `base-api/src/main/java/com/tonic/model/DeviceID.java`

### Mouse trajectories data

Trajectory training data is serialized to:

- `${user.home}/.runelite/vitalite/data/trajectories.dat`
  Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/TrajectoryService.java` (`SAVE_PATH`)

### Enhanced Profiles database

Profiles are stored in an encrypted file:

- `${user.home}/.runelite/vitalite/profiles.db`
  Source: `plugins/src/main/java/com/tonic/plugins/profiles/session/ProfilesSession.java` (`DIRECTORY`)

The encryption key is derived from a device ID. Moving the file to another machine can make it undecryptable.
Source: `plugins/src/main/java/com/tonic/plugins/profiles/session/ProfilesSession.java:generateAESKey`
Source: `base-api/src/main/java/com/tonic/model/DeviceID.java:vanillaGetDeviceID`

