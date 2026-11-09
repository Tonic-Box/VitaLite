[SDK index](../SDK-DOCS.md) · [Game API →](game-api.md)

# Getting started

## Depending on VitaLite

A plugin compiles against three artifacts: RuneLite's `client`, VitaLite's `base-api` and VitaLite's `api`. They are published to your local Maven repository when you build VitaLite, so build it once before building a plugin. [Plugin Dev Guide](../EXTERNALPLUGIN.md) has the Gradle setup.

- `base-api` holds `Static`, `Logger`, the UI components, IPC, click handling and other client-level pieces.
- `api` holds the game API, queries, wrappers, handlers, the walker and the services built on top of the game.

## Static

`com.tonic.Static` is the entry point to the client.

| Member | What it is |
|---|---|
| `getClient()` | The RuneLite `Client`. Cast to `TClient` for VitaLite's extra methods (packet writer and so on). |
| `getInjector()` | RuneLite's Guice injector, for `ItemManager`, `ConfigManager` and the like. |
| `invoke(...)` / `invokeLater(...)` | Run code on the client thread (see below). |
| `post(event)` | Post an event on RuneLite's event bus. |
| `getVitaConfig()` | VitaLite's own settings. |
| `isHeadless()` / `setHeadless(...)` | Headless mode state. |
| `RUNELITE_DIR`, `VITA_DIR` | `~/.runelite` and `~/.runelite/vitalite`. |

## Threading

Game state may only be touched on the client thread. VitaLite handles that for you in two ways.

**Instant calls.** Almost every API method (the `*API` classes, queries, wrappers) wraps its work in `Static.invoke` itself. It reads state or sends one action and returns right away. You can call these from any thread.

**Waiting calls.** Code that sleeps or waits for game ticks must not run on the client thread, or it would freeze the game. That covers the `com.tonic.api.threaded` package, `Delays`, `StepHandler.execute()` and a few other methods marked "(waits)" in these docs. `Delays` throws if you call it from the client thread.

Use `Static.invoke` for your own client reads:

```java
int hp = Static.invoke(() -> client.getBoostedSkillLevel(Skill.HITPOINTS));
```

The `Supplier` form blocks until the client thread has run it. The `Runnable` form returns straight away when called from another thread. Both run immediately if you are already on the client thread.

## VitaPlugin

`com.tonic.util.VitaPlugin` is a RuneLite `Plugin` with one addition: `loop()`. It is started on a worker thread at the start of each game tick, unless the previous run is still going, so it can use the waiting calls.

```java
public class MyPlugin extends VitaPlugin {
    @Override
    public void loop() throws Exception {
        Delays.tick(2);
    }
}
```

`haltLoop(callback)` stops the running loop. Under the hood each run is an `AsyncTask`. Waiting calls check `AsyncTask` cancellation and throw when cancelled, which is how a loop gets stopped mid-wait.

Plugins that extend RuneLite's plain `Plugin` work too. They just don't get `loop()`.

## Logger

`com.tonic.Logger` writes to the console under the game view. It has `norm`, `info`, `warn` and `error`, each taking a plain string or a format string with `{}` arguments. `error` also takes a `Throwable`.

[SDK index](../SDK-DOCS.md) · [Game API →](game-api.md)
