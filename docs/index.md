# VitaLite Documentation

This documentation covers the VitaLite client, its embedded SDK, and the built in plugins shipped with the client.

## Start Here

- [Getting Started](GETTING-STARTED.md) - Build and run from source, release packaging, and where VitaLite stores data.
- [CLI Overview](CLI.md) - Command line options and parsing rules.
- [CLI Reference](cli-reference.md) - Complete command line reference.
- [Configuration](CONFIGURATION.md) - User and developer configuration files, keys, and defaults.
- [Authentication](AUTHENTICATION.md) - Auto login options and the Enhanced Profiles plugin flows.
- [External Plugins](EXTERNALPLUGIN.md) - External plugin development and sideloading.
- [Troubleshooting](TROUBLESHOOTING.md) - Troubleshooting and failure modes.

## Feature and Advanced Topics

- [Features](FEATURES.md) - User visible features and where they live in code.
- [Click Manager](CLICKMANAGER.md) - Click strategies and click box integration.
- [Mouse Movement](MOUSE-MOVEMENT.md) - Mouse movement training, recording, and generation.
- [Script DSL](SCRIPT-DSL.md) - Script DSL (StepHandler builder) usage and patterns.
- [Profiler](PROFILER.md) - JVM profiler window and related utilities.
- [Injector](INJECTOR.md) - Injector pipeline and mixin development workflow.
- [Common Mistakes](common-mistakes.md) - Frequent errors and how to fix them.

## API Reference

- [Query System](api/queries.md) - NPC, Item, Object, and Widget queries.
- [Interaction APIs](api/interactions.md) - Click, movement, and dialogue handling.
- [Pathfinding and Walker](api/pathfinding.md) - World walker and pathfinding.
- [IPC Channel](api/ipc.md) - Inter-process communication between clients.
- [Static](api/static.md) - Core access class.
- [Logger](api/logger.md) - Logging utilities.

## Scope and Non Goals

- These docs only describe behavior present in this repository.
- If a feature is mentioned in UI text but not backed by code, it is treated as unconfirmed and is not documented as behavior.

## Version

The current version is defined in `build.gradle.kts`.
