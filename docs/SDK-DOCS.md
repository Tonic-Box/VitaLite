# VitaLite SDK

VitaLite runs RuneLite plugins and gives them more to work with: a game API for reading the game and acting in it, queries, automation helpers, a world walker, and client services such as multi-client messaging and break scheduling. These pages list what exists and what each piece is for. They aren't a full reference; the code and its Javadoc have the details.

## Pages

1. [Getting started](sdk/getting-started.md): depending on VitaLite, `Static`, the threading rule, `VitaPlugin`, `Logger`.
2. [Game API](sdk/game-api.md): entities, game state, sailing, interfaces, in-game settings, and the threaded task classes.
3. [Queries and wrappers](sdk/queries-and-wrappers.md): searching NPCs, items, objects, widgets and worlds, and the objects queries return.
4. [Automation](sdk/automation.md): step handlers and their builders, the Script DSL, loadouts, dialogue trees.
5. [Navigation](sdk/navigation.md): the walker, pathfinding, transports and sailing routes.
6. [Game data](sdk/game-data.md): enums and constants for spells, skills, shops, locations and ids.
7. [Services](sdk/services.md): game state caches, break handler, messaging between clients, clicks and mouse, settings storage, proxies, packet events.
8. [UI](sdk/ui.md): Swing components, overlay drawing helpers and ground overlays.
9. [Utilities](sdk/utilities.md): threads, reflection, text, distances and points.
10. [Client features](sdk/client-features.md): sideloading, Vita Hub, built-in plugins, the settings panel and launch options.

## Other docs

- [Plugin Dev Guide](EXTERNALPLUGIN.md): setting up a plugin project.
- [Script DSL](SCRIPT-DSL.md): writing tasks as scripts.
- [Click Manager](CLICKMANAGER.md): click strategies and where clicks land.
- [Mouse Movement](MOUSE-MOVEMENT.md): recording and generating mouse movement.
- [Profiler](PROFILER.md): the built-in JVM profiler.
