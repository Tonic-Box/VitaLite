# VitaLite SDK Documentation

VitaLite is a launcher and SDK for RuneLite that provides additional features and a comprehensive API for plugin development.

## Documentation Contents

### Getting Started
- [Installation and Setup](getting-started.md) - How to install and run VitaLite
- [External Plugin Development](EXTERNALPLUGIN.md) - Guide to building plugins
- [SDK Overview](SDK-DOCS.md) - High-level API overview

### Core Features
- [Features Overview](FEATURES.md) - Built-in client features
- [Configuration Reference](configuration.md) - All configuration options
- [Command Line Options](cli-reference.md) - Command line arguments

### API Reference
- [Query System](api/queries.md) - NPC, Item, Object, and Widget queries
- [Interaction APIs](api/interactions.md) - Click, movement, and dialogue handling
- [Pathfinding and Walker](api/pathfinding.md) - World walker and pathfinding
- [IPC Channel](api/ipc.md) - Inter-process communication between clients

### Advanced Topics
- [Click Manager](CLICKMANAGER.md) - Mouse click strategies and configuration
- [Mouse Movement System](MOUSE-MOVEMENT.md) - Trajectory-based mouse movement
- [Script DSL](SCRIPT-DSL.md) - Coroutine-style automation scripting
- [Profiler](PROFILER.md) - JVM profiling and performance monitoring

### Troubleshooting
- [Common Mistakes](common-mistakes.md) - Frequent errors and how to fix them

---

## Quick Links

| Topic | Description |
|-------|-------------|
| [VitaPlugin](EXTERNALPLUGIN.md#plugin-structure) | Base class for looped plugins |
| [Static](api/static.md) | Global access to client objects |
| [Logger](api/logger.md) | Logging utilities |
| [Walker](api/pathfinding.md) | World pathfinding |
| [Query System](api/queries.md) | Entity and item searching |

---

## Version Information

Current version can be found in `build.gradle.kts`. The versioning follows the format `major.minor.patch.subrev_build`.

Recent notable changes:
- Version 1.12.12: JVM-wide proxy support, proxy display in title bar
- Version 1.12.11: Logger panel visibility persistence, updated collision maps
- Version 1.12.9: Client thread deadlock watcher and recovery

Source: `build.gradle.kts`, git history
