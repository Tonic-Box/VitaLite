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
| `AbstractActorQuery` | Base for NPC and player queries: name, attackable, reachable (a tile it can be interacted from can be walked to), line of sight, within a distance, sort by distance or path, `nearest()`. |
| `NpcQuery` | NPCs by id, name, action, alive, area. `walkable()` is the same as `isReachable()`. |
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
| `ActorEx` | Base of NPCs and players: interacting target, idle, dead, combat level, overhead icon, who it's fighting, orientation, reachability, where to interact from. |
| `NpcEx` | An NPC: id, definition, health. |
| `PlayerEx` | A player. `PlayerEx.getLocal()` is you. |
| `ItemEx` | An item in the inventory, bank or equipment: noted, placeholder, tradeable, prices, actions. |
| `ItemContainerEx` | An item container by id: all items, first match, next empty slot. |
| `TileItemEx` | A ground item, with its prices. |
| `TileObjectEx` | An object: actions, definition, reachability, where to interact from, shape, orientation. |

### Interaction tiles and reachability

Objects and actors answer where they can be interacted with from, by the server's rule (`ReachRule` in `com.tonic.api.game.reach`):

| Method | What it gives |
|---|---|
| `interactableFrom()` | The tiles it can be used from that can be stood on, reachable or not, in its own world view. |
| `reachedFrom(WorldPoint)` | Whether one tile is such a tile. |
| `getInteractionPoint()` | The nearest such tile to you, reachable or not; null when there is none. Your own player's is its own tile. |
| `isReachable()` | Whether you can walk now to one of them. False when it's in another world view than you (on a boat or off it). |

The rule by kind of thing:
- **Walls and doors:** their own tile, the tile across them, and the tiles along them that the wall tile can step out to.
- **Wall decorations:** their own tile and the tiles their shape faces.
- **Objects with a footprint:** inside it, or straight beside a side its definition leaves open (bank booths, ranges and furnaces have one or two) with no wall on the edge between. Never diagonally.
- **NPCs and players:** straight beside their footprint with no wall between, or two tiles out across a counter, a tile that blocks walking but not line of sight.

"Reachable" everywhere means the same thing: a flood over your own world view's live collision (doors as they are now), worked out once per tick. `GameManager.isReachable`, `SceneAPI.reachableTiles`, `LocationQuery.isReachable()` and every wrapper's `isReachable()` use it.

The interfaces in `com.tonic.data.wrappers.abstractions` tie these together. `Entity` combines `Locatable` (position and distance), `Interactable` (actions) and `Identifiable` (id and name).

[SDK index](../SDK-DOCS.md) · [← Game API](game-api.md) · [Automation →](automation.md)
