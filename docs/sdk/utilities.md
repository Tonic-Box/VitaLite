[SDK index](../SDK-DOCS.md) · [← UI](ui.md) · [Client features →](client-features.md)

# Utilities

## General

`com.tonic.util` in `base-api`.

| Class | What it is |
|---|---|
| `ThreadPool` | VitaLite's shared worker threads: `submit` work, or `schedule` it after a delay. |
| `ReflectBuilder` | Chain field reads and method calls by name through reflection, using RuneLite's class loader. |
| `ReflectUtil` | Single reflection calls: get a field, call a method, create an instance. |
| `TextUtil` | Text encoding (CP1252, UTF-8) and small string helpers. |
| `Pair` / `IntPair` | Two values together. `IntPair.randomEnclosed()` picks a number in its range. |
| `Profiler` | A simple start and stop timer for measuring code. |
| `ResourceUtil` | Load an image from your plugin's resources. |
| `StackTraceUtil` | Readable stack traces and the calling method, for debugging. |
| `StaticIntFinder` | Find the name of the constant that holds a given number in a class. |

## Game helpers

`com.tonic.util` in `api`.

| Class | What it is |
|---|---|
| `Distance` | Distances between points: walking path, straight line, Manhattan, Chebyshev, diagonal. |
| `Location` | Whether a point is in an area or the wilderness, and the points around one. |
| `WorldPointUtil` | Convert points between instances and their templates, and pack points into single ints. |
| `ActorPathing` | Line-of-walk checks and how NPCs path towards a target. |
| `MessageUtil` | Put a message in your own chat box, or send to public, friends or clan chat. |

[SDK index](../SDK-DOCS.md) · [← UI](ui.md) · [Client features →](client-features.md)
