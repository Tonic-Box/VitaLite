[SDK index](../SDK-DOCS.md) · [← Navigation](navigation.md) · [Services →](services.md)

# Game data

`com.tonic.data`: enums and constants describing game content, some with helpers attached.

## General

| Class | What it is |
|---|---|
| `AccountType` | Normal, ironman and the other account types. |
| `AttackStyle` | Combat attack styles. |
| `EmoteID` | Emotes, each with `perform()`. |
| `EquipmentSlot` | Equipment slots. |
| `GrandExchangeSlot` | The eight Grand Exchange slots and their offer state. |
| `ItemConstants` | Groups of item ids for items with several variants, such as charged and uncharged. |
| `LayoutView` | The main interface parts and their ids in each client layout. |
| `Orientation` | Directions an entity can face. |
| `RegionInfo` | Named regions (bosses, minigames, cities and so on) by region id. |
| `SailingConstants` | Sailing ids and values. |
| `StrongholdSecurityQuestion` | Stronghold of Security door questions and answers. |
| `settings.SettingID` | Every setting in the settings menu as a typed key, generated from the cache, with each dropdown's options and the keys a key binding can take. Used by `SettingsAPI` and `Settings`. |
| `Tab` | Side tabs, used by `TabsAPI`. |
| `VarrockMuseumAnswer` | Varrock Museum quiz answers. |
| `Walls` | The wall objects on and around a tile. Reach doesn't use it; see [interaction tiles](queries-and-wrappers.md#interaction-tiles-and-reachability). |
| `WorldLocation` | Named areas of the world. |

## Magic

`com.tonic.data.magic`

| Class | What it is |
|---|---|
| `Spell` | The interface every spell implements. |
| `spellbooks.Standard`, `Ancient`, `Lunar`, `Necromancy` | The spells of each spellbook. |
| `SpellBook` | Which spellbook is active. |
| `MagicCast` | Whether a spell can be cast with the runes you have. |
| `Rune` / `RuneRequirement` | Runes and a spell's rune cost. |
| `RunePouch` | Rune pouch contents and actions. |

## Other skills

| Package | What it is |
|---|---|
| `com.tonic.data.runecrafting` | `EssencePouch` tracks each essence pouch's fill and `PouchType` lists them. |
| `com.tonic.data.smithing` | What each bar can be smithed into (`BronzeItem` up to `RuneItem`), `BarType`, ore needs, smelting modifiers. |
| `com.tonic.data.slayerrewards` | The slayer rewards tabs (`BuyTab`, `ExtendTab`, `TasksTab`, `UnlockTab`): buy, extend, cancel or block tasks. |

## Shops

`com.tonic.data.trading`: `Shop` lists the game's shops with their shopkeeper, location and access requirements (`ShopRequirements`). Search it with `ShopQuery`.

## Locations

`com.tonic.data.locatables`

| Class | What it is |
|---|---|
| `AreaLocations` | Named areas and whether you're in one. |
| `BankLocations` | Banks, the nearest one and a path to it. `walkToNearest()` (waits). |
| `NpcLocations` | Where common NPCs stand. `interact`, `talkTo`, `attack` and `trade` walk there first (waits). |

### Sailing locations

`com.tonic.data.locatables.sailing`

| Class | What it is |
|---|---|
| `BoatBoardingAPI` | The boat boarding interface. |
| `CargoHoldAPI` | Your boat's cargo hold: open, list items, withdraw crates. |
| `NoticeBoardPosting` | Each notice-board task: accept, cancel, level, and a path to do it. |
| `PortLocation` / `PortPaths` | Ports and the sailing routes between them. |
| `CourierTaskData` / `PortTaskTrigger` | Courier and port task data. |

## Ids

`base-api` ships RuneLite's id constants in `net.runelite.api` (`ItemID`, `NpcID`, `ObjectID`, `Varbits` and the `gameval` set).

[SDK index](../SDK-DOCS.md) · [← Navigation](navigation.md) · [Services →](services.md)
