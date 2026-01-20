# Interaction APIs

VitaLite provides comprehensive APIs for interacting with game entities, widgets, and handling dialogues.

## Overview

Interactions are performed through API classes that wrap packet sending and ensure thread safety.

---

## Entity Interaction

### NpcAPI

Interact with NPCs.

```java
// Interact by action name
NpcEx banker = new NpcQuery().withName("Banker").first();
NpcAPI.interact(banker, "Bank");

// Interact by option number (1-indexed)
NpcAPI.interact(banker, 1);  // First option

// Interact by index
NpcAPI.interact(npcIndex, 1);
```

Source: `api/src/main/java/com/tonic/api/entities/NpcAPI.java`

### PlayerAPI

Interact with other players.

```java
// Find and trade with player
PlayerEx player = PlayerAPI.search()
    .withName("SomeName")
    .first();

if (player != null) {
    PlayerAPI.interact(player, "Trade with");
}
```

Source: `api/src/main/java/com/tonic/api/entities/PlayerAPI.java`

### TileObjectAPI

Interact with game objects.

```java
// Open a door
TileObjectEx door = TileObjectAPI.search()
    .withName("Door")
    .withAction("Open")
    .nearest();

if (door != null) {
    TileObjectAPI.interact(door, "Open");
}

// Mine a rock
TileObjectEx rock = TileObjectAPI.search()
    .withNameContains("Iron rocks")
    .sortShortestPath()
    .first();

TileObjectAPI.interact(rock, "Mine");
```

Source: `api/src/main/java/com/tonic/api/entities/TileObjectAPI.java`

### TileItemAPI

Pick up ground items.

```java
// Pick up item
TileItemEx item = TileItemAPI.search()
    .withName("Bones")
    .nearest();

if (item != null) {
    TileItemAPI.interact(item, "Take");
}
```

Source: `api/src/main/java/com/tonic/api/entities/TileItemAPI.java`

---

## Widget Interaction

### WidgetAPI

General widget interaction.

```java
// Click a button
Widget button = WidgetAPI.search()
    .withText("Yes")
    .visible()
    .first();

if (button != null) {
    WidgetAPI.interact(button, 1);  // Left click
}

// Interact by action name
WidgetAPI.interact(widget, "Use");
```

Source: `api/src/main/java/com/tonic/api/widgets/WidgetAPI.java`

### InventoryAPI

Inventory item interactions.

```java
// Use an item
ItemEx food = InventoryAPI.search()
    .withAction("Eat")
    .first();

if (food != null) {
    InventoryAPI.interact(food, "Eat");
}

// Get all items
List<ItemEx> items = InventoryAPI.getItems();

// Get specific item by ID
ItemEx item = InventoryAPI.getItem(314);

// Get by name
ItemEx lobster = InventoryAPI.getItem("Lobster");
```

Source: `api/src/main/java/com/tonic/api/widgets/InventoryAPI.java`

### BankAPI

Banking operations.

```java
// Check if bank is open
if (BankAPI.isOpen()) {
    // Deposit all
    BankAPI.depositInventory();

    // Withdraw items using loadout
    InventoryLoadout loadout = new InventoryLoadout()
        .addItem("Shark", 10)
        .addItem("Prayer potion(4)", 4);

    BankAPI.withdraw(loadout);

    // Custom withdraw amount
    BankAPI.setX(14);  // Set custom amount
}
```

Source: `api/src/main/java/com/tonic/api/widgets/BankAPI.java`

---

## Dialogue Handling

### DialogueAPI

Handle NPC dialogues.

```java
// Check if dialogue is present
if (DialogueAPI.dialoguePresent()) {
    // Get dialogue text
    String text = DialogueAPI.getDialogueText();
    String speaker = DialogueAPI.getDialogueHeader();

    // Continue dialogue
    DialogueAPI.continueDialogue();

    // Select option
    DialogueAPI.selectOption(1);  // First option

    // Submit numeric input
    DialogueAPI.resumeNumericDialogue(100);

    // Submit string input
    DialogueAPI.resumeStringDialogue("hello");
}
```

Source: `api/src/main/java/com/tonic/api/widgets/DialogueAPI.java`

### DialogueBuilder

Fluent builder for complex dialogue handling.

```java
// Process dialogue with specific options
StepHandler handler = DialogueBuilder.get()
    .waitForDialogue()
    .processDialogues("Yes", "Continue")
    .continueAllDialogue()
    .build();

handler.execute();
```

Source: `api/src/main/java/com/tonic/api/handlers/DialogueBuilder.java`

---

## Movement

### MovementAPI

Player movement utilities.

```java
// Check movement state
boolean running = MovementAPI.isRunEnabled();
boolean moving = MovementAPI.isMoving();
boolean hasStamina = MovementAPI.staminaInEffect();

// Toggle run
MovementAPI.toggleRun();

// Walk to location
MovementAPI.walkToWorldPoint(new WorldPoint(3200, 3200, 0));
MovementAPI.walkToWorldPoint(3200, 3200);

// Get destination
WorldPoint dest = MovementAPI.getDestinationWorldPoint();
```

Source: `api/src/main/java/com/tonic/api/game/MovementAPI.java`

### Walking to Specific Locations

For long-distance travel, use the Walker instead of MovementAPI:

```java
// World walker for long distances
Walker.walkTo(new WorldPoint(3200, 3200, 0));

// With stop condition
Walker.walkTo(target, () -> PlayerEx.getLocal().getHealthRatio() < 20);
```

See [Pathfinding](pathfinding.md) for detailed Walker usage.

---

## Click Management

### ClickManager

Control how clicks are generated.

```java
// Set static click point
ClickManager.setPoint(400, 300);

// Queue click box for controlled clicking
Rectangle bounds = object.getClickbox();
ClickManager.queueClickBox(bounds);

// Clear click box
ClickManager.clearClickBox();

// Register a click
ClickManager.click();
ClickManager.click(ClickType.OBJECT);
```

### ClickManagerUtil

Helper utilities for click box setup.

```java
// Queue click on tile object
ClickManagerUtil.queueClickBox(tileObject);

// Queue click on actor
ClickManagerUtil.queueClickBox(npc);

// Queue click on item
ClickManagerUtil.queueClickBox(item);
```

Source: `base-api/src/main/java/com/tonic/services/ClickManager.java`

---

## Static.invoke Pattern

All interaction APIs use `Static.invoke()` internally for thread safety. When building custom interactions:

```java
// Invoke on client thread with return value
Widget result = Static.invoke(() -> {
    return client.getWidget(widgetId);
});

// Invoke without return
Static.invoke(() -> {
    client.doSomething();
});

// Invoke asynchronously
Static.invokeLater(() -> {
    // This runs later on client thread
});
```

Source: `src/main/java/com/tonic/Static.java`

---

## Common Mistakes

### Not waiting for actions to complete

**Wrong:**
```java
NpcAPI.interact(banker, "Bank");
BankAPI.depositInventory();  // Bank might not be open yet!
```

**Correct:**
```java
NpcAPI.interact(banker, "Bank");
Delays.waitUntil(BankAPI::isOpen, 5000);
BankAPI.depositInventory();
```

### Interacting from wrong thread

**Wrong:**
```java
// In a non-client thread
client.getWidget(...)  // May cause issues
```

**Correct:**
```java
Static.invoke(() -> {
    client.getWidget(...)
});
```

### Not checking action availability

**Wrong:**
```java
TileObjectAPI.interact(door, "Open");  // Door might not have "Open" action
```

**Correct:**
```java
TileObjectEx door = TileObjectAPI.search()
    .withName("Door")
    .withAction("Open")  // Only get doors with Open action
    .first();

if (door != null) {
    TileObjectAPI.interact(door, "Open");
}
```

---

## Error Handling

Interactions may fail silently. Always verify the expected outcome:

```java
// Interact and verify
NpcAPI.interact(banker, "Bank");

boolean success = Delays.waitUntil(BankAPI::isOpen, 3000);
if (!success) {
    Logger.warn("Failed to open bank");
    // Handle failure
}
```
