# VitaLite Features

This page describes user visible VitaLite features that are backed by code in this repository.

## Built In Features

### Built in plugins

The following built in plugins are packaged into the VitaLite client:

- Authenticator Gen
  - Plugin: `# Authenticator Gen`
  - Source: `plugins/src/main/java/com/tonic/plugins/authgen/AuthGenPlugin.java` (`@PluginDescriptor`)
- Bank Valuer
  - Plugin: `# Bank Valuer`
  - Source: `plugins/src/main/java/com/tonic/plugins/bankvaluer/BankValuerPlugin.java` (`@PluginDescriptor`)
- Break Handler
  - Plugin: `# Break Handler`
  - Source: `plugins/src/main/java/com/tonic/plugins/breakhandler/BreakHandlerPlugin.java` (`@PluginDescriptor`)
- Code Eval
  - Plugin: `# Code Eval`
  - Source: `plugins/src/main/java/com/tonic/plugins/codeeval/CodeEvalPlugin.java` (`@PluginDescriptor`)
- Multi Client Util
  - Plugin: `# Multi-Client Util`
  - Source: `plugins/src/main/java/com/tonic/plugins/multiclientutils/MultiClientUtilPlugin.java` (`@PluginDescriptor`)
  - Note: This plugin extends `VitaPlugin`.
    Source: `plugins/src/main/java/com/tonic/plugins/multiclientutils/MultiClientUtilPlugin.java`
- Enhanced Profiles
  - Plugin: `# Enhanced Profiles`
  - Source: `plugins/src/main/java/com/tonic/plugins/profiles/ProfilesPlugin.java` (`@PluginDescriptor`)

### Enhanced Profiles (legacy and Jagex accounts)

- Built in plugin: `# Enhanced Profiles`
  Source: `plugins/src/main/java/com/tonic/plugins/profiles/ProfilesPlugin.java` (`@PluginDescriptor`)
- Stores profiles at `${user.home}/.runelite/vitalite/profiles.db`
  Source: `plugins/src/main/java/com/tonic/plugins/profiles/session/ProfilesSession.java` (`DIRECTORY`)

Related documentation: `docs/AUTHENTICATION.md`

### World map walker and path testing

The client adds menu entries like `Walk` and `Test Path` to the world map and uses the pathfinding services to compute and execute paths.
Source: `api/src/main/java/com/tonic/services/GameManager.java` (world map menu entry creation and `WalkerPath` usage)

### Cached RandomDat

When enabled, VitaLite writes and reuses cached `random.dat` data per account identifier.

- Toggle key: `cachedRandomDat`
  Source: `base-api/src/main/java/com/tonic/util/ClientConfig.java:shouldCacheRandomDat`
- Mixins that read and write cached values
  Source: `src/main/java/com/tonic/mixins/TRandomDatMixin.java`
- Storage backend
  Source: `base-api/src/main/java/com/tonic/model/RandomDat.java`

### Cached Device ID

When enabled, VitaLite generates and reuses a UUID per account identifier instead of the default device ID.

- Toggle key: `cachedDeviceID`
  Source: `base-api/src/main/java/com/tonic/util/ClientConfig.java:shouldCacheDeviceId`
- Mixin behavior
  Source: `src/main/java/com/tonic/mixins/TDeviceIDMixin.java`
- Storage backend
  Source: `base-api/src/main/java/com/tonic/model/DeviceID.java` (`CachedUUID`)

### Bank cache persistence

When enabled, VitaLite persists a snapshot of bank contents per player name and exposes cached lookup helpers.

- Toggle key: `cachedBank`
  Source: `base-api/src/main/java/com/tonic/util/ClientConfig.java:shouldCacheBank`
- Implementation and storage backend
  Source: `api/src/main/java/com/tonic/services/BankCache.java`

### Headless mode (reduced rendering)

The VitaLite options panel can toggle a headless mode that hides the client panel or overlays a headless map view, and stops and restarts the GPU plugin when a map overlay is used.
Source: `base-api/src/main/java/com/tonic/headless/HeadlessMode.java:toggleHeadless`
Source: `base-api/src/main/java/com/tonic/Static.java:setHeadless`

### Embedded logger panel and packet logging

VitaLite can embed a console style logger panel into the client UI and provides toggles for logging menu actions and packets.

- Logger UI embedding
  Source: `src/main/java/com/tonic/runelite/ClientUIUpdater.java:inject`
- Logger formatting support with `{}` placeholders
  Source: `base-api/src/main/java/com/tonic/Logger.java`
  Source: `base-api/src/main/java/com/tonic/util/LoggerFormatting.java`
- Packet and menu action hooks
  Source: `base-api/src/main/java/com/tonic/model/ui/VitaLiteOptionsPanel.java:onPacketSent`
  Source: `src/main/java/com/tonic/mixins/TDoActionMixin.java`

### Click strategies and click box integration

The click manager supports configurable click strategies and allows plugins to provide a click box for controlled clicks.
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java`
Source: `api/src/main/java/com/tonic/util/ClickManagerUtil.java`

Related documentation: `docs/CLICKMANAGER.md`

### Mouse movement training and spoofing

VitaLite can record mouse movement trajectories and generate synthetic mouse movement samples for clicks (when enabled).
Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/TrajectoryService.java`
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java`

Related documentation: `docs/MOUSE-MOVEMENT.md`

### JVM profiler window

VitaLite includes a JVM profiler window that can be opened from the options panel.
Source: `base-api/src/main/java/com/tonic/model/ui/VitaLiteOptionsPanel.java` (Profiler button)
Source: `base-api/src/main/java/com/tonic/services/profiler/ProfilerWindow.java`

Related documentation: `docs/PROFILER.md`

### Proxy support

The client can set a JVM wide SOCKS proxy from the CLI. The proxy is verified using an HTTP request.
Source: `base-api/src/main/java/com/tonic/services/proxy/ProxyManager.java:process`
Source: `base-api/src/main/java/com/tonic/services/proxy/SocksProxyUtil.java:verify`

### Incognito mode

Incognito mode avoids VitaLite branding changes (for example splash screen patching).
Source: `src/main/java/com/tonic/rlmixins/SplashScreenMixin.java:constructorHook`

### Self update and version checks (release builds)

When running from a shaded jar, VitaLite performs version checks and can prompt the user to update by opening the releases page.
Source: `src/main/java/com/tonic/VitaLite.java:main`
Source: `src/main/java/com/tonic/vitalite/SelfUpdate.java:checkAndUpdate`

## Side Loading Plugins

VitaLite supports sideloading external plugin jars from the RuneLite directory.
It scans both:

- `${user.home}/.runelite/sideloaded-plugins`
- `${user.home}/.runelite/externalplugins`

Source: `api/src/main/java/com/tonic/services/hotswapper/PluginReloader.java:findJars`

Related documentation: `docs/EXTERNALPLUGIN.md`
