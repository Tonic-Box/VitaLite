# Command Line Reference

Complete reference for VitaLite command line options.

## Synopsis

```
java -jar vitalite.jar [OPTIONS]
```

Or when running from source:

```
./gradlew run --args="[OPTIONS]"
```

---

## Options

### -runInjector

Run the bytecode injector on startup and generate patch diffs.

**Type:** Boolean flag

**Use case:** Mixin development. When enabled, VitaLite runs the full ASM injection pipeline and generates `patches.zip` in `src/main/resources`.

**Example:**
```bash
java -jar vitalite.jar -runInjector
```

Source: `src/main/java/com/tonic/vitalite/Main.java:77-91`

---

### --rsdump

Dump the gamepack to the specified path.

**Type:** String (file path)

**Example:**
```bash
java -jar vitalite.jar --rsdump /path/to/dump.jar
```

---

### -noPlugins

Disable loading of core VitaLite plugins.

**Type:** Boolean flag

**Example:**
```bash
java -jar vitalite.jar -noPlugins
```

Source: `src/main/java/com/tonic/vitalite/Main.java:145-148`

---

### -min

Run with minimal memory allocation. Also disables plugins and music.

**Type:** Boolean flag

**Example:**
```bash
java -jar vitalite.jar -min
```

Source: `src/main/java/com/tonic/vitalite/Main.java:145-148`

---

### -noMusic

Prevent loading of music tracks.

**Type:** Boolean flag

**Example:**
```bash
java -jar vitalite.jar -noMusic
```

Source: `src/main/java/com/tonic/VitaLiteOptions.java`

---

### -incognito

Display as 'RuneLite' instead of 'VitaLite' in the title bar and UI.

**Type:** Boolean flag

**Example:**
```bash
java -jar vitalite.jar -incognito
```

---

### -help

Display help information about command line options.

**Type:** Boolean flag

**Example:**
```bash
java -jar vitalite.jar -help
```

---

### --legacyLogin

Provide legacy login credentials for automatic login.

**Type:** String

**Format:** `username:password`

**Example:**
```bash
java -jar vitalite.jar --legacyLogin "myuser:mypass"
```

Source: `src/main/java/com/tonic/vitalite/Main.java:54-57`

---

### --jagexLogin

Provide Jagex account login details for automatic login.

**Type:** String

**Format:** `sessionID:characterID:displayName` OR path to RuneLite credentials file

**Examples:**
```bash
# Direct credentials
java -jar vitalite.jar --jagexLogin "abc123:char456:MyName"

# Using credentials file
java -jar vitalite.jar --jagexLogin "/path/to/credentials.json"
```

Source: `src/main/java/com/tonic/vitalite/Main.java:58-61`

---

### --proxy

Set a proxy server to use for all connections.

**Type:** String

**Formats:**
- `ip:port` - No authentication
- `ip:port:username:password` - With authentication

**Behavior:**
- Sets JVM-wide SOCKS proxy
- Proxy address displayed in client title bar (since v1.12.12.1_2)

**Examples:**
```bash
# Without authentication
java -jar vitalite.jar --proxy "192.168.1.100:1080"

# With authentication
java -jar vitalite.jar --proxy "192.168.1.100:1080:user:pass"
```

Source: `src/main/java/com/tonic/vitalite/Main.java:50-53`, `base-api/src/main/java/com/tonic/services/proxy/ProxyManager.java`

---

### --world

Set a specific game world to log into.

**Type:** Integer (world number)

**Example:**
```bash
java -jar vitalite.jar --world 301
```

Source: `src/main/java/com/tonic/vitalite/Main.java:66-69`

---

### -disableMouseHook

Disable RuneLite's mousehook rlicn DLL from being loaded or called.

**Type:** Boolean flag

**Example:**
```bash
java -jar vitalite.jar -disableMouseHook
```

---

### --targetBootstrap

Force a specific RuneLite version.

**Type:** String (version string)

**Behavior:** Sets the system property `forced.runelite.version`.

**Example:**
```bash
java -jar vitalite.jar --targetBootstrap "1.10.0"
```

Source: `src/main/java/com/tonic/vitalite/Main.java:62-65`

---

### --port

Internal use - port for launcher communication.

**Type:** String (port number)

Source: `src/main/java/com/tonic/vitalite/Main.java:98-101`

---

## Combining Options

Options can be combined:

```bash
# Run with proxy, specific world, and incognito
java -jar vitalite.jar --proxy "10.0.0.1:1080" --world 420 -incognito

# Development mode with injector and no plugins
java -jar vitalite.jar -runInjector -noPlugins

# Minimal mode for testing
java -jar vitalite.jar -min -noMusic
```

---

## Common Mistakes

### Incorrect proxy format

**Wrong:**
```bash
--proxy "http://192.168.1.100:1080"  # Don't include protocol
--proxy "192.168.1.100"              # Missing port
```

**Correct:**
```bash
--proxy "192.168.1.100:1080"
```

### Using = instead of space

**Wrong:**
```bash
--world=301
--proxy="192.168.1.100:1080"
```

**Correct:**
```bash
--world 301
--proxy "192.168.1.100:1080"
```

### Forgetting quotes around credentials

**Wrong:**
```bash
--legacyLogin user:pass with spaces  # Will parse incorrectly
```

**Correct:**
```bash
--legacyLogin "user:pass with spaces"
```
