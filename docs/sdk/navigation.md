[SDK index](../SDK-DOCS.md) · [← Automation](automation.md) · [Game data →](game-data.md)

# Navigation

`com.tonic.services.pathfinder`: walking anywhere in the world, including doors, stairs, teleports and transport networks, plus sailing.

## Walking

| Class | What it is |
|---|---|
| `Walker` | Walk to a point or any of a set of areas, with an optional stop condition (waits). Also `sailTo`, `cancel()` and `isWalking()`. Turns run on and drinks stamina along the way. |
| `WalkerPath` | The same walk as an object you advance yourself with `step()` once per tick, so it doesn't block. `HandlerBuilder.walkTo` uses it. `WalkerPath.nearby(areas)` plans only when a route inside the loaded scene exists, so an unreachable target is given up on at once; `WalkerPath.get(object)` walks to a tile an object can be used from. |
| `StrategicPathing` | Short paths inside the loaded scene that avoid tiles you mark as dangerous. |

```java
Walker.walkTo(new WorldPoint(3208, 3220, 2));
```

While walking, each click is checked against the walk the scene actually allows: a route tile the player can no longer reach is skipped when a later one still is, and otherwise the door, gate or curtain in the way is opened or the walk is replanned. A tile is clicked once while the player is busy, never more than 17 tiles ahead. A teleport that does not land is tried three times and then left out of replans, and a transport whose requirements stop holding is replanned around. Replans run off the client thread.

Turn on **Trace Walker** in VitaLite's settings to log what the walker does and why.

## Algorithm

The world walker plans with a breadth-first search (`impl.Pathfinder`, an `IPathfinder`) in which each level is one step: transports count two steps per tick they take, and teleports join after 25 steps of walking. Tiles of the loaded scene are planned over the scene's own collision (`impl.LiveLayer`), with doors and gates the walker opens left passable, and inside an instance its tiles are mapped to the world tiles they copy. The rest of the world is planned over the collision map dumped from the cache (`collision.dat`), which every local client shares through one memory-mapped file.

## Transports

`transports` holds the links the pathfinder can take besides walking.

| Class | What it is |
|---|---|
| `TransportLoader` | The transport graph: every transport in `transports.json` plus the code-defined ones, by the tile they leave from. |
| `Transport` | One link: using an object or NPC (`id`, `isObject`) with a 1-based `option` at `location`, answering `dialogueOptions`, lands on `destination` after `duration` ticks. It builds a fresh handler each time it is used. |
| `CodeTransport` | A link too involved for the JSON, defined in code. The networks live in `transports.hardcoded`: fairy rings, quetzals, gnome gliders, spirit trees, minecarts, charter ships, canoes, dwarven carts, Barnaby's ship, magic carpets, the Shantay Pass, trapdoors and more. |

`transports.json` entries look like this:

```json
{
  "name": "Ladder",
  "id": 11794,
  "isObject": true,
  "location": { "worldX": 3202, "worldY": 3435, "plane": 0, "packed": 56282242 },
  "destination": { "worldX": 3202, "worldY": 3435, "plane": 1, "packed": 593153154 },
  "option": 1,
  "dialogueOptions": [],
  "duration": 1
}
```

`requirements` holds what a transport needs before it's used (`ItemRequirement`, `SkillRequirement`, `QuestRequirement`, `VarRequirement`, `WorldRequirement`, `OtherRequirement`), combined in `Requirements`; in the JSON a transport carries an optional `requirements` block. Quests use RuneLite's `Quest` names.

`teleports` holds the teleports a route can start with: spells, items and jewellery. Each waits until the player lands before the walk goes on, and teleports landing in the Wilderness are only used when the target is in it.

## Transport recorder and editor

In a dev run (not from the shaded jar) the **Transport Recorder** sidebar panel records transports as you use them: switch on Record and click an object or NPC option, answer its dialogue, and the transport is staged once you land. Show tiles marks every tile a transport leaves from, and right-clicking one offers "Edit transport". Promote adds staged transports to `transports.json` and the running walker.

The **Transport Editor** (VitaLite's settings, or the recorder panel) edits live and staged transports and their requirements. Saving a live transport updates the running walker and `transports.json` at once.

## Sailing

`com.tonic.services.pathfinder.sailing`: voyages for the player's boat. Routes are planned a server tick at a time with the boat's real speed, turning and hull, on a thread of their own, and followed tick by tick, replanning when the boat strays or something gets in the way (other boats, sharks, lightning, rapids, fetid pools).

| Class | What it is |
|---|---|
| `BoatPathing` | Voyages as step handlers: `travelTo(point)` sails there and stops, `travelTo(port)` sails to a `PortLocation` and docks, `board(port)` walks to the gangplank and boards (recovering the boat first if needed), `dock(port)` docks from nearby. Also `cancel()`, `isSailing()`, `getState()`, `getFailure()`, and `findFullPath` for a route's tiles. |
| `Sailing` | The voyage service behind it: the boat's pose, hull, speed and throttle, the route being followed, recent plans. |
| `BoatCollisionAPI` | Your boat, its tile, and the tiles its collision and deck cover. |

`Walker.sailTo(point)`, `Walker.sailTo(port)`, `Walker.boardBoat(port)` and `Walker.dockAt(port)` do the same and (wait). Handler builders have `sailTo`, `boardBoat` and `dockAt` steps. `PortLocation` lists every dock you can sail to.

[SDK index](../SDK-DOCS.md) · [← Automation](automation.md) · [Game data →](game-data.md)
