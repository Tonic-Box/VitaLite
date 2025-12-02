# Script DSL - Coroutine-Style StepHandler

The Script DSL provides a yield/await-style syntax for building `StepHandler` instances. It eliminates manual step numbering, magic string context keys, and repetitive boilerplate while compiling down to the same step-based execution model.

## Quick Start

```java
import com.tonic.util.handler.script.*;

// Build a handler
StepHandler handler = Script.build(s -> {
    s.action(() -> openBank());
    s.await(BankAPI::isOpen);
    s.action(() -> BankAPI.depositInventory());
});

// Execute it
handler.execute();

// Or build and execute in one call
Script.execute(s -> {
    s.action(() -> openBank());
    s.await(BankAPI::isOpen);
});
```

## Core Concepts

### Actions
Actions execute code and automatically proceed to the next step:

```java
// Simple action
s.action(() -> NpcAPI.interact(banker, "Bank"));

// Action with context access
s.action(ctx -> {
    NpcEx npc = findNearestBanker();
    if (npc != null) {
        NpcAPI.interact(npc, "Bank");
        ctx.set(FOUND_BANKER, true);
    }
});

// Action that stores a result
s.action(BANKER, () -> new NpcQuery().withName("Banker").first());
```

### Yield / Await
Pause execution for ticks or until conditions:

```java
// Pause for 1 tick
s.yield();

// Pause for N ticks
s.yield(5);

// Wait until condition is true
s.await(BankAPI::isOpen);
s.await(() -> InventoryAPI.isFull());

// Wait with context access
s.await(ctx -> ctx.get(TARGET) != null);

// Wait with timeout
s.await(100, BankAPI::isOpen, () -> {
    System.out.println("Bank didn't open in time!");
});
```

### Type-Safe Context Variables

Replace magic strings with typed `Var<T>` keys:

```java
// Define variables (typically as static fields for sharing)
static final Var<NpcEx> TARGET = Var.of("target", NpcEx.class);
static final Var<Integer> COUNT = Var.intVar("count");  // Default 0
static final Var<Boolean> DONE = Var.boolVar("done");   // Default false
static final Var<WorldPoint> DEST = Var.of("dest", WorldPoint.class);

// Use in scripts
Script.execute(s -> {
    s.action(TARGET, () -> NpcAPI.search().withName("Guard").first());
    s.exitIf(ctx -> ctx.get(TARGET) == null);

    s.action(ctx -> {
        ctx.set(COUNT, ctx.get(COUNT) + 1);
        NpcAPI.attack(ctx.get(TARGET));
    });

    s.await(ctx -> ctx.get(TARGET).isDead());
});
```

### Script-Local Variables

For variables scoped to a single script instance (not shared across calls):

```java
Script.execute(s -> {
    // Object types
    var target = s.var("target", NpcEx.class);
    var destination = s.var("dest", WorldPoint.class);

    // Primitives with defaults
    var count = s.varInt("count");           // default 0
    var count5 = s.varInt("count", 5);       // default 5
    var done = s.varBool("done");            // default false
    var name = s.varString("name");          // default ""
    var timer = s.varLong("timer");          // default 0L
    var ratio = s.varDouble("ratio", 0.5);   // default 0.5

    s.action(ctx -> {
        ctx.set(count, ctx.get(count) + 1);
        ctx.set(target, findTarget());
    });

    s.await(ctx -> ctx.get(target) != null);
});
```

**When to use which:**
- **Static `Var.of()`**: Shared across multiple script instances, reusable keys
- **`s.var()`**: Self-contained to one script, no namespace collisions

### Branching

```java
// Simple conditional (skip if false)
s.when(() -> shouldDeposit(), body -> {
    body.action(() -> BankAPI.depositInventory());
});

// If-then
s.ifThen(() -> inventoryFull(), body -> {
    body.action(() -> walkToBank());
    body.await(BankAPI::isOpen);
    body.action(() -> BankAPI.depositInventory());
});

// If-then-else
s.ifThenElse(
    () -> hasPickaxe(),
    thenBranch -> {
        thenBranch.action(() -> mine());
    },
    elseBranch -> {
        elseBranch.action(() -> getPickaxeFromBank());
    }
);

// With context access
s.when(ctx -> ctx.get(TARGET) != null, body -> {
    body.action(ctx -> NpcAPI.interact(ctx.get(TARGET), "Talk-to"));
});
```

### Loops

```java
// Loop N times
s.loop(5, body -> {
    body.action(() -> performAction());
    body.yield();
});

// Loop while condition is true
s.loopWhile(() -> !inventoryFull(), body -> {
    body.action(() -> mine());
    body.await(PlayerAPI::isIdle);
});

// Loop until condition becomes true
s.loopUntil(InventoryAPI::isFull, body -> {
    body.action(() -> fish());
    body.await(PlayerAPI::isIdle);
});

// Infinite loop (use exit() to break)
s.loop(body -> {
    body.action(() -> doWork());
    body.exitIf(() -> shouldStop());
    body.yield();
});
```

### Labels and Jumps

```java
s.label("start");
s.action(() -> tryOpenDoor());
s.yield(2);

s.jumpIf(() -> !doorIsOpen(), "start");  // Retry if failed

s.action(() -> walkThrough());
```

### Exit

```java
// Exit immediately
s.exit();

// Exit if condition is true
s.exitIf(() -> playerDied());
s.exitIf(ctx -> ctx.get(ATTEMPTS) >= 5);
```

### Composition

```java
// Include another script inline
Consumer<ScriptBuilder> depositItems = sub -> {
    sub.await(BankAPI::isOpen);
    sub.action(() -> BankAPI.depositInventory());
};

Script.execute(s -> {
    s.action(() -> openBank());
    s.include(depositItems);
    s.action(() -> BankAPI.close());
});

// Include an existing StepHandler
StepHandler walkingHandler = BankBuilder.get().open().build();

Script.execute(s -> {
    s.include(walkingHandler);
    s.action(() -> BankAPI.depositInventory());
});
```

## Complete Examples

### Example 1: Banking

```java
static final Var<Boolean> FOUND_BANK = Var.boolVar("foundBank");

public StepHandler createBankingHandler() {
    return Script.build(s -> {
        // Early exit if already open
        s.exitIf(BankAPI::isOpen);

        // Try nearby banker
        s.action(ctx -> {
            NpcEx banker = new NpcQuery()
                .withNameContains("Banker")
                .nearest();
            if (banker != null) {
                NpcAPI.interact(banker, "Bank");
                ctx.set(FOUND_BANK, true);
            }
        });
        s.jumpIf(ctx -> ctx.get(FOUND_BANK), "wait_open");

        // Try bank booth
        s.action(ctx -> {
            TileObjectEx booth = TileObjectAPI.search()
                .withNameContains("Bank booth")
                .first();
            if (booth != null) {
                TileObjectAPI.interact(booth, "Bank");
                ctx.set(FOUND_BANK, true);
            }
        });
        s.jumpIf(ctx -> ctx.get(FOUND_BANK), "wait_open");

        // Walk to nearest bank
        s.action(() -> MovementAPI.walkTo(BankLocations.getNearest()));
        s.await(PlayerAPI::isIdle);

        // Wait for bank to open
        s.label("wait_open");
        s.await(BankAPI::isOpen);
    });
}
```

### Example 2: Mining with Banking

```java
static final Var<Integer> ORES_MINED = Var.intVar("oresMined");
static final Var<TileObjectEx> ROCK = Var.of("rock", TileObjectEx.class);

public void mineWithBanking(WorldPoint miningSpot, int targetOres) {
    Script.execute(s -> {
        s.loopUntil(ctx -> ctx.get(ORES_MINED) >= targetOres, loop -> {

            // Mine until inventory full
            loop.loopUntil(InventoryAPI::isFull, mineLoop -> {
                mineLoop.action(ROCK, () -> TileObjectAPI.search()
                    .withNameContains("rocks")
                    .nearest());

                mineLoop.when(ctx -> ctx.get(ROCK) != null, mine -> {
                    mine.action(ctx -> TileObjectAPI.interact(ctx.get(ROCK), "Mine"));
                    mine.await(PlayerAPI::isIdle);
                    mine.action(ctx -> ctx.set(ORES_MINED, ctx.get(ORES_MINED) + 1));
                });

                mineLoop.yield();
            });

            // Bank the ores
            loop.action(() -> MovementAPI.walkTo(BankLocations.getNearest()));
            loop.await(() -> BankAPI.isNearBank());
            loop.action(() -> BankAPI.open());
            loop.await(BankAPI::isOpen);
            loop.action(() -> BankAPI.depositInventory());
            loop.yield(2);
            loop.action(() -> BankAPI.close());

            // Return to mining spot
            loop.action(() -> MovementAPI.walkTo(miningSpot));
            loop.await(PlayerAPI::isIdle);
        });
    });
}
```

### Example 3: NPC Dialogue

```java
static final Var<NpcEx> NPC = Var.of("npc", NpcEx.class);

public StepHandler talkToNpc(String npcName, String... dialogueOptions) {
    return Script.build(s -> {
        // Find and interact
        s.action(NPC, () -> NpcAPI.search().withName(npcName).first());
        s.exitIf(ctx -> ctx.get(NPC) == null);

        s.action(ctx -> NpcAPI.interact(ctx.get(NPC), "Talk-to"));
        s.await(DialogueAPI::dialoguePresent);

        // Process dialogue options
        s.loopWhile(DialogueAPI::dialoguePresent, loop -> {
            loop.action(() -> {
                for (String option : dialogueOptions) {
                    if (DialogueAPI.hasOption(option)) {
                        DialogueAPI.selectOption(option);
                        return;
                    }
                }
                DialogueAPI.continueDialogue();
            });
            loop.yield(2);
        });
    });
}
```

### Example 4: Retry Pattern

```java
static final Var<Integer> ATTEMPTS = Var.intVar("attempts");

public StepHandler retryableAction(int maxAttempts) {
    return Script.build(s -> {
        s.label("retry");

        s.action(ctx -> ctx.set(ATTEMPTS, ctx.get(ATTEMPTS) + 1));
        s.exitIf(ctx -> ctx.get(ATTEMPTS) > maxAttempts);

        s.action(() -> {
            TileObjectEx door = TileObjectAPI.search()
                .withName("Door")
                .withAction("Open")
                .first();
            if (door != null) {
                TileObjectAPI.interact(door, "Open");
            }
        });

        s.yield(3);
        s.jumpIf(() -> !isDoorOpen(), "retry");
    });
}
```

## Migration from HandlerBuilder

### Before (HandlerBuilder)

```java
int step = currentStep;
add(context -> {
    if (BankAPI.isOpen()) {
        return jump("end", context);
    }
    NpcEx banker = new NpcQuery().withName("Banker").first();
    if (banker != null) {
        NpcAPI.interact(banker, "Bank");
        return step + 2;
    }
    return step + 1;
});
addDelayUntil(() -> PlayerAPI.isIdle());
add("end", context -> { /* ... */ });
```

### After (Script DSL)

```java
Script.build(s -> {
    s.exitIf(BankAPI::isOpen);

    s.action(ctx -> {
        NpcEx banker = new NpcQuery().withName("Banker").first();
        if (banker != null) {
            NpcAPI.interact(banker, "Bank");
            ctx.set(FOUND, true);
        }
    });
    s.jumpIf(ctx -> ctx.get(FOUND), "end");

    s.await(PlayerAPI::isIdle);

    s.label("end");
});
```

## API Reference

### Script (Entry Point)

| Method | Description |
|--------|-------------|
| `Script.build(Consumer<ScriptBuilder>)` | Builds a StepHandler without executing |
| `Script.execute(Consumer<ScriptBuilder>)` | Builds and executes immediately |
| `Script.run(Consumer<ScriptBuilder>)` | Alias for build() |

### ScriptBuilder Methods

| Category | Methods |
|----------|---------|
| **Actions** | `action(Runnable)`, `action(Consumer<ScriptContext>)`, `action(Var<T>, Supplier<T>)` |
| **Yield/Await** | `yield()`, `yield(int)`, `await(BooleanSupplier)`, `await(Predicate<ScriptContext>)`, `await(int, BooleanSupplier, Runnable)` |
| **Branching** | `when()`, `ifThen()`, `ifThenElse()` |
| **Loops** | `loop(Consumer)`, `loop(int, Consumer)`, `loopWhile()`, `loopUntil()` |
| **Control** | `exit()`, `exitIf()`, `label(String)`, `jump(String)`, `jumpIf()` |
| **Subroutines** | `sub(name, body)`, `call(name)`, `ret()` |
| **Composition** | `include(Consumer<ScriptBuilder>)`, `include(StepHandler)` |
| **Context** | `get(Var<T>)`, `set(Var<T>, T)` |
| **Local Vars** | `var(name, type)`, `var(name, type, default)`, `varInt()`, `varBool()`, `varLong()`, `varDouble()`, `varString()` |

### Var Factory Methods (Static)

| Method | Description |
|--------|-------------|
| `Var.of(name, type)` | Creates a Var with no default |
| `Var.of(name, type, default)` | Creates a Var with a default value |
| `Var.intVar(name)` | Integer var with default 0 |
| `Var.boolVar(name)` | Boolean var with default false |
| `Var.stringVar(name)` | String var with default "" |

### Script-Local Variable Methods

| Method | Description |
|--------|-------------|
| `s.var(name, type)` | Script-local object var, no default |
| `s.var(name, type, default)` | Script-local object var with default |
| `s.varInt(name)` | Script-local int, default 0 |
| `s.varInt(name, default)` | Script-local int with default |
| `s.varBool(name)` | Script-local boolean, default false |
| `s.varBool(name, default)` | Script-local boolean with default |
| `s.varLong(name)` | Script-local long, default 0L |
| `s.varLong(name, default)` | Script-local long with default |
| `s.varDouble(name)` | Script-local double, default 0.0 |
| `s.varDouble(name, default)` | Script-local double with default |
| `s.varString(name)` | Script-local string, default "" |
| `s.varString(name, default)` | Script-local string with default |

### Subroutines

Subroutines are reusable blocks of steps that share the caller's context. They execute when called and return to the call site when complete.

```java
Script.build(s -> {
    var action = s.varString("action");

    // Define subroutines (skipped during normal execution)
    s.sub("interactTable", sub -> {
        sub.action(() -> {
            TileObjectEx table = TileObjectAPI.search()
                    .withName("Ledger table").nearest();
            if (table != null) table.interact(sub.get(action));
        });
        sub.await(() -> PlayerEx.getLocal().isIdle());
    });

    s.sub("boardBoat", sub -> {
        sub.action(() -> {
            TileObjectEx plank = TileObjectAPI.search()
                    .withName("Gangplank").withAction("Board").nearest();
            if (plank != null) plank.interact("Board");
        });
        sub.await(SailingAPI::isOnBoat);
        sub.yield(3);
    });

    // Main execution
    s.action(ctx -> ctx.set(action, "Take-cargo"));
    s.call("interactTable");  // Jumps to sub, returns here

    s.call("boardBoat");

    // ... later ...

    s.action(ctx -> ctx.set(action, "Deposit-cargo"));
    s.call("interactTable");  // Reuse with different action
});
```

| Method | Description |
|--------|-------------|
| `s.sub(name, body)` | Define a named subroutine |
| `s.call(name)` | Call subroutine, return after completion |
| `s.ret()` | Explicit early return from subroutine |

**Key features:**
- Subroutines share context with caller (can read/write all variables)
- Nested calls supported via return address stack
- Subroutine bodies are skipped during linear execution
- Implicit return at end of each subroutine
