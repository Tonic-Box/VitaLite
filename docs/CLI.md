# Command Line Options

VitaLite parses command line options using a small reflection based parser.
Source: `base-api/src/main/java/com/tonic/util/optionsparser/OptionsParser.java:parse`

## Parsing Rules (Important)

VitaLite uses different prefixes depending on whether an option takes a value:

- Boolean flags use a single leading `-` and do not take a value.
  - Example: `-incognito`
- Value flags use a double leading `--` and require a separate value token.
  - Example: `--proxy 127.0.0.1:1080`

This is not a convention. It is required for correct parsing because the parser strips either 1 or 2 prefix characters based on whether it thinks a value is present.
Source: `base-api/src/main/java/com/tonic/util/optionsparser/OptionsParser.java:parse`

### Pass through arguments

Unknown arguments that start with `-` are passed through to RuneLite (they are returned from `parse()` and printed as `Passing to RL: ...`).
Source: `base-api/src/main/java/com/tonic/util/optionsparser/OptionsParser.java:parse`

## Options

The options below are defined in `VitaLiteOptions`.
Source: `base-api/src/main/java/com/tonic/VitaLiteOptions.java`

### Boolean flags (use `-name`)

| Flag | Default | Notes |
|------|---------|-------|
| `-noPlugins` | `false` | Disables loading of core plugins. Used by `Main.isMinMode()`. Source: `src/main/java/com/tonic/vitalite/Main.java:isMinMode` |
| `-incognito` | `false` | Avoids VitaLite UI branding patches and enables different plugin loading behavior. Source: `src/main/java/com/tonic/rlmixins/SplashScreenMixin.java:constructorHook` |
| `-safeLaunch` | `false` | Internal flag. `com.tonic.vitalite.Main` exits if this is not set. Source: `src/main/java/com/tonic/vitalite/Main.java:main` |
| `-min` | `false` | Enables a minimal mode check used during plugin loading. Source: `src/main/java/com/tonic/vitalite/Main.java:isMinMode` |
| `-noMusic` | `false` | Used by mixins and injector logic to disable music related behavior. Source: `src/main/java/com/tonic/injector/OSGlobalMixin.java` |
| `-runInjector` | `false` | Runs the injector pipeline and writes `patches.zip` for mixin development. Source: `src/main/java/com/tonic/vitalite/Main.java:main` |
| `-disableMouseHook` | `false` | Disables the RuneLite mousehook loader method. Source: `src/main/java/com/tonic/mixins/TMouseHookMixin.java:mouseHookLoader` |
| `-help` | n/a | Special case. Prints help and exits. Source: `base-api/src/main/java/com/tonic/util/optionsparser/OptionsParser.java:parse` |

### Value flags (use `--name value`)

| Flag | Type | Default | Notes |
|------|------|---------|-------|
| `--rsdump <path>` | String | unset | Dumps the injected gamepack classes to a jar. Source: `src/main/java/com/tonic/util/JarDumper.java:dump` |
| `--proxy <spec>` | String | unset | Sets a JVM wide SOCKS proxy and verifies connectivity. Source: `base-api/src/main/java/com/tonic/services/proxy/SocksProxyUtil.java:setProxy` |
| `--legacyLogin <user:pass>` | String | unset | Sets legacy credentials for auto login. Source: `base-api/src/main/java/com/tonic/services/AutoLogin.java:setCredentials` |
| `--jagexLogin <sessionId:characterId:displayName or path>` | String | unset | Sets Jagex credentials for auto login, or loads them from a properties file. Source: `base-api/src/main/java/com/tonic/services/AutoLogin.java:setCredentials` |
| `--targetBootstrap <runeliteVersion>` | String | unset | Sets the `forced.runelite.version` system property. Source: `src/main/java/com/tonic/vitalite/Main.java:main` |
| `--world <id>` | int | `-1` | If `> 0`, requests a world change via `WorldSetter`. Source: `src/main/java/com/tonic/vitalite/Main.java:main` |
| `--launcherCom <port>` | String | unset | If set, sends a ready signal to the launcher. Source: `src/main/java/com/tonic/vitalite/Main.java:main` |
