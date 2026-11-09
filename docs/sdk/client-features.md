[SDK index](../SDK-DOCS.md) · [← Utilities](utilities.md)

# Client features

What VitaLite adds to the client itself, in brief. Most of these are useful to know about while testing a plugin.

## Plugins

- **Sideloading.** Jars in `~/.runelite/sideloaded-plugins` load at startup, and each gets a reload button in the plugin list so you can rebuild and reload without restarting.
- **Vita Hub.** A tab in the configuration panel for installing, updating and removing plugins from plugin repositories. The official repository is built in; to publish your own, see [VitaLitePlugins](https://github.com/Tonic-Box/VitaLitePlugins).
- **Built-in plugins:**
  - Authenticator Gen: set up two-factor authentication on an account without a phone.
  - Bank Valuer: shows what your bank's value is made up of.
  - Break Handler: settings and schedule for plugin breaks (see [Services](services.md#break-handler)).
  - Code Eval: run Java code against the live client.
  - Multi-Client Util: tools for running several clients together.
  - Enhanced Profiles: save Jagex and legacy accounts and switch between them.

## Settings panel

VitaLite's own sidebar panel has these sections:

- **General:** headless mode, headless map view, never log out when idle.
- **Logging:** packets, mouse packets, server packets, menu actions, and showing or hiding the console.
- **Caching:** cached `random.dat`, keeping the bank cache between sessions.
- **Walker:** pathfinding algorithm, drawing the walker's path, the Transport Editor.
- **Boat Debug** and **Scene:** debug overlays for boats, collision, interactable tiles and distances.
- **Input:** mouse movement spoofing and training, click strategy. See [Mouse Movement](../MOUSE-MOVEMENT.md) and [Click Manager](../CLICKMANAGER.md).
- **Debug:** the [profiler](../PROFILER.md) and platform and mouse info windows.

## Other

- **Console.** A log console under the game view, which `Logger` writes to.
- **Headless mode.** Stops drawing the game to save CPU, optionally showing a small collision map you can click to walk.
- **World map walking.** Right-click the world map to walk or sail to that spot.
- **Launch options.** Proxy, auto-login, start world, low-memory mode and more. See the table in the [README](../../README.md#client-command-line-options).
- **Updates.** VitaLite checks GitHub for new releases at startup and offers to update.

[SDK index](../SDK-DOCS.md) · [← Utilities](utilities.md)
