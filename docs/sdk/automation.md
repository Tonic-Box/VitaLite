[SDK index](../SDK-DOCS.md) · [← Queries and wrappers](queries-and-wrappers.md) · [Navigation →](navigation.md)

# Automation

Ways to write multi-step tasks that don't block the client thread.

## Step handlers

`com.tonic.util.handler`: a task split into numbered steps. Each step runs once, then says which step comes next. A finished handler is a `StepHandler`. Call `step()` once per game tick to advance it without blocking, or `execute()` (waits) to run it to the end on a worker thread.

| Class | What it is |
|---|---|
| `StepHandler` | The runnable task: `step()`, `execute()`, `reset()`. |
| `StepContext` | A key-value store shared by a handler's steps. |
| `HandlerBuilder` | Builds a handler from steps you number yourself, with delays, wait-until steps, walking and appending other handlers. |
| `AbstractHandlerBuilder` | The same with automatic step numbers and optional labels to jump to. The builders below extend it. |

```java
StepHandler trip = BankBuilder.get()
        .open()
        .depositInventory()
        .withdraw(false, BankBuilder.BankItem.of(ItemID.LOBSTER, 10))
        .build();
```

### Builders

`com.tonic.api.handlers`: ready-made steps for common tasks. Each has a static `get()`, and they can be chained and appended to each other.

| Class | What it is |
|---|---|
| `GenericHandlerBuilder` | An empty builder for your own steps. |
| `BankBuilder` | Open a bank (optionally a given one), deposit inventory or equipment, withdraw, deposit. |
| `DialogueBuilder` | Get through dialogue, choosing options. |
| `GrandExchangeHandler` | Open the Grand Exchange, collect, buy, sell. |
| `InventoryBuilder` | Use, drop and interact with inventory items. |
| `NpcBuilder` | Walk to an NPC, talk to it, attack it, trade with it. |
| `PlayerBuilder` | Interact with, follow, attack or trade a player. |
| `TileItemBuilder` | Pick up ground items. |
| `TileObjectBuilder` | Walk to an object and interact with it. |
| `TradeBuilder` | Trade a player and offer given items. |

## Script DSL

`com.tonic.util.handler.script`: write a task as a script (actions, waits, loops, conditions, variables, subroutines) and it compiles to a `StepHandler`. `Script.build(...)` returns the handler and `Script.execute(...)` (waits) runs it. `IScript` lets you write a script as a class. See [Script DSL](../SCRIPT-DSL.md).

## Loadouts

`com.tonic.api.loadouts`: describe the items you need, then check or fix what you have against that.

| Class | What it is |
|---|---|
| `InventoryLoadout` | Items the inventory should hold. Says what's missing, extra or foreign. |
| `EquipmentLoadout` | Items to wear, by slot. |
| `IdLoadoutItem` / `NamedLoadoutItem` | One required item, matched by ids or names, with amounts, noted, stackable and optional settings. |
| `LoadoutItem` / `Loadout` | The base classes for the above. |
| `ItemDepletionListener` | Called when the bank runs out of an item the loadout needs. |

Loadouts don't do anything on their own. `BankAPI.withdraw(loadout)` moves the inventory towards one a few actions at a time, and `EquipmentAPI.equip(loadout)` puts on what it can.

## Dialogue trees

`com.tonic.util.DialogueNode`: a tree of dialogue options to pick in order. `process()` (waits) runs it through, and `processStep()` does one step per call.

[SDK index](../SDK-DOCS.md) · [← Queries and wrappers](queries-and-wrappers.md) · [Navigation →](navigation.md)
