# Getting Started

## For Users (Pre-built Release)

If you just want to run VitaLite, download the latest release from the VitaLite Launcher:

- https://github.com/Tonic-Box/VitaLauncher/releases

## What This Repo Contains

This repository builds a runnable VitaLite client and three Java modules:

- `src/` - The VitaLite launcher, injector, and mixins that modify RuneLite and the gamepack at runtime.
- `base-api/` - Shared SDK types and services used by the client and plugins.
- `api/` - Higher level SDK APIs for interacting with the client and game state.
- `plugins/` - Built in plugins that are embedded into the client at build time.

Source: `settings.gradle.kts`

## Requirements

- Java 11 is required for builds and local runs.
  Source: `build.gradle.kts` (JavaVersion check)

## Build and Run From Source

1. (Required for first build and after revision updates only) Sync selected `runelite-api` sources into `base-api`.
   - Task: `:base-api:syncRuneliteApi`
   - Source: `base-api/build.gradle.kts:syncRuneliteApi`

2. Publish all modules to Maven Local so they can be consumed as dependencies during development.
   - Task: `buildAndPublishAll`
   - Source: `build.gradle.kts:buildAndPublishAll`

3. Run the client from your IDE using the `com.tonic.VitaLite` main class.
   - Source: `src/main/java/com/tonic/VitaLite.java:main`

### Why `com.tonic.VitaLite` and Not `com.tonic.vitalite.Main`

`com.tonic.VitaLite` re launches `com.tonic.vitalite.Main` in a new JVM and injects the internal `-safeLaunch` flag.
`com.tonic.vitalite.Main` exits immediately if `-safeLaunch` was not provided.

Source: `src/main/java/com/tonic/VitaLite.java:isSafeLaunch`
Source: `src/main/java/com/tonic/vitalite/Main.java:main`

## Run a Release Build

The release pipeline can package a runnable zip containing `VitaLite.jar` and launcher scripts:

- Task: `buildRelease`
- Task: `packageRelease`

Source: `build.gradle.kts:buildRelease`
Source: `build.gradle.kts:packageRelease`

On Windows, `scripts/run-windows.bat` downloads a JDK into `~/.runelite/vitalite/jdk` and runs `VitaLite.jar`.
Source: `scripts/run-windows.bat`

## Run the Jar

The runnable jar entry point is `com.tonic.VitaLite`.
Source: `build.gradle.kts` (jar manifest `Main-Class`)
Source: `src/main/java/com/tonic/VitaLite.java:main`

Example:

```sh
java -jar VitaLite.jar
```

## Command Line Options

For command line options and parsing rules, see:

- `docs/CLI.md`
- `docs/cli-reference.md`

When running via Gradle:

```sh
./gradlew run --args="[OPTIONS]"
```

## Side-Loading External Plugins

VitaLite scans for external plugin jars in:

- `${user.home}/.runelite/sideloaded-plugins`
- `${user.home}/.runelite/externalplugins`

Source: `api/src/main/java/com/tonic/services/hotswapper/PluginReloader.java:findJars`

See `docs/EXTERNALPLUGIN.md`.

## Data and File Locations

VitaLite uses the RuneLite home directory plus a VitaLite subdirectory:

- RuneLite base directory: `${user.home}/.runelite`
- VitaLite directory: `${user.home}/.runelite/vitalite`

Source: `base-api/src/main/java/com/tonic/Static.java` (`RUNELITE_DIR`, `VITA_DIR`)

## First Run Notes

On first launch VitaLite will create the VitaLite directory and related data files as needed.

If startup fails, see `docs/TROUBLESHOOTING.md`.
