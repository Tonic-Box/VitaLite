# Troubleshooting

This page lists failure modes for VitaLite usage and SDK development, with the relevant code locations.

## Incorrect initialization

### Running `com.tonic.vitalite.Main` directly

`com.tonic.vitalite.Main` exits unless `-safeLaunch` is set.
Run `com.tonic.VitaLite` instead, which re launches `Main` with `-safeLaunch`.

Source: `src/main/java/com/tonic/vitalite/Main.java:main`
Source: `src/main/java/com/tonic/VitaLite.java:main`

## Missing configuration

### Expecting settings to appear under RuneLite plugin config

Some VitaLite settings are stored in VitaLite specific config files under `${user.home}/.runelite/vitalite`.
See `docs/CONFIGURATION.md`.
Source: `base-api/src/main/java/com/tonic/Static.java` (`VITA_DIR`)

## Invalid credentials

### `--legacyLogin` and `--jagexLogin` parsing

Credentials are split on `:`. If your values contain extra `:`, parsing breaks and the value can be treated as a file path.
Source: `base-api/src/main/java/com/tonic/services/AutoLogin.java:setCredentials`

### Profiles plugin Jagex OAuth server cannot bind port 80

The Jagex OAuth helper binds to port 80, which may require elevated privileges and can conflict with other software.
Source: `plugins/src/main/java/com/tonic/plugins/profiles/jagex/JagexHttpServer.java:start`

## Misuse of optional parameters

### `--world` default behavior

`--world` defaults to `-1`. World setting is only applied if the value is `> 0`.
Source: `src/main/java/com/tonic/vitalite/Main.java:main`

## Pagination misuse

The VitaLite SDK does not expose a generic pagination API in this repository.
If you are iterating query results, use the query builders and streams as provided by the relevant APIs.
Source: `api/src/main/java/com/tonic/queries/NpcQuery.java`

## Error handling omissions

### `VitaPlugin.loop()` exceptions

`VitaPlugin` catches exceptions thrown by `loop()` and logs them. If you swallow exceptions inside your plugin, you can hide root causes.
Source: `api/src/main/java/com/tonic/util/VitaPlugin.java:_onGameTick`

## Retry and timeout misconfiguration

Several network and background operations use fixed timeouts that are not configurable through public settings:

- GitHub update check uses a 30 second timeout.
  Source: `src/main/java/com/tonic/vitalite/SelfUpdate.java` (`TIMEOUT`)
- Jagex OAuth flow waits up to 2 minutes for a browser redirect.
  Source: `plugins/src/main/java/com/tonic/plugins/profiles/jagex/JagexAccountService.java:requestLoginToken`
- SOCKS proxy verification uses a 3 second socket connect timeout and 5 second HTTP timeouts.
  Source: `base-api/src/main/java/com/tonic/services/proxy/SocksProxyUtil.java:verify`

If you run in an environment without outbound network access, these operations can fail regardless of correct configuration.

## Incorrect environment setup

### Java version mismatch

The Gradle build requires Java 11.
Source: `build.gradle.kts` (JavaVersion check)

### Network access required for version lookup

RuneLite version lookup uses `https://static.runelite.net/bootstrap.json`.
Source: `base-api/src/main/java/com/tonic/util/RuneliteConfigUtil.java:getRuneLiteVersion`

### Proxy verification uses an external service

Proxy verification makes an HTTP request to `https://api.ipify.org`.
Source: `base-api/src/main/java/com/tonic/services/proxy/SocksProxyUtil.java:verify`
