# IPC Channel

VitaLite provides a multicast-based inter-process communication (IPC) system for peer-to-peer communication between multiple client instances.

## Overview

The IPC system allows multiple VitaLite clients to communicate without a master-slave relationship. All nodes are equal participants.

Key features:
- Multicast UDP for low-latency messaging
- Automatic duplicate message detection
- Serializable object transmission
- No central server required

Source: `base-api/src/main/java/com/tonic/services/ipc/`

---

## Channel

The `Channel` class manages IPC communication.

### Creating a Channel

```java
Channel channel = new ChannelBuilder("MyClient")
    .port(5000)
    .group("230.0.0.1")
    .build();
```

### Builder Options

| Method | Default | Description |
|--------|---------|-------------|
| `port(int)` | 5000 | Multicast UDP port |
| `group(String)` | "230.0.0.0" | Multicast group address |
| `ttl(int)` | 1 | Time-to-live for packets |
| `networkInterface(NetworkInterface)` | null | Specific network interface |

Source: `base-api/src/main/java/com/tonic/services/ipc/ChannelBuilder.java`

### Starting and Stopping

```java
// Start listening
channel.start();

// Check status
if (channel.isRunning()) {
    // ...
}

// Stop and release resources
channel.stop();
```

---

## Sending Messages

### Basic Broadcast

```java
// Broadcast a message with type and payload
channel.broadcast("hello", Map.of(
    "message", "Hello from client!",
    "timestamp", System.currentTimeMillis()
));
```

### Pre-built Message

```java
// Create message with builder
Message message = new Message.Builder(clientId, clientName)
    .type("command")
    .put("action", "moveToBank")
    .put("world", 301)
    .build();

channel.broadcast(message);
```

---

## Receiving Messages

### Adding Handlers

```java
channel.addHandler(new MessageHandler() {
    @Override
    public void onMessage(Message message) {
        String type = message.getType();
        String sender = message.getSenderName();

        if ("hello".equals(type)) {
            String text = (String) message.get("message");
            Logger.info("Received from {}: {}", sender, text);
        }
    }

    @Override
    public void onError(Throwable error) {
        Logger.error("IPC error: " + error.getMessage());
    }
});
```

### Lambda Handler

```java
channel.addHandler(msg -> {
    Logger.info("Got message type: " + msg.getType());
});
```

### Removing Handlers

```java
MessageHandler handler = msg -> { /* ... */ };
channel.addHandler(handler);

// Later...
channel.removeHandler(handler);
```

---

## Message Structure

### Message Class

```java
// Message properties
String messageId;     // Unique message ID (UUID)
String senderId;      // Sender's client ID
String senderName;    // Sender's display name
String type;          // Message type
Map<String, Object> payload;  // Message data
long timestamp;       // Send timestamp
```

### Accessing Message Data

```java
channel.addHandler(msg -> {
    // Get message metadata
    String id = msg.getMessageId();
    String senderId = msg.getSenderId();
    String senderName = msg.getSenderName();
    String type = msg.getType();
    long timestamp = msg.getTimestamp();

    // Get payload data
    Object value = msg.get("key");
    String stringVal = (String) msg.get("stringKey");
    Integer intVal = (Integer) msg.get("intKey");
});
```

Source: `base-api/src/main/java/com/tonic/services/ipc/Message.java`

---

## Complete Example

### Multi-client Coordination

```java
public class MultiClientCoordinator {
    private static final String CHANNEL_GROUP = "230.0.0.1";
    private static final int CHANNEL_PORT = 5000;

    private Channel channel;
    private String myClientId;

    public void initialize(String clientName) {
        myClientId = UUID.randomUUID().toString();

        channel = new ChannelBuilder(clientName)
            .port(CHANNEL_PORT)
            .group(CHANNEL_GROUP)
            .build();

        channel.addHandler(this::handleMessage);
        channel.start();

        // Announce presence
        channel.broadcast("join", Map.of(
            "clientId", myClientId,
            "name", clientName
        ));
    }

    private void handleMessage(Message msg) {
        // Ignore own messages
        if (msg.getSenderId().equals(myClientId)) {
            return;
        }

        switch (msg.getType()) {
            case "join":
                Logger.info("Client joined: " + msg.getSenderName());
                break;

            case "command":
                String action = (String) msg.get("action");
                executeCommand(action);
                break;

            case "status":
                updatePeerStatus(msg);
                break;
        }
    }

    public void sendCommand(String action) {
        channel.broadcast("command", Map.of("action", action));
    }

    public void broadcastStatus(int health, WorldPoint location) {
        channel.broadcast("status", Map.of(
            "health", health,
            "x", location.getX(),
            "y", location.getY(),
            "plane", location.getPlane()
        ));
    }

    public void shutdown() {
        channel.broadcast("leave", Map.of("clientId", myClientId));
        channel.stop();
    }
}
```

---

## Implementation Details

### Duplicate Detection

Messages are tracked for 5 seconds to prevent duplicate processing:

```java
// Internal constant
private static final long DUPLICATE_WINDOW_MS = 5000;
```

The system uses message IDs (UUIDs) to identify duplicates.

Source: `base-api/src/main/java/com/tonic/services/ipc/Channel.java:63`

### Buffer Size

Maximum message size is 64KB:

```java
private static final int BUFFER_SIZE = 65536;
```

Source: `base-api/src/main/java/com/tonic/services/ipc/Channel.java:43`

### Serialization

Messages are serialized using Java Object Serialization. All payload values must be `Serializable`.

---

## Common Mistakes

### Not starting the channel

**Wrong:**
```java
Channel channel = new ChannelBuilder("Client").build();
channel.broadcast("hello", Map.of());  // Channel not started!
```

**Correct:**
```java
Channel channel = new ChannelBuilder("Client").build();
channel.start();
channel.broadcast("hello", Map.of());
```

### Non-serializable payload

**Wrong:**
```java
channel.broadcast("data", Map.of(
    "object", new NonSerializableClass()  // Will fail
));
```

**Correct:**
```java
channel.broadcast("data", Map.of(
    "value", serializableValue,
    "x", 100,  // Primitives are fine
    "name", "string"  // Strings are fine
));
```

### Processing own messages

**Wrong:**
```java
channel.addHandler(msg -> {
    executeAction(msg);  // Will execute own broadcasts too!
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

### Not stopping channel on shutdown

**Wrong:**
```java
// Plugin disabled without stopping channel
// Resources leak, socket stays open
```

**Correct:**
```java
@Override
protected void shutDown() {
    if (channel != null) {
        channel.stop();
    }
}
```

---

## Network Considerations

### Firewall Rules

Ensure UDP port (default 5000) is open for:
- Outbound multicast to group address
- Inbound multicast from group address

### Multiple Network Interfaces

When multiple interfaces exist, specify which to use:

```java
NetworkInterface iface = NetworkInterface.getByName("eth0");
Channel channel = new ChannelBuilder("Client")
    .networkInterface(iface)
    .build();
```

### TTL (Time-to-Live)

The default TTL of 1 limits messages to the local network segment. Increase for wider distribution:

```java
Channel channel = new ChannelBuilder("Client")
    .ttl(5)  // Allow crossing up to 5 routers
    .build();
```
