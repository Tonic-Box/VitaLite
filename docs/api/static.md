# Static Class

The `Static` class provides global access to core client objects and thread-safe invocation utilities.

## Overview

`Static` is the central access point for:
- Client instance and RuneLite wrapper
- Thread-safe client thread invocation
- Event bus posting
- Configuration access

Source: `base-api/src/main/java/com/tonic/Static.java`

---

## Client Access

### Getting the Client

```java
// Get the client instance (generic return type)
// Can be assigned to TClient or Client
TClient client = Static.getClient();
// Or: Client client = Static.getClient();

// Example: Get current tick
int tick = client.getTickCount();

// Note: Due to generics, you must assign to a variable first.
// Static.getClient().getTickCount() will not compile.
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
// Invoke later (non-blocking, fire-and-forget)
Static.invokeLater(() -> {
    // Runs on client thread at next opportunity
    client.doSomething();
});

// Invoke later with return (blocks until complete, returns value directly)
Widget widget = Static.invokeLater(() -> {
    return client.getWidget(widgetId);
});
// Note: Despite the name, invokeLater with a return value blocks the calling
// thread until the result is available. It uses CompletableFuture.join() internally.
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

For widget access, prefer using `WidgetAPI` or `WidgetQuery` which handle thread safety internally:

```java
// Recommended: Use WidgetAPI/WidgetQuery (handles invocation internally)
Widget widget = WidgetAPI.search()
    .withId(groupId << 16 | childId)
    .isVisible()
    .first();

// Or using WidgetQuery directly
Widget widget = new WidgetQuery()
    .withId(groupId << 16 | childId)
    .isVisible()
    .first();

if (widget != null) {
    // Use widget
}

// Manual invoke is still available if needed for custom logic:
Widget widget = Static.invoke(() -> {
    Widget w = client.getWidget(groupId, childId);
    if (w != null && !w.isHidden()) {
        return w;
    }
    return null;
});
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

Nested invokes are safe - the implementation checks `isClientThread()` and executes directly if already on the client thread. However, keeping everything in one invoke is cleaner:

```java
// This is safe (no deadlock), but unnecessary nesting
Static.invoke(() -> {
    Static.invoke(() -> {  // Executes directly since already on client thread
        // ...
    });
});

// Cleaner: Do everything in one invoke
Static.invoke(() -> {
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

Some read-only operations like `getTickCount()` are safe to call off the client thread:

```java
// Reading tick count is fine off-thread
TClient client = Static.getClient();
int tick = client.getTickCount();  // OK for read-only values

// Note: Due to generics, you must assign to a variable first:
// Static.getClient().getTickCount();  // Won't compile

// For operations that modify state or access mutable data, use invoke:
Static.invoke(() -> {
    TClient client = Static.getClient();
    client.doSomethingThatModifiesState();
});
```

### Storing references from invoke

Widget references can become stale. Re-fetch when needed or use `WidgetAPI`/`WidgetQuery`:

**Avoid:**
```java
Widget savedWidget = Static.invoke(() -> client.getWidget(id));
// Later, in another thread...
savedWidget.getText();  // Widget might be stale or modified!
```

**Better - Re-fetch when needed:**
```java
String text = Static.invoke(() -> {
    Widget w = client.getWidget(id);
    return w != null ? w.getText() : null;
});
```

**Best - Use WidgetAPI/WidgetQuery:**
```java
// WidgetAPI/WidgetQuery handles thread safety and freshness
Widget w = WidgetAPI.search()
    .withId(id)
    .first();
String text = w != null ? w.getText() : null;
```

---

## Thread Safety Notes

1. **API classes** (NpcAPI, InventoryAPI, etc.) handle invocation internally
2. **VitaPlugin.loop()** runs on a worker thread - use APIs or invoke()
3. **GameTick handlers** run on client thread - no invoke needed
4. **Never block** the client thread with long operations
5. **Keep invokes atomic** - don't split related operations across multiple invokes
