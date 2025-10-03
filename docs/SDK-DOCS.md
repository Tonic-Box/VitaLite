# VitaLite SDK Documentation

## Overview

VitaLite SDK is a comprehensive framework for developing RuneLite plugins, providing enhanced APIs, utilities, and tools to simplify plugin development. It consists of two main modules: `api` and `base-api`, each serving distinct purposes in the plugin ecosystem.

## Module Structure

### base-api Module
The `base-api` module provides foundational utilities and core functionality for plugin development.

**Key Components:**
- **Static Access**: [`Static.java`](../base-api/src/main/java/com/tonic/Static.java) - Central static access point for client, RuneLite instance, and thread invocation
- **Logging**: [`Logger.java`](../base-api/src/main/java/com/tonic/Logger.java) - Enhanced logging utilities
- **Click Management**: [`ClickManager.java`](../base-api/src/main/java/com/tonic/services/ClickManager.java) - Advanced click handling with multiple strategies
- **Packet Handling**: Packet classes in [`packets/`](../base-api/src/main/java/com/tonic/packets/) directory
- **Event System**: Custom events in [`events/`](../base-api/src/main/java/com/tonic/events/) directory
- **UI Components**: Custom UI elements in [`model/ui/`](../base-api/src/main/java/com/tonic/model/ui/) directory

**Build Configuration:**
- Uses Gradle with Kotlin DSL
- Key dependencies: Netty, Gson, Guice, Apache Commons, ANTLR, mxGraph
- Includes task for syncing RuneLite API files from GitHub

### api Module
The `api` module provides higher-level APIs and utilities for game interaction and plugin functionality.

**Key Components:**
- **Entity APIs**: [`entities/`](../api/src/main/java/com/tonic/entities/) - Actor, NPC, Player, TileItem, and TileObject APIs
- **Game APIs**: [`game/`](../api/src/main/java/com/tonic/game/) - ClientScript, Combat, Game, House, Movement, Quest, Scene, Skill, and Var APIs
- **Widget APIs**: [`widgets/`](../api/src/main/java/com/tonic/widgets/) - Bank, Dialogue, Emote, Equipment, GrandExchange, Inventory, Magic, MiniMap, Prayer, Shop, SlayerRewards, Tabs, Widget, and WorldMap APIs
- **Data Classes**: [`data/`](../api/src/main/java/com/tonic/data/) - Extended game data representations and constants
- **Query System**: [`queries/`](../api/src/main/java/com/tonic/queries/) - Flexible query system for filtering game entities
- **Services**: [`services/`](../api/src/main/java/com/tonic/services/) - Pathfinding, code evaluation, and other services
- **Utilities**: [`util/`](../api/src/main/java/com/tonic/util/) - Coroutine system and other utilities

**Build Configuration:**
- Depends on `base-api` module
- Uses RuneLite client as compileOnly dependency
- Includes Lombok for reduced boilerplate code

## Core Utilities

### Static Access
[`Static.java`](base-api/src/main/java/com/tonic/Static.java) provides centralized access to critical components:

```java
// Get client instance
TClient client = Static.getClient();

// Invoke code on client thread
Static.invoke(() -> {
    // Client thread code here
});

// Post events to event bus
Static.post(new CustomEvent());
```

### Coroutine System
[`Coroutine.java`](api/src/main/java/com/tonic/util/Coroutine.java) enables cooperative multitasking:

```java
Coroutine coroutine = new Coroutine(() -> {
    while (!coroutine.isCancelled()) {
        // Task logic
        Coroutine.checkYieldStatus(); // Check for yields
    }
});
new Thread(coroutine).start();
```

### Click Management
[`ClickManager.java`](base-api/src/main/java/com/tonic/services/ClickManager.java) supports multiple click strategies:

```java
// Static clicking
ClickManager.setPoint(x, y);
ClickManager.click();

// Random clicking within viewport
ClickManager.setStrategy(ClickStrategy.RANDOM);
ClickManager.click();

// Controlled clicking within shape
Shape clickArea = new Rectangle(x, y, width, height);
ClickManager.queueClickBox(clickArea);
ClickManager.setStrategy(ClickStrategy.CONTROLLED);
ClickManager.click();
```

## Query System

The query system provides fluent interfaces for filtering game entities:

### Inventory Queries
[`InventoryQuery.java`](api/src/main/java/com/tonic/queries/InventoryQuery.java) example:

### Widget Queries
[`WidgetQuery.java`](../api/src/main/java/com/tonic/queries/WidgetQuery.java) provides widget filtering:

## Plugin Development

### Basic Plugin Structure
Plugins should extend [`VitaPlugin.java`](../api/src/main/java/com/tonic/util/VitaPlugin.java):

```java
@PluginDescriptor(
    name = "Example Plugin",
    description = "A sample VitaLite plugin"
)
public class ExamplePlugin extends VitaPlugin {
    @Inject
    private ExamplePluginConfig config;
    
    @Override
    protected void startUp() {
        // Plugin initialization
    }
    
    @Override
    protected void shutDown() {
        // Cleanup
    }

    @Override
    public void loop() throws Exception
    {
        //threaded space for code to run, runes once untill thread ends then
        //waits till next gametick to run again
    }
}
```

### Overlays
Extend [`VitaOverlay.java`](../api/src/main/java/com/tonic/ui/VitaOverlay.java) for custom overlays:

## Build System

### Dependencies
The SDK uses Gradle with specific dependencies:

**base-api dependencies:**
- Netty for networking
- Gson for JSON processing
- Guice for dependency injection
- Apache Commons for configuration
- ANTLR for language processing
- mxGraph for graph visualization

**api dependencies:**
- RuneLite client (compileOnly)
- Lombok for reduced boilerplate
- Trove4j and fastutil for collections

## Pathfinding System

The SDK includes a comprehensive pathfinding system in [`services/pathfinder/`](../api/src/main/java/com/tonic/services/pathfinder/):

- **Pathfinder**: Main pathfinding logic
- **Collision Maps**: [`CollisionMap.java`](api/src/main/java/com/tonic/services/pathfinder/collision/CollisionMap.java) for navigation
- **Transports**: Various transport methods (fairy rings, spirit trees, etc.)
- **Requirements**: Skill, item, and quest requirements for path validation

## Best Practices

1. **Use Static Access**: Always use [`Static.invoke()`](../base-api/src/main/java/com/tonic/Static.java:82) for client thread operations
2. **Leverage Queries**: Use the query system for efficient entity filtering
3. **Handle Threading**: Use coroutines for background tasks to maintain responsiveness
4. **Proper Cleanup**: Implement `shutDown()` method to release resources
5. **Error Handling**: Use [`Logger.java`](../base-api/src/main/java/com/tonic/Logger.java) for consistent logging

## Troubleshooting

**Common Issues:**
- **Missing Dependencies**: Ensure all Gradle dependencies are resolved
- **Threading Errors**: Use `Static.invoke()` for client thread access
- **API Sync Issues**: Run `:base-api:syncRuneliteApi` task to update RuneLite API files
