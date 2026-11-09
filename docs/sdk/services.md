[SDK index](../SDK-DOCS.md) · [← Game data](game-data.md) · [UI →](ui.md)

# Services

Client-wide pieces that plugins can use.

## Game state

| Class | What it is |
|---|---|
| `GameManager` | `com.tonic.services`. The tick counter and the per-tick cached lists of players, NPCs, objects, ground items and widgets that queries read from. Also reachable-tile checks. |
| `BankCache` | `com.tonic.services`. Remembers each account's bank contents so you can check them while the bank is closed. Posts `BankCacheChanged` when they change. |
| `LoginService` | `com.tonic.services`. Fill in the login screen and optionally log in, for normal or Jagex accounts. |

## Break handler

`com.tonic.services.breakhandler.BreakHandler` schedules breaks for a plugin. Get it from the injector, `register` your plugin, and check `isBreaking(...)` in your loop. Its settings live in the built-in Break Handler plugin.

## Multi-client messaging

`com.tonic.services.ipc` lets VitaLite clients on the same network send each other messages over UDP multicast.

| Class | What it is |
|---|---|
| `ChannelBuilder` | Sets the client name, port, multicast group and TTL, then `build()`s a `Channel`. |
| `Channel` | `start()`, `stop()`, `broadcast(type, payload)` and message handlers. |
| `Message` | One message: sender, type, timestamp and a payload map. |
| `MessageHandler` | The callback for incoming messages. |

```java
Channel channel = new ChannelBuilder("MyClient").port(5000).build();
channel.addHandler(message -> Logger.info(message.getType()));
channel.start();
channel.broadcast("hello", Map.of("text", "hi"));
```

## Clicks and mouse

| Class | What it is |
|---|---|
| `ClickManager` | `com.tonic.services`. Decides where each action's click lands, according to the click strategy chosen in settings. `queueClickBox(shape)` sets the area for the next click. See [Click Manager](../CLICKMANAGER.md). |
| `ClickManagerUtil` | `com.tonic.util`. `queueClickBox` for an object, item, actor, ground item or widget. |
| `TrajectoryService` | `com.tonic.services.mouserecorder.trajectory`. Records your real mouse movement and generates movement from it. See [Mouse Movement](../MOUSE-MOVEMENT.md). |

## Settings storage

`com.tonic.services.ConfigManager` is a small properties file in the VitaLite folder. Create one with a file name, then get and set properties.

`com.tonic.util.config.ConfigFactory` turns an interface annotated with `@ConfigGroup` and `@ConfigKey` into a settings object backed by storage, which is how VitaLite's own settings (`ClientConfig`) work.

## Proxies

`com.tonic.services.proxy`: `ProxyManager.process("ip:port[:user:pass]")` routes the client through a SOCKS proxy, as the `-proxy` launch option does. `SocksProxyUtil` sets, checks and clears it.

## Packets and events

| Class | What it is |
|---|---|
| `PacketSent` / `PacketReceived` | `com.tonic.events`. Posted for every packet sent and received. Each has the id and a readable buffer. |
| `BankCacheChanged` | `com.tonic.events`. Posted when the bank cache changes. |
| `PacketMapReader` | `com.tonic.packets`. Packet names and ids, and turning raw packets into readable text. |
| `TClient` and the other `T*` interfaces | `com.tonic.api`. Extra access to client internals that RuneLite doesn't expose, such as the packet writer. Cast `Static.getClient()` to `TClient`. |

[SDK index](../SDK-DOCS.md) · [← Game data](game-data.md) · [UI →](ui.md)
