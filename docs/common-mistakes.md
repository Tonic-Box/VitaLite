# Common Mistakes

This document covers frequent errors when using the VitaLite SDK and how to fix them.

---

## Initialization Errors

### Missing API Sync

**Symptom:** Class not found errors during build

**Cause:** RuneLite API not synced

**Fix:**
```bash
./gradlew SyncRuneliteApi
./gradlew buildAndPublishAll
```

### Wrong Gradle Dependencies

**Symptom:** Cannot resolve dependency errors

**Wrong:**
```kotlin
implementation("com.tonic:api:1.0.0")  // Wrong - use compileOnly
```

**Correct:**
```kotlin
val apiVersion = "latest.release"
compileOnly("net.runelite:client:$apiVersion")
compileOnly("com.tonic:base-api:$apiVersion")
compileOnly("com.tonic:api:$apiVersion")
```

Source: `docs/EXTERNALPLUGIN.md:7-13`

---

## Thread Safety Errors

### Accessing Client from Wrong Thread

**Symptom:** Inconsistent behavior, crashes, or stale data

**Wrong:**
```java
// From worker thread
TClient client = Static.getClient();
int tick = client.getTickCount();
```

**Correct:**
```java
int tick = Static.invoke(() -> {
    return Static.getClient().getTickCount();
});
```

### Blocking Client Thread

**Symptom:** Game freezes, UI unresponsive

**Wrong:**
```java
@Subscribe
public void onGameTick(GameTick event) {
    Thread.sleep(1000);  // Blocks client thread!
    Walker.walkTo(target);  // Also blocks!
}
```

**Correct:**
```java
// Use VitaPlugin.loop() for blocking operations
@Override
public void loop() {
    Delays.wait(1000);  // OK in loop
    Walker.walkTo(target);  // OK in loop
}
```

### Nested Static.invoke()

**Note:** Nested invokes are actually safe - the implementation checks `isClientThread()` and executes directly if already on the client thread. However, nesting is unnecessary and keeping everything in one invoke is cleaner:

```java
// Safe but unnecessary nesting
Static.invoke(() -> {
    Static.invoke(() -> {  // Executes directly, no deadlock
        // ...
    });
});

// Cleaner: Do everything in one invoke
Static.invoke(() -> {
    // All operations here
});
```

---

## Query Errors

### Not Checking for Null

**Symptom:** NullPointerException

**Wrong:**
```java
NpcEx banker = new NpcQuery().withName("Banker").first();
NpcAPI.interact(banker, "Bank");  // NPE if null!
```

**Correct:**
```java
NpcEx banker = new NpcQuery().withName("Banker").first();
if (banker != null) {
    NpcAPI.interact(banker, "Bank");
}
```

### Case Sensitivity Assumption

**Symptom:** Query returns nothing when entity exists

**Wrong:**
```java
new NpcQuery().withName("BANKER")  // Case matters!
```

**Correct:**
```java
new NpcQuery().withName("Banker")        // Use correct case
new NpcQuery().withNameContains("bank")  // Or use contains (case-insensitive)
```

### Forgetting Distance Limits

**Symptom:** Interacting with distant entities, getting stuck

**Wrong:**
```java
NpcEx target = new NpcQuery().withName("Goblin").first();
// Might return goblin on other side of map
```

**Correct:**
```java
NpcEx target = new NpcQuery()
    .withName("Goblin")
    .withinDistance(15)
    .nearest();
```

---

## Interaction Errors

### Not Waiting for Actions

**Symptom:** Actions executed too fast, failing

**Wrong:**
```java
NpcAPI.interact(banker, "Bank");
BankAPI.depositAll();  // Bank not open yet!
```

**Correct:**
```java
NpcAPI.interact(banker, "Bank");
Delays.waitUntil(BankAPI::isOpen, 5000);
BankAPI.depositAll();
```

### Missing Action Check

**Symptom:** Interaction fails silently

**Wrong:**
```java
TileObjectAPI.interact(door, "Open");  // Door might not have "Open"
```

**Correct:**
```java
TileObjectEx door = TileObjectAPI.search()
    .withName("Door")
    .withAction("Open")
    .first();

if (door != null) {
    TileObjectAPI.interact(door, "Open");
}
```

---

## Walker Errors

### Calling Walker from Client Thread

**Symptom:** Game freezes

**Wrong:**
```java
@Subscribe
public void onGameTick(GameTick event) {
    Walker.walkTo(target);  // Blocks client thread!
}
```

**Correct:**
```java
@Override
public void loop() {
    Walker.walkTo(target);  // OK in VitaPlugin.loop()
}
```

### Ignoring Unreachable Destinations

**Symptom:** Walker gets stuck

**Wrong:**
```java
Walker.walkTo(unreachablePoint);
```

**Correct:**
```java
WorldPoint walkable = Walker.getCollisionMap()
    .nearestWalkableEuclidean(target, 5);
Walker.walkTo(walkable);
```

### No Stop Condition

**Symptom:** Walking continues when it shouldn't

**Wrong:**
```java
Walker.walkTo(target);
// Keeps walking even if player dies
```

**Correct:**
```java
Walker.walkTo(target, () -> {
    return !GameAPI.isLoggedIn() ||
           PlayerEx.getLocal().isDead();
});
```

---

## Configuration Errors

### Wrong Proxy Format

**Symptom:** Proxy connection fails

**Wrong:**
```bash
--proxy "http://192.168.1.100:1080"  # Don't include protocol
--proxy "192.168.1.100"              # Missing port
```

**Correct:**
```bash
--proxy "192.168.1.100:1080"
--proxy "192.168.1.100:1080:user:pass"
```

### Forgetting to Save Config

**Symptom:** Settings lost on restart

**Wrong:**
```java
config.setProperty("key", "value");
// Forgot to save!
```

**Correct:**
```java
config.setProperty("key", "value");
config.saveConfig();
```

---

## IPC Errors

### Channel Not Started

**Symptom:** Messages not sent/received

**Wrong:**
```java
Channel channel = new ChannelBuilder("Client").build();
channel.broadcast("msg", Map.of());  // Not started!
```

**Correct:**
```java
Channel channel = new ChannelBuilder("Client").build();
channel.start();
channel.broadcast("msg", Map.of());
```

### Processing Own Messages

**Symptom:** Infinite loops, duplicate actions

**Wrong:**
```java
channel.addHandler(msg -> {
    executeAction(msg);  // Executes own broadcasts!
});
```

**Correct:**
```java
channel.addHandler(msg -> {
    if (!msg.getSenderId().equals(myClientId)) {
        executeAction(msg);
    }
});
```

### Non-Serializable Payload

**Symptom:** Message send fails

**Wrong:**
```java
channel.broadcast("data", Map.of(
    "object", new NonSerializableClass()
));
```

**Correct:**
```java
channel.broadcast("data", Map.of(
    "value", 100,  // Primitives OK
    "name", "text"  // Strings OK
));
```

---

## Click Manager Errors

### Click Box Not Set for Controlled

**Symptom:** Falls back to static clicking

**Wrong:**
```java
// Strategy is CONTROLLED but no box set
ClickManager.click();  // Falls back to STATIC
```

**Correct:**
```java
ClickManager.queueClickBox(object.getClickbox());
ClickManager.click();
```

### Stale Click Box

**Symptom:** Clicking wrong location

**Wrong:**
```java
ClickManager.queueClickBox(oldObject.getClickbox());
// Object moved/changed
ClickManager.click();  // Clicks old position
```

**Correct:**
```java
// Re-query fresh object
TileObjectEx obj = TileObjectAPI.search()...first();
ClickManager.queueClickBox(obj.getClickbox());
ClickManager.click();
```

---

## Script DSL Errors

### Forgetting to Execute

**Symptom:** Script never runs

**Wrong:**
```java
Script.build(s -> {
    s.action(() -> doSomething());
});
// Never executed!
```

**Correct:**
```java
StepHandler handler = Script.build(s -> {
    s.action(() -> doSomething());
});
handler.execute();

// Or use execute directly
Script.execute(s -> {
    s.action(() -> doSomething());
});
```

### Mismatched Labels

**Symptom:** Jump fails, unexpected behavior

**Wrong:**
```java
s.label("start");
s.jumpIf(condition, "Start");  // Case mismatch!
```

**Correct:**
```java
s.label("start");
s.jumpIf(condition, "start");
```

---

## Logging Errors

### Losing Stack Traces

**Symptom:** Hard to debug errors

**Wrong:**
```java
catch (Exception e) {
    Logger.error("Error occurred");  // Stack trace lost!
}
```

**Correct:**
```java
catch (Exception e) {
    Logger.error(e);
    // or
    Logger.error(e, "Error during operation");
}
```

### Excessive Logging

**Symptom:** Console spam, performance issues

**Wrong:**
```java
@Override
public void loop() {
    Logger.info("Loop iteration");  // Every tick!
}
```

**Correct:**
```java
@Override
public void loop() {
    if (stateChanged) {
        Logger.info("State: {}", newState);
    }
}
```

---

## General Best Practices

1. **Always null-check** query results
2. **Use VitaPlugin.loop()** for blocking operations
3. **Use Static.invoke()** for client thread access
4. **Add stop conditions** to walkers
5. **Save configs** after changes
6. **Start channels** before broadcasting
7. **Filter own IPC messages**
8. **Log exceptions** with the throwable parameter
9. **Distance-limit** entity queries
10. **Wait for actions** to complete before continuing
