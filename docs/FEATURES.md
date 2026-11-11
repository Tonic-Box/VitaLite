# VitaLite Features

What VitaLite adds on top of RuneLite. Most of it is in VitaLite's settings panel in the sidebar.

## Plugins

- **Sideloading.** Put plugin jars in `~/.runelite/sideloaded-plugins` and they load at startup. Each one gets a reload button in the plugin list, so an updated jar can be reloaded without restarting the client.
- **Vita Hub.** A tab in the configuration panel for installing, updating and removing plugins. The official repository is built in, and you can add other repositories. Plugins from unofficial repositories aren't checked by VitaLite, so only add ones you trust.
- **Built-in plugins:**
  - **Enhanced Profiles:** save Jagex and legacy accounts and switch between them.
  - **Break Handler:** schedule breaks for plugins that support it.
  - **Bank Valuer:** shows what your bank's value is made up of.
  - **Authenticator Gen:** set up two-factor authentication on an account without a phone.
  - **Multi-Client Util:** tools for running several clients together.
  - **Code Eval:** run Java code against the live client.

## Playing

- **World map walking.** Right-click the world map to walk, or sail, to that spot from wherever you are.
- **Never log out.** Turn off the idle logout.
- **Headless mode.** Stop drawing the game to save CPU. You can show a small collision map instead and click it to walk.

## Accounts

- **Cached random.dat.** Keeps a separate `random.dat` for each account.
- **Bank cache.** Remembers each account's bank contents, optionally between sessions, so plugins can check them while the bank is closed.

## Input

- **Click strategy.** Choose where plugin clicks land: a fixed point, a random point, inside the clicked object, or no click at all. See [Click Manager](CLICKMANAGER.md).
- **Mouse movement.** Record your own mouse movement and have VitaLite generate similar movement for plugin actions. See [Mouse Movement](MOUSE-MOVEMENT.md).

## Tools

- **Console.** A log console under the game view, which you can hide.
- **Logging.** Log packets, mouse packets, server packets and menu actions to the console.
- **Profiler.** A built-in JVM profiler for checking performance. See [Profiler](PROFILER.md).
- **Debug overlays.** Collision, interactable tiles, boat hull and deck, distances, and the walker's path.
- **Ground overlays under objects.** Tile overlays from any plugin are drawn on the ground in the 3D scene, so trees, walls and characters in front of them hide them. Works with the software renderer, the GPU plugin and 117 HD.
- **Transport Editor.** Add and edit the doors, stairs and other links the walker uses.

## Launching

- **Launch options.** Proxy, auto-login, start world, low-memory mode and more. See the table in the [README](../README.md#client-command-line-options).
- **Updates.** VitaLite checks for a new release at startup and offers to update.
