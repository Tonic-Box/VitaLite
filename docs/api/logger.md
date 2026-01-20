# Logger

VitaLite provides a centralized logging system with colored console output and SLF4J-style formatting.

## Overview

The `Logger` class provides:
- Color-coded log levels
- Embedded console UI
- File logging
- SLF4J-style placeholder formatting

Source: `src/main/java/com/tonic/Logger.java`

---

## Log Levels

| Level | Color | Method | Use Case |
|-------|-------|--------|----------|
| Normal | Green | `Logger.norm()` | Standard messages |
| Info | Light Blue | `Logger.info()` | Informational messages |
| Warning | Yellow | `Logger.warn()` | Non-critical issues |
| Error | Red | `Logger.error()` | Errors and exceptions |

---

## Basic Logging

### Simple Messages

```java
Logger.norm("Plugin started");
Logger.info("Processing item: " + itemName);
Logger.warn("Low health detected");
Logger.error("Failed to open bank");
```

### SLF4J-Style Formatting

Supports `{}` placeholders for cleaner code:

```java
Logger.norm("Found {} items", count);
Logger.info("Player {} at position ({}, {})", name, x, y);
Logger.warn("Retry {} of {}", attempt, maxRetries);
Logger.error("Error processing {}: {}", item, errorMessage);
```

Source: Added in commit `1f4df6a`

### Logging Exceptions

```java
try {
    // some code
} catch (Exception e) {
    Logger.error(e);  // Logs exception with stack trace
    Logger.error(e, "Failed during operation");  // With context message
}
```

---

## Console UI

### Visibility

```java
// Toggle console visibility (persists since v1.12.11)
Logger.setLoggerVisible(true);
Logger.setLoggerVisible(false);
```

### Initialization

For plugin developers - console is auto-initialized by VitaLite:

```java
// Initialize logger UI (done by VitaLite core)
Logger.initLoggerUI(consoleComponent, container, frame);
```

---

## Configuration

### Toggle Log Levels

```java
// Enable/disable specific levels
Logger.setNormal(true);
Logger.setInfo(true);
Logger.setWarning(true);
Logger.setError(true);
```

### Message Limit

```java
// Set maximum messages in console buffer
Logger.setMaxMessages(100);
```

Default is 50 messages.

---

## Console Input/Output

For interactive console features:

```java
// Display user input
Logger.console("user command here");

// Display console output
Logger.consoleOutput("Result", "Output body text");
```

---

## File Logging

Logs are written to files automatically via `LogFileManager`:

- Location: `~/.runelite/vitalite/logs/`
- Format: Timestamped entries

Source: `src/main/java/com/tonic/logging/LogFileManager.java`

---

## Output Format

Console messages are formatted as:

```
[dd/MM/yyyy HH:mm:ss] message
```

Example:
```
[20/01/2024 14:30:45] Found 5 items
[20/01/2024 14:30:46] Processing complete
```

---

## Complete Example

```java
public class MyPlugin extends VitaPlugin {

    @Override
    protected void startUp() {
        Logger.norm("MyPlugin starting up");
        Logger.info("Version: {}", VERSION);
    }

    @Override
    public void loop() {
        try {
            int itemCount = InventoryAPI.search().count();
            Logger.norm("Inventory has {} items", itemCount);

            if (itemCount > 20) {
                Logger.warn("Inventory nearly full ({}/28)", itemCount);
            }

            processItems();

        } catch (Exception e) {
            Logger.error(e, "Error in main loop");
        }
    }

    @Override
    protected void shutDown() {
        Logger.norm("MyPlugin shutting down");
    }
}
```

---

## Best Practices

### Use Appropriate Levels

```java
// Good: Appropriate levels
Logger.norm("Started fishing");           // Normal operation
Logger.info("Caught {} fish", count);     // Informational
Logger.warn("Out of bait");               // Warning condition
Logger.error("Connection lost");          // Error condition

// Bad: Wrong levels
Logger.error("Caught 5 fish");            // Not an error
Logger.norm("Critical failure!");         // Should be error
```

### Use Placeholders for Performance

```java
// Good: Placeholder (evaluated only if logged)
Logger.info("Processing {} items at ({}, {})", count, x, y);

// Less efficient: String concatenation (always evaluated)
Logger.info("Processing " + count + " items at (" + x + ", " + y + ")");
```

### Log Context

```java
// Good: Include context
Logger.error("Failed to withdraw {} {} from bank", quantity, itemName);

// Bad: No context
Logger.error("Withdraw failed");
```

### Avoid Excessive Logging

```java
// Bad: Logging every tick
@Subscribe
public void onGameTick(GameTick event) {
    Logger.norm("Tick!");  // Creates spam
}

// Good: Log meaningful events
@Subscribe
public void onGameTick(GameTick event) {
    if (stateChanged) {
        Logger.norm("State changed to: {}", newState);
    }
}
```

---

## Common Mistakes

### Forgetting exception parameter

**Wrong:**
```java
try {
    // code
} catch (Exception e) {
    Logger.error("Error occurred");  // Stack trace lost!
}
```

**Correct:**
```java
try {
    // code
} catch (Exception e) {
    Logger.error(e);  // Full stack trace
    // or
    Logger.error(e, "Error during operation");
}
```

### Logging sensitive data

**Wrong:**
```java
Logger.info("Login: {}:{}", username, password);  // Never log credentials!
```

**Correct:**
```java
Logger.info("Login attempt for user: {}", username);
```

### Calling Logger too early

Fixed in v1.12.12.1_1 - Logger now handles early calls gracefully.

Source: Commit `288455d`
