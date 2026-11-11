[SDK index](../SDK-DOCS.md) · [← Getting started](getting-started.md) · [Queries and wrappers →](queries-and-wrappers.md)

# Game API

Static classes for reading the game and acting in it. Calls are instant unless marked "(waits)"; see [Threading](getting-started.md#threading).

## Entities

`com.tonic.api.entities`: finding and interacting with things in the world. Each `search()` returns a [query](queries-and-wrappers.md).

| Class | What it is |
|---|---|
| `NpcAPI` | Search NPCs and interact with them by option index or action name. |
| `PlayerAPI` | Search other players and interact with them. |
| `TileItemAPI` | Search ground items and interact with them (take, and so on). |
| `TileObjectAPI` | Search objects and interact with them; shortcuts to get one by name or id. |
| `ActorAPI` | Base of `NpcAPI` and `PlayerAPI`. Its one method is deprecated in favour of `ActorEx.getInCombatWith()`. |

## Game

`com.tonic.api.game`: game state that isn't tied to one interface.

| Class | What it is |
|---|---|
| `CameraAPI` | Camera yaw, pitch and speed; turn, tilt, face north, look at a tile. |
| `ClientScriptAPI` | Run client scripts, such as closing an interface or switching tabs. Scripts it runs may send packets, as the game's own do. |
| `CombatAPI` | Hitpoints, special attack, attack style, auto retaliate, poison, venom, antifire, teleblock, multi-combat and PvP area checks. |
| `GameAPI` | Logged-in and login-screen state, logout, wilderness level, cutscenes, membership days, raw menu actions. |
| `HouseAPI` | Player-owned house locations and where you leave the house. |
| `HouseOptionsAPI` | House settings: building mode, teleport inside, door mode. |
| `MovementAPI` | Run energy, run toggle, stamina, whether you're moving, and walk clicks. Each walk call sends one click; it doesn't wait to arrive. For long walks use the [Walker](navigation.md). |
| `QuestAPI` | Quest state: not started, in progress, completed. |
| `SceneAPI` | The loaded scene: tiles, reachable tiles, local paths, line of sight. |
| `reach.ReachRule` | The server's rule for which tiles an object or actor can be interacted with from. `reach.SceneCollision` reads a world view's collision by scene tile; `reach.Reachability` holds the per-tick reachable tiles. See [interaction tiles](queries-and-wrappers.md#interaction-tiles-and-reachability). |
| `SkillAPI` | Real and boosted levels, experience, level and XP maths. |
| `VarAPI` | Read and set varbits, varps and client vars. |
| `WorldsAPI` | Current world, members check, world queries. Its `hop` methods return a `StepHandler` to run rather than hopping directly. |

### Sailing

`com.tonic.api.game.sailing`

| Class | What it is |
|---|---|
| `SailingAPI` | Sails, navigation, heading, trimming and boat state. Each call makes one adjustment. |
| `BoatStatsAPI` | Your boat's resistances and health, while on board. |
| `NoticeBoardAPI` | Sailing notice-board tasks: best, available and accepted ones; open and close the board. |
| `Heading` | Boat headings, including the best heading towards a point. |
| `MoveMode` | How the sails are set: stopped, slow, full, reverse or moored. |
| `SailingTab` | The sailing side-panel tabs. |
| `SailingRenderUtil` | Drawing the boat's exact hull, deck, route, predicted track, the sea map and the nav graph in overlays. |

## Widgets

`com.tonic.api.widgets`: one class per interface.

| Class | What it is |
|---|---|
| `BankAPI` | Bank state, withdraw and deposit by id, name or item, noted mode, deposit all, tabs. `withdraw(InventoryLoadout)` brings the inventory towards a [loadout](automation.md#loadouts) a bit each call. |
| `DepositBoxAPI` | Deposit box: deposit all, worn items, looting bag, single items. |
| `DialogueAPI` | Dialogue text and options; continue, pick an option, answer number and item prompts. One step per call. |
| `EmoteAPI` | Perform emotes. |
| `EquipmentAPI` | Worn items: search, check, equip, unequip, equip a loadout. |
| `GrandExchangeAPI` | Grand Exchange offers: start buy and sell, collect, abort. `cancel()` and `startBuyOfferPercentage()` (waits). |
| `InventoryAPI` | Inventory items: search, interact, use on things, drag, drop, counts. |
| `MagicAPI` | Cast spells, autocast state, home teleport cooldown. |
| `MakeXAPI` | The "make how many" interface. |
| `MiniMapAPI` | Drawing paths and points on the minimap. |
| `PrayerAPI` | An enum of prayers with on, off and toggle, plus quick prayers, flicking and best-prayer helpers. |
| `SettingsAPI` | Read any setting, and change it in the open settings menu with every step sent in the same tick: toggles, dropdowns, key bindings, stepped sliders, numbers, colours and action buttons. Volumes are read only. |
| `ShopAPI` | Buy and sell in shops, and shop stock. `sellX` (waits) between sells of non-stackable items. |
| `SlayerRewardsAPI` | Slayer rewards interface and points. |
| `SmithingAPI` | Anvil interface: smith an item, bar and hammer checks. |
| `TabsAPI` | Open side tabs. |
| `TanningAPI` | Tanner interface: tan hides, costs. |
| `TradeAPI` | Trade screens: offer, remove, accept, decline, what each side offers. |
| `WidgetAPI` | Any widget: search, read, interact, drag, use on things, close interfaces. |
| `WorldMapAPI` | World map coordinates and drawing paths and markers on it. |

## Threaded

`com.tonic.api.threaded`: whole tasks that wait between steps. All of these (wait), so call them from a `VitaPlugin` loop or another worker thread.

| Class | What it is |
|---|---|
| `Delays` | Sleep a number of ticks or milliseconds, or until a condition, idle or a tile. |
| `Dialogues` | Click through dialogue, picking given options along the way. |
| `GrandExchange` | Full buy and sell flows, raising the price on retries. |
| `Cannon` | Place, reload, repair and pick up a dwarf multicannon, or keep it running. |
| `Minigames` | Minigame group teleports. |
| `Settings` | Open the settings menu and change a setting, waiting until the game shows the new value; returns a `SettingResult`. |

## In-game settings

The game's settings menu (the "All settings" window) can be read and changed from code. Every setting has a typed key in `com.tonic.data.settings.SettingID`, one nested class per menu tab:

```java
SettingID.Activities.TILE_HIGHLIGHTING             // ToggleSetting
SettingID.Activities.NPC_HIGHLIGHT_DISPLAY_NAME    // DropdownSetting
SettingID.Activities.NpcHighlightDisplayName.ON_BOLD
SettingID.Controls.COMBAT_TAB_KEYBIND              // KeyBindingSetting
SettingID.KeyBindings.F1
SettingID.Audio.MASTER_VOLUME                      // SliderSetting
```

`SettingID` is generated from the game cache, so a setting the game adds shows up once the file is regenerated. The menu's layout is read from the live client, so keys keep working when the game reorders the menu.

**Reading** works any time, menu open or not, through `SettingsAPI`: `isEnabled` for toggles, and `get` for sliders, dropdowns, key bindings, numbers and colours. `isAvailable` and `isEditable` tell whether the menu lists a setting and whether the game lets it be changed right now (some settings, such as tile highlighting, exist only on the official clients and are never listed here); `getBlockedMessage` gives the reason the game shows when it can't be.

**Changing** comes in two forms:

| | What it does |
|---|---|
| `SettingsAPI` | Needs the menu open (`SettingsAPI.open()` opens it). Sends the whole change in the same tick: the tab, the row and, for dropdowns, key bindings, numbers and colours, the follow-up. Returns `true` when the change was sent or the setting already had the value, `false` when nothing was sent (menu closed, setting blocked or not listed). |
| `Settings` (threaded) | Opens the menu if needed, sends the change through `SettingsAPI`, then (waits) until the game shows the new value. Returns a `SettingResult`: `DONE`, `BLOCKED`, `NOT_AVAILABLE`, `NOT_OPEN`, `NOT_CONFIRMED` or `NOT_IN_GAME`. Leaves the menu open. |

```java
SettingResult result = Settings.set(SettingID.Activities.TILE_HIGHLIGHTING, true);
Settings.select(SettingID.Activities.NpcHighlightDisplayName.ON_BOLD);
Settings.set(SettingID.Controls.COMBAT_TAB_KEYBIND, SettingID.KeyBindings.F1);
```

Accept aid (`SettingID.Gameplay.ACCEPT_AID`) is changed with its button on the settings side panel, so it needs no menu, and `SettingsAPI.setRun` / `Settings.setRun` turn run on or off the same way.

Toggles, dropdowns, key bindings, stepped sliders (by step, from 0), numbers, colours (`0xRRGGBB`) and action buttons can be changed. `SettingsAPI.run` only presses an action button; `Settings.run` also answers its confirmation. Volumes can be read (0 to 100) but not changed, and smooth sliders other than the volumes, such as camera zoom, and the loot tracker's list editors aren't supported.

[SDK index](../SDK-DOCS.md) · [← Getting started](getting-started.md) · [Queries and wrappers →](queries-and-wrappers.md)
