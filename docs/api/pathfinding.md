# Pathfinding and Walker

VitaLite includes a comprehensive pathfinding system for navigating the game world.

## Overview

The pathfinding system consists of:
- **Walker** - High-level world walking API
- **Pathfinder algorithms** - Multiple algorithm implementations
- **Collision maps** - Pre-loaded collision data
- **Transport system** - Teleports and shortcuts

Source: `api/src/main/java/com/tonic/services/pathfinder/`

---

## Walker

The `Walker` class provides high-level world navigation.

### Basic Usage

```java
// Walk to a specific point
Walker.walkTo(new WorldPoint(3200, 3200, 0));

// Walk to nearest area from a list
List<WorldArea> banks = List.of(
    new WorldArea(3180, 3430, 10, 10, 0),
    new WorldArea(3250, 3420, 10, 10, 0)
);
Walker.walkTo(banks);
```

### Walking with Stop Condition

```java
// Stop walking if condition becomes true
boolean stopped = Walker.walkTo(target, () -> {
    // Return true to stop walking
    return PlayerEx.getLocal().getHealthRatio() < 20;
});

if (stopped) {
    Logger.info("Walking stopped due to condition");
}
```

### Boat Navigation

For sailing areas:

```java
// Sail to a destination
Walker.sailTo(new WorldPoint(3680, 3500, 0));
```

Source: `api/src/main/java/com/tonic/services/pathfinder/Walker.java:70-84`

### Checking State

```java
// Check if walker is active
if (Walker.isWalking()) {
    // ...
}

// Cancel walking
Walker.cancel();
```

---

## Walker Settings

Configure walker behavior through static settings:

```java
// Range for toggling run on (energy %)
Walker.Setting.toggleRunRange = new IntPair(25, 35);

// Range for consuming stamina potions (energy %)
Walker.Setting.consumeStaminaRange = new IntPair(50, 60);

// Update thresholds (randomized within range)
Walker.Setting.toggleRunThreshold = Walker.Setting.toggleRunRange.randomEnclosed();
Walker.Setting.consumeStaminaThreshold = Walker.Setting.consumeStaminaRange.randomEnclosed();
```

Source: `api/src/main/java/com/tonic/services/pathfinder/Walker.java:61-68`

---

## Pathfinding Algorithms

The system supports multiple pathfinding algorithms:

| Algorithm | Description | Use Case |
|-----------|-------------|----------|
| `HYBRID_BFS` | Hybrid bidirectional BFS | Default, balanced |
| `BI_DIR_BFS` | Pure bidirectional BFS | Simple paths |
| `ASTAR` | A* pathfinding | Optimal paths |
| `JPS` | Jump Point Search | Open areas |
| `FLOW_FIELD` | Flow field algorithm | Multi-destination |

Source: `base-api/src/main/java/com/tonic/services/pathfinder/PathfinderAlgo.java`

---

## Collision Map

The Walker uses pre-loaded collision data:

```java
// Access collision map
CollisionMap collisionMap = Walker.getCollisionMap();

// Find nearest walkable tile
WorldPoint walkable = collisionMap.nearestWalkableEuclidean(target, 5);

// Access object map
ObjectMap objectMap = Walker.getObjectMap();

// Access tile type map
TileTypeMap tileTypeMap = Walker.getTileTypeMap();

// Access navigation graph (for sailing)
NavGraph navGraph = Walker.getNavGraph();
```

Source: `api/src/main/java/com/tonic/services/pathfinder/Walker.java:33-52`

---

## Transport System

The pathfinder automatically uses transports (teleports, shortcuts) when beneficial.

### Transport Types

| Type | Description |
|------|-------------|
| Fairy Rings | Using fairy ring codes |
| Spirit Trees | Spirit tree network |
| Gnome Gliders | Gnome glider network |
| Canoes | River canoe system |
| Charter Ships | Ship charter routes |
| Minecarts | Keldagrim minecarts |
| Teleports | Item and spell teleports |

Source: `api/src/main/java/com/tonic/services/pathfinder/transports/`

### Transport Requirements

Transports have requirements that are checked automatically:

```java
// Requirements checked:
// - Quest completion
// - Skill levels
// - Item requirements
// - Varbit states
```

Source: `api/src/main/java/com/tonic/services/pathfinder/requirements/`

---

## Local Pathfinding

For short-range pathfinding within the loaded scene:

```java
// Local pathfinder for visible area
LocalPathfinder localPathfinder = new LocalPathfinder();

// Scene-based collision
LocalCollisionMap localCollision = new LocalCollisionMap();
```

Source: `api/src/main/java/com/tonic/services/pathfinder/LocalPathfinder.java`

---

## WalkerPath

The Walker internally uses `WalkerPath` objects:

```java
// Create a path
WalkerPath path = WalkerPath.get(targetPoint);

// Execute path
while (path.step()) {
    Delays.tick();
}

// Shutdown path
path.shutdown();

// Cancel path
path.cancel();
```

Source: `api/src/main/java/com/tonic/services/pathfinder/model/WalkerPath.java`

---

## Strategic Pathing

For multi-region pathfinding with transport optimization:

```java
// StrategicPathing handles:
// - Teleport selection
// - Transport chaining
// - Multi-region navigation
```

Source: `api/src/main/java/com/tonic/services/pathfinder/StrategicPathing.java`

---

## Common Mistakes

### Not checking reachability

**Wrong:**
```java
Walker.walkTo(unreachablePoint);  // May get stuck
```

**Correct:**
```java
WorldPoint walkable = Walker.getCollisionMap()
    .nearestWalkableEuclidean(targetPoint, 5);
Walker.walkTo(walkable);
```

### Calling walkTo from client thread

**Wrong:**
```java
// In a GameTick handler (client thread)
Walker.walkTo(target);  // Blocks client thread!
```

**Correct:**
```java
// Use from VitaPlugin.loop() or another thread
@Override
public void loop() {
    Walker.walkTo(target);  // OK - runs in separate thread
}
```

### Not handling cancel

**Wrong:**
```java
Walker.walkTo(target);  // May never return if player dies
```

**Correct:**
```java
boolean stopped = Walker.walkTo(target, () -> {
    return !GameAPI.isLoggedIn() || PlayerEx.getLocal().isDead();
});
```

### Ignoring return value

**Wrong:**
```java
Walker.walkTo(target, stopCondition);
// Assuming we reached target
doAction();
```

**Correct:**
```java
boolean stoppedEarly = Walker.walkTo(target, stopCondition);
if (!stoppedEarly) {
    // We reached the destination
    doAction();
}
```

---

## Performance Tips

1. **Use area targets** when exact location doesn't matter
2. **Set appropriate stop conditions** to avoid unnecessary walking
3. **Configure stamina thresholds** based on your use case
4. **Check `isWalking()`** before starting new paths

```java
// Good: Use area target
Walker.walkTo(List.of(bankArea));

// Good: Stop early when possible
Walker.walkTo(target, () -> {
    return targetObject != null && targetObject.getWorldLocation().distanceTo(playerLoc) < 3;
});
```

---

## Extending Transports

Transports are defined in JSON format and can be extended:

```java
// Transport definition structure
{
    "name": "Transport Name",
    "origin": {"x": 0, "y": 0, "plane": 0},
    "destination": {"x": 0, "y": 0, "plane": 0},
    "requirements": {
        "skills": [...],
        "quests": [...],
        "items": [...]
    }
}
```

Source: `api/src/main/java/com/tonic/services/pathfinder/transports/TransportLoader.java`
