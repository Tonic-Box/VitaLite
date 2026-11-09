[SDK index](../SDK-DOCS.md) · [← Game API](game-api.md) · [Automation →](automation.md)

# Queries and wrappers

## Queries

`com.tonic.queries`: chainable filters over what's in the game. Start one from an API's `search()` or with `new`, add filters and sorts, then end with a terminal call such as `first()`, `nearest()`, `collect()` or `count()`.

```java
NpcEx goblin = NpcAPI.search().withName("Goblin").alive().nearest();
NpcAPI.interact(goblin, "Attack");
```

Filters and sorts only record what to do. The terminal call runs the whole query on the client thread, so queries are safe from any thread. Entity lists come from a cache that `GameManager` refreshes once per tick.

| Class | What it is |
|---|---|
| `AbstractQuery` | Base of every query: `keepIf`, `removeIf`, `sort`, `limit`, `distinct`, and terminals `first`, `last`, `random`, `collect`, `count`, `isEmpty`, `any`, `all`. |
| `AbstractActorQuery` | Base for NPC and player queries: name, attackable, reachable, line of sight, within a distance, sort by distance or path, `nearest()`. |
| `NpcQuery` | NPCs by id, name, action, alive, area. |
| `PlayerQuery` | Other players. Everything comes from `AbstractActorQuery`. |
| `InventoryQuery` | Items in a container (inventory, bank, equipment, shop) by id, name, action, slot or value, with quantity totals. |
| `TileItemQuery` | Ground items by id, name, price and distance. |
| `TileObjectQuery` | Objects by id, name, action and reachability. |
| `WidgetQuery` | Widgets by id, parent, text, action, item, visibility. |
| `WorldQuery` | Worlds by members, activity, type, region, population, PvP. |
| `LocationQuery` | Tiles: reachable, line of sight, distance, area, has an object. |
| `ShopQuery` | Shops from the `Shop` list, by shopkeeper, name, requirements and distance. |
| `EntityQuery` | NPCs, players, ground items and objects in one query, through the `Entity` interface. |
| `NoticeBoardAPI.NoticeBoardQuery` | Sailing notice-board tasks. |

## Wrappers

`com.tonic.data.wrappers`: objects that wrap RuneLite's game objects. Getters read on the client thread for you, and `interact(...)` goes through the matching API class. Queries return these.

| Class | What it is |
|---|---|
| `ActorEx` | Base of NPCs and players: interacting target, idle, dead, combat level, overhead icon, who it's fighting, orientation. |
| `NpcEx` | An NPC: id, definition, health. |
| `PlayerEx` | A player. `PlayerEx.getLocal()` is you. |
| `ItemEx` | An item in the inventory, bank or equipment: noted, placeholder, tradeable, prices, actions. |
| `ItemContainerEx` | An item container by id: all items, first match, next empty slot. |
| `TileItemEx` | A ground item, with its prices. |
| `TileObjectEx` | An object: actions, definition, reachability, where to interact from, shape, orientation. |

The interfaces in `com.tonic.data.wrappers.abstractions` tie these together. `Entity` combines `Locatable` (position and distance), `Interactable` (actions) and `Identifiable` (id and name).

[SDK index](../SDK-DOCS.md) · [← Game API](game-api.md) · [Automation →](automation.md)
