[SDK index](../SDK-DOCS.md) · [← Automation](automation.md) · [Game data →](game-data.md)

# Navigation

`com.tonic.services.pathfinder`: walking anywhere in the world, including doors, stairs, teleports and transport networks, plus sailing.

## Walking

| Class | What it is |
|---|---|
| `Walker` | Walk to a point or any of a set of areas, with an optional stop condition (waits). Also `sailTo`, `cancel()` and `isWalking()`. Turns run on and drinks stamina along the way. |
| `WalkerPath` | The same walk as an object you advance yourself with `step()` once per tick, so it doesn't block. `HandlerBuilder.walkTo` uses it. |
| `StrategicPathing` | Short paths inside the loaded scene that avoid tiles you mark as dangerous. |
| `LocalPathfinder` | A plain path between two tiles in the loaded scene. |

```java
Walker.walkTo(new WorldPoint(3208, 3220, 2));
```

## Algorithms

The world walker can use one of five algorithms, picked in VitaLite's settings or passed to `WalkerPath`. They are listed in `PathfinderAlgo` (`HYBRID_BFS`, the default, then `BI_DIR_BFS`, `FLOW_FIELD`, `ASTAR` and `JPS`). Each implements `IPathfinder`.

## Transports

`transports` holds the links the pathfinder can take besides walking: doors, stairs, ladders, shortcuts, NPC and item transports, and teleports from `teleports`.

| Class | What it is |
|---|---|
| `TransportLoader` | Loads every transport. You can add temporary ones of your own with `updateTempTransports`. |
| `Transport` / `LongTransport` | One link from a tile to a tile, and one that takes several actions. |
| `transports.data` | Fixed networks: fairy rings, spirit trees, gnome gliders, canoes, minecarts, dwarven carts, charter ships, Barnaby's ship, quetzals. |

`requirements` holds what a transport needs before it's used (`ItemRequirement`, `SkillRequirement`, `QuestRequirement`, `VarRequirement`, `WorldRequirement`, `OtherRequirement`), combined in `Requirements`.

The settings panel has a Transport Editor for adding and editing transports.

## Sailing

`com.tonic.services.pathfinder.sailing`: voyages for the player's boat. Routes are planned a server tick at a time with the boat's real speed, turning and hull, on a thread of their own, and followed tick by tick, replanning when the boat strays or something gets in the way (other boats, sharks, lightning, rapids, fetid pools).

| Class | What it is |
|---|---|
| `BoatPathing` | Voyages as step handlers: `travelTo(point)` sails there and stops, `travelTo(port)` sails to a `PortLocation` and docks, `board(port)` walks to the gangplank and boards (recovering the boat first if needed), `dock(port)` docks from nearby. Also `cancel()`, `isSailing()`, `getState()`, `getFailure()`, and `findFullPath` for a route's tiles. |
| `Sailing` | The voyage service behind it: the boat's pose, hull, speed and throttle, the route being followed, recent plans. |
| `BoatCollisionAPI` | Your boat, its tile, and the tiles its collision and deck cover. |

`Walker.sailTo(point)`, `Walker.sailTo(port)`, `Walker.boardBoat(port)` and `Walker.dockAt(port)` do the same and (wait). Handler builders have `sailTo`, `boardBoat` and `dockAt` steps. `PortLocation` lists every dock you can sail to.

[SDK index](../SDK-DOCS.md) · [← Automation](automation.md) · [Game data →](game-data.md)
