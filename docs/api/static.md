# Static Class

The `Static` class provides global access to core client objects and thread-safe invocation utilities.

## Overview

`Static` is the central access point for:
- Client instance and RuneLite wrapper
- Thread-safe client thread invocation
- Event bus posting
- Configuration access

Source: `src/main/java/com/tonic/Static.java`

---

## Client Access

### Getting the Client

```java
// Get the TClient instance
TClient client = Static.getClient();

// Example: Get current tick
int tick = client.getTickCount();
```

### Getting RuneLite

```java
// Get RuneLite wrapper
RuneLite runelite = Static.getRuneLite();

// Get Guice injector
Injector injector = Static.getInjector();
```

### Getting Configuration

```java
// Get VitaLite configuration
VitaConfig config = Static.getVitaConfig();

// Check settings
ClickStrategy strategy = config.getClickStrategy();
boolean spoofMouse = config.shouldSpoofMouseMovemnt();
```

---

## Thread-Safe Invocation

The game client is not thread-safe. Use `invoke()` methods to run code on the client thread.

### Synchronous Invocation

```java
// Invoke with return value
Widget widget = Static.invoke(() -> {
    return client.getWidget(widgetId);
});

// Invoke without return
Static.invoke(() -> {
    client.doSomething();
});
```

### Asynchronous Invocation

```java
// Invoke later (non-blocking)
Static.invokeLater(() -> {
    // Runs on client thread at next opportunity
    client.doSomething();
});

// Invoke later with return (returns Future)
Future<Widget> future = Static.invokeLater(() -> {
    return client.getWidget(widgetId);
});
```

### Usage Guidelines

| Situation | Use |
|-----------|-----|
| Need result immediately | `Static.invoke()` |
| Don't need result | `Static.invoke()` (void) |
| Fire and forget | `Static.invokeLater()` |
| From VitaPlugin.loop() | Use API classes directly (they invoke internally) |

---

## Event Bus

### Posting Events

```java
// Post an event to the event bus
MyCustomEvent event = new MyCustomEvent(data);
Static.post(event);
```

### Subscribing to Events

Events are subscribed via the standard RuneLite annotation:

```java
@Subscribe
public void onMyCustomEvent(MyCustomEvent event) {
    // Handle event
}
```

---

## Directory Constants

```java
// RuneLite base directory (~/.runelite)
Path runeliteDir = Static.RUNELITE_DIR;

// VitaLite config directory (~/.runelite/vitalite)
Path vitaDir = Static.VITA_DIR;
```

---

## Headless Mode

```java
// Enable/disable headless mode (no rendering)
Static.setHeadless(true);

// Check if running from shaded JAR
boolean fromJar = Static.isRunningFromShadedJar();
```

---

## Command Line Arguments

```java
// Get parsed CLI arguments
VitaLiteOptions options = Static.getCliArgs();

// Check options
if (options.isIncognito()) {
    // ...
}
```

---

## Common Patterns

### Safe Widget Access

```java
// Get widget safely on client thread
Widget widget = Static.invoke(() -> {
    Widget w = client.getWidget(groupId, childId);
    if (w != null && !w.isHidden()) {
        return w;
    }
    return null;
});

if (widget != null) {
    // Use widget
}
```

### Batch Operations

```java
// Perform multiple operations atomically
Static.invoke(() -> {
    Widget widget = client.getWidget(id);
    if (widget != null) {
        // Do multiple things
        int x = widget.getRelativeX();
        int y = widget.getRelativeY();
        // etc.
    }
});
```

### Conditional Invocation

```java
// Only invoke if needed
if (someCondition) {
    Static.invoke(() -> {
        // Expensive operation
    });
}
```

---

## Common Mistakes

### Nested invokes

**Wrong:**
```java
Static.invoke(() -> {
    Static.invoke(() -> {  // Deadlock risk!
        // ...
    });
});
```

**Correct:**
```java
Static.invoke(() -> {
    // Do everything in one invoke
    Widget w = client.getWidget(id);
    // Process w
});
```

### Long-running operations in invoke

**Wrong:**
```java
Static.invoke(() -> {
    Thread.sleep(1000);  // Blocks client thread!
    Walker.walkTo(target);  // Never do this!
});
```

**Correct:**
```java
// Keep invoke fast
Widget widget = Static.invoke(() -> client.getWidget(id));

// Do slow operations outside invoke
Thread.sleep(1000);
Walker.walkTo(target);
```

### Accessing client outside invoke

**Wrong:**
```java
// From a worker thread
TClient client = Static.getClient();
int tick = client.getTickCount();  // Unsafe!
```

**Correct:**
```java
int tick = Static.invoke(() -> {
    return Static.getClient().getTickCount();
});
```

### Storing references from invoke

**Wrong:**
```java
Widget savedWidget = Static.invoke(() -> client.getWidget(id));
// Later, in another thread...
savedWidget.getText();  // Widget might be stale or modified!
```

**Correct:**
```java
// Re-fetch when needed
String text = Static.invoke(() -> {
    Widget w = client.getWidget(id);
    return w != null ? w.getText() : null;
});
```

---

## Thread Safety Notes

1. **API classes** (NpcAPI, InventoryAPI, etc.) handle invocation internally
2. **VitaPlugin.loop()** runs on a worker thread - use APIs or invoke()
3. **GameTick handlers** run on client thread - no invoke needed
4. **Never block** the client thread with long operations
5. **Keep invokes atomic** - don't split related operations across multiple invokes
