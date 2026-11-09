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

The world pathfinder can use one of five algorithms, picked in VitaLite's settings or passed to `WalkerPath`. They are listed in `PathfinderAlgo` (`HYBRID_BFS`, the default, then `BI_DIR_BFS`, `FLOW_FIELD`, `ASTAR` and `JPS`). Each implements `IPathfinder`.

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

| Class | What it is |
|---|---|
| `BoatPathing` | Routes across water. `travelTo(point)` returns a `StepHandler` that sails there. |
| `BoatCollisionAPI` | Your boat's hull, deck and outline, and whether it overlaps an area. |

[SDK index](../SDK-DOCS.md) · [← Automation](automation.md) · [Game data →](game-data.md)
