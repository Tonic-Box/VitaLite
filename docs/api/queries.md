# Query System

VitaLite provides a fluent query system for searching game entities, items, and widgets.

## Overview

All queries follow the same pattern:
1. Create a query instance
2. Chain filter methods
3. Call a terminal operation (first, collect, count, etc.)

Queries are thread-safe and use client thread invocation internally.

---

## Query Types

| Query Class | Purpose | Source |
|-------------|---------|--------|
| `NpcQuery` | Search NPCs | `api/src/main/java/com/tonic/queries/NpcQuery.java` |
| `PlayerQuery` | Search players | `api/src/main/java/com/tonic/queries/PlayerQuery.java` |
| `TileObjectQuery` | Search game objects | `api/src/main/java/com/tonic/queries/TileObjectQuery.java` |
| `TileItemQuery` | Search ground items | `api/src/main/java/com/tonic/queries/TileItemQuery.java` |
| `InventoryQuery` | Search inventory items | `api/src/main/java/com/tonic/queries/InventoryQuery.java` |
| `WidgetQuery` | Search UI widgets | `api/src/main/java/com/tonic/queries/WidgetQuery.java` |
| `WorldQuery` | Search game worlds | `api/src/main/java/com/tonic/queries/WorldQuery.java` |
| `ShopQuery` | Search shop items | `api/src/main/java/com/tonic/queries/ShopQuery.java` |
| `LocationQuery` | Search locations | `api/src/main/java/com/tonic/queries/LocationQuery.java` |

---

## NpcQuery

Search for NPCs in the game world.

### Basic Usage

```java
// Find first banker
NpcEx banker = new NpcQuery()
    .withName("Banker")
    .first();

// Find all guards
List<NpcEx> guards = new NpcQuery()
    .withName("Guard")
    .collect();
```

### Filter Methods

| Method | Description |
|--------|-------------|
| `withIds(int... ids)` | Filter by NPC IDs |
| `withName(String name)` | Filter by exact name (case-insensitive) |
| `withNames(String... names)` | Filter by multiple names |
| `withNameContains(String part)` | Filter by partial name |
| `withAction(String action)` | Filter by available action |
| `withIndex(int index)` | Filter by NPC index |

### Inherited Methods (from AbstractActorQuery)

| Method | Description |
|--------|-------------|
| `notMoving()` | Filter to non-moving actors |
| `notAnimating()` | Filter to non-animating actors |
| `notInCombat()` | Filter to actors not in combat |
| `inCombat()` | Filter to actors in combat |
| `notInteractingWithMe()` | Filter to actors not interacting with local player |
| `interactingWithMe()` | Filter to actors interacting with local player |
| `withinDistance(int distance)` | Filter by distance from player |
| `withinArea(WorldArea area)` | Filter by world area |
| `sortNearest()` | Sort by distance (ascending) |
| `sortFurthest()` | Sort by distance (descending) |
| `nearest()` | Get nearest actor (terminal) |

### Example: Find attackable NPC

```java
NpcEx target = new NpcQuery()
    .withName("Goblin")
    .withAction("Attack")
    .notInCombat()
    .nearest();

if (target != null) {
    NpcAPI.interact(target, "Attack");
}
```

Source: `api/src/main/java/com/tonic/queries/NpcQuery.java`

---

## InventoryQuery

Search for items in inventory or other item containers.

### Creating Queries

```java
// From player inventory (default)
InventoryQuery query = InventoryAPI.search();

// From bank
InventoryQuery bankQuery = BankAPI.search();

// From specific container
InventoryQuery containerQuery = InventoryQuery.fromInventoryId(InventoryID.BANK);

// From current shop
InventoryQuery shopQuery = InventoryQuery.fromCurrentShop();
```

### Filter Methods

| Method | Description |
|--------|-------------|
| `withId(int... ids)` | Filter by item IDs |
| `withCanonicalId(int... ids)` | Filter by canonical item IDs |
| `withName(String... names)` | Filter by exact names |
| `withNameContains(String part)` | Filter by partial name |
| `withNameMatches(String pattern)` | Filter by wildcard pattern |
| `withAction(String action)` | Filter by available action |
| `withActionContains(String part)` | Filter by partial action name |
| `fromSlot(int... slots)` | Filter by inventory slot |
| `greaterThanGePrice(int price)` | Filter by minimum GE price |
| `lessThanGePrice(int price)` | Filter by maximum GE price |
| `greaterThanHighAlchValue(int value)` | Filter by minimum alch value |

### Terminal Operations

| Method | Description |
|--------|-------------|
| `first()` | Get first matching item |
| `collect()` | Get all matching items |
| `count()` | Get total quantity |
| `getQuantity()` | Get sum of quantities |
| `unique()` | Get unique items (by ID) |
| `getTotalGeValue()` | Get total GE value |
| `getTotalShopValue()` | Get total shop value |
| `getTotalHighAlchValue()` | Get total alch value |

### Example: Check for food

```java
// Find any food
ItemEx food = InventoryAPI.search()
    .withAction("Eat")
    .first();

// Count sharks
int sharkCount = InventoryAPI.search()
    .withName("Shark")
    .count();

// Get total inventory value
long totalValue = InventoryAPI.search()
    .getTotalGeValue();
```

Source: `api/src/main/java/com/tonic/queries/InventoryQuery.java`

---

## TileObjectQuery

Search for game objects (doors, trees, rocks, etc.).

### Basic Usage

```java
// Find nearest bank booth
TileObjectEx booth = TileObjectAPI.search()
    .withName("Bank booth")
    .withAction("Bank")
    .nearest();

// Find all rocks
List<TileObjectEx> rocks = TileObjectAPI.search()
    .withNameContains("rocks")
    .collect();
```

### Filter Methods

| Method | Description |
|--------|-------------|
| `withId(int... ids)` | Filter by object IDs |
| `withName(String name)` | Filter by exact name |
| `withNameContains(String part)` | Filter by partial name |
| `withAction(String action)` | Filter by available action |
| `withinDistance(int distance)` | Filter by distance |
| `withinArea(WorldArea area)` | Filter by world area |

### Sorting Methods

| Method | Description |
|--------|-------------|
| `sortNearest()` | Sort by distance (ascending) |
| `sortFurthest()` | Sort by distance (descending) |
| `sortShortestPath()` | Sort by actual path distance |
| `nearest()` | Get nearest object (terminal) |

### Example: Mining script

```java
TileObjectEx rock = TileObjectAPI.search()
    .withNameContains("Iron rocks")
    .withAction("Mine")
    .sortShortestPath()
    .first();

if (rock != null) {
    TileObjectAPI.interact(rock, "Mine");
}
```

Source: `api/src/main/java/com/tonic/queries/TileObjectQuery.java`

---

## WidgetQuery

Search for UI widgets.

### Basic Usage

```java
// Find continue button
Widget continueBtn = WidgetAPI.search()
    .withText("Click here to continue")
    .first();

// Find all visible widgets with specific text
List<Widget> widgets = WidgetAPI.search()
    .withTextContains("Bank")
    .isVisible()
    .collect();
```

### Filter Methods

| Method | Description |
|--------|-------------|
| `withId(int... widgetIds)` | Filter by widget IDs |
| `withParentId(int... parentIds)` | Filter by parent widget IDs |
| `withText(String text)` | Filter by exact text |
| `withTextContains(String... parts)` | Filter by partial text |
| `withActions(String... actions)` | Filter by available actions |
| `isVisible()` | Filter to visible widgets only |
| `isHidden()` | Filter to hidden widgets only |
| `isSelfVisible()` | Filter by widget's own visibility |
| `isSelfHidden()` | Filter by widget's own hidden state |

Source: `api/src/main/java/com/tonic/queries/WidgetQuery.java`

---

## WorldQuery

Search for game worlds.

### Basic Usage

```java
// Find PvP worlds
List<World> pvpWorlds = new WorldQuery()
    .pvp()
    .collect();

// Find members worlds with low ping
World bestWorld = new WorldQuery()
    .members()
    .maxPing(50)
    .first();
```

Source: `api/src/main/java/com/tonic/queries/WorldQuery.java`

---

## LocationQuery

Search for tiles in the game world.

### Basic Usage

```java
// Find reachable tiles within distance
List<Tile> tiles = new LocationQuery()
    .withinDistance(10)
    .isReachable()
    .collect();

// Find tiles within an area
List<Tile> areaTiles = new LocationQuery()
    .withinArea(new WorldArea(3200, 3200, 20, 20, 0))
    .collect();
```

### Filter Methods

| Method | Description |
|--------|-------------|
| `isReachable()` | Filter to tiles reachable from player |
| `hasLosTo()` | Filter to tiles with line of sight to player |
| `withinDistance(int distance)` | Filter by Chebyshev distance from player |
| `beyondDistance(int distance)` | Filter to tiles beyond distance |
| `withinPathingDistance(int distance)` | Filter by actual path distance |
| `beyondPathingDistance(int distance)` | Filter to tiles beyond path distance |
| `withinArea(WorldArea area)` | Filter to tiles within area |
| `outsideArea(WorldArea area)` | Filter to tiles outside area |
| `hasTileObject()` | Filter to tiles containing objects |

### Conversion Methods

| Method | Description |
|--------|-------------|
| `toWorldPointList()` | Convert results to WorldPoint list |
| `toLocalPointList()` | Convert results to LocalPoint list |

Source: `api/src/main/java/com/tonic/queries/LocationQuery.java`

---

## ShopQuery

Search for shops in the game.

### Basic Usage

```java
// Find accessible shops nearby
Shop shop = new ShopQuery()
    .canAccess()
    .sortNearest()
    .first();

// Find shops by name
List<Shop> generalStores = new ShopQuery()
    .withNameContains("GENERAL")
    .collect();
```

### Filter Methods

| Method | Description |
|--------|-------------|
| `withInventoryId(int... ids)` | Filter by inventory IDs |
| `withShopkeeper(NpcLocations... shopkeepers)` | Filter by shopkeeper NPC |
| `withShopkeeperNameContains(String namePart)` | Filter by shopkeeper name substring |
| `withShopkeeperNameMatches(String pattern)` | Filter by wildcard pattern |
| `withName(String name)` | Filter by exact shop enum name |
| `withNameContains(String namePart)` | Filter by shop name substring |
| `withNames(String... names)` | Filter by multiple shop names |
| `withNameMatches(String pattern)` | Filter by wildcard pattern |
| `canAccess()` | Filter to accessible shops |
| `withRequirements()` | Filter to shops with requirements |
| `withoutRequirements()` | Filter to shops without requirements |
| `within(int distance)` | Filter by distance from player |
| `within(WorldPoint center, int distance)` | Filter by distance from point |
| `atLocation(WorldPoint location)` | Filter by exact location |

### Sorting Methods

| Method | Description |
|--------|-------------|
| `sortNearest()` | Sort by Euclidean distance (nearest first) |
| `sortFurthest()` | Sort by Euclidean distance (furthest first) |
| `sortShortestPath()` | Sort by pathfinding distance |
| `sortLongestPath()` | Sort by pathfinding distance (longest first) |
| `sortShortestGlobalPath()` | Sort by global path (includes teleports) |
| `sortLongestGlobalPath()` | Sort by global path (longest first) |

### Terminal Operations

| Method | Description |
|--------|-------------|
| `nearest()` | Get nearest shop |
| `furthest()` | Get furthest shop |
| `shortestPath()` | Get shop with shortest path |
| `longestPath()` | Get shop with longest path |
| `shortestGlobalPath()` | Get shop with shortest global path |
| `getCurrent()` | Get currently open shop |

Source: `api/src/main/java/com/tonic/queries/ShopQuery.java`

---

## EntityQuery

Combined query for all entity types (NPCs, Players, TileItems, TileObjects).

### Basic Usage

```java
// Find any entity by name
Entity entity = new EntityQuery()
    .withName("Banker")
    .nearest();

// Find NPCs and objects only
List<Entity> entities = new EntityQuery()
    .removePlayers()
    .removeTileItems()
    .withAction("Bank")
    .collect();
```

### Type Filters

| Method | Description |
|--------|-------------|
| `ofTypes(Class<? extends Entity>... types)` | Filter to specific entity types |
| `removePlayers()` | Exclude players |
| `removeNpcs()` | Exclude NPCs |
| `removeTileItems()` | Exclude ground items |
| `removeTileObjects()` | Exclude game objects |

### Filter Methods

| Method | Description |
|--------|-------------|
| `withId(int... id)` | Filter by IDs |
| `withName(String name)` | Filter by exact name |
| `withNameContains(String name)` | Filter by name substring |
| `withNames(String... names)` | Filter by multiple names |
| `withNamesContains(String... names)` | Filter by multiple name substrings |
| `withNameMatches(String namePart)` | Filter by wildcard pattern |
| `withAction(String action)` | Filter by action |
| `withPartialAction(String partial)` | Filter by partial action |
| `withinDistance(int distance)` | Filter by distance |
| `beyondDistance(int distance)` | Filter beyond distance |

### Sorting Methods

| Method | Description |
|--------|-------------|
| `sortNearest()` | Sort by distance (nearest first) |
| `sortFurthest()` | Sort by distance (furthest first) |
| `sortShortestPath()` | Sort by path distance |
| `sortLongestPath()` | Sort by path distance (longest first) |

### Terminal Operations

| Method | Description |
|--------|-------------|
| `nearest()` | Get nearest entity |
| `farthest()` | Get farthest entity |
| `shortestPath()` | Get entity with shortest path |
| `longestPath()` | Get entity with longest path |

Source: `api/src/main/java/com/tonic/queries/combined/EntityQuery.java`

---

## Abstract Query Methods

All queries inherit from `AbstractQuery` and have these common methods:

### Filtering

| Method | Description |
|--------|-------------|
| `keepIf(Predicate<T>)` | Keep only matching elements |
| `removeIf(Predicate<T>)` | Remove matching elements |

### Terminal Operations

| Method | Description |
|--------|-------------|
| `first()` | Get first element or null |
| `collect()` | Get all elements as List |
| `collect(Collector)` | Collect with custom collector |
| `isEmpty()` | Check if empty |
| `count()` | Get count |

### Aggregation

| Method | Description |
|--------|-------------|
| `aggregate(Function<Stream<T>, R>)` | Apply custom aggregation to stream |

Source: `api/src/main/java/com/tonic/queries/abstractions/AbstractQuery.java`

---

## Common Mistakes

### Not checking for null

**Wrong:**
```java
NpcEx banker = new NpcQuery().withName("Banker").first();
NpcAPI.interact(banker, "Bank");  // NPE if banker is null
```

**Correct:**
```java
NpcEx banker = new NpcQuery().withName("Banker").first();
if (banker != null) {
    NpcAPI.interact(banker, "Bank");
}
```

### Using collect when first is sufficient

**Wrong:**
```java
List<NpcEx> bankers = new NpcQuery().withName("Banker").collect();
if (!bankers.isEmpty()) {
    NpcAPI.interact(bankers.get(0), "Bank");
}
```

**Correct:**
```java
NpcEx banker = new NpcQuery().withName("Banker").first();
if (banker != null) {
    NpcAPI.interact(banker, "Bank");
}
```

### Case sensitivity with names

**Wrong:**
```java
new NpcQuery().withName("BANKER")  // May not match "Banker"
```

**Correct:**
```java
new NpcQuery().withName("Banker")        // Exact match (case-insensitive)
new NpcQuery().withNameContains("bank")  // Partial match (case-insensitive)
```

### Forgetting distance filters

**Wrong:**
```java
// May return NPC on other side of map
NpcEx target = new NpcQuery().withName("Goblin").first();
```

**Correct:**
```java
NpcEx target = new NpcQuery()
    .withName("Goblin")
    .withinDistance(15)
    .nearest();
```

---

## Performance Tips

1. **Add specific filters first** - Reduce the dataset early
2. **Use ID filters when possible** - Faster than name matching
3. **Limit distance** - Avoid searching entire game world
4. **Use `first()` instead of `collect().get(0)`** - Short-circuits search

```java
// Good: ID filter first, then distance
NpcEx npc = new NpcQuery()
    .withIds(2633, 2634)  // Filter by ID first
    .withinDistance(10)    // Then by distance
    .nearest();

// Less efficient: Distance filter first
NpcEx npc = new NpcQuery()
    .withinDistance(10)
    .withIds(2633, 2634)
    .nearest();
```
