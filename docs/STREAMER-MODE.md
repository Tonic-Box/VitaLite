# Streamer Mode

Streamer mode opens a second window, the **mirror**, that looks like stock RuneLite. Capture the mirror in OBS or Stream it directly and keep playing in VitaLite.

The mirror shows only stock content: the RuneLite client, its built-in plugins and Plugin Hub plugins. Everything VitaLite or its plugins add is left out.

## Turning it on

- The **Streamer mode** button in the title bar, or
- **VitaLite Options -> Streamer -> Streamer Mode**.

Closing the mirror window turns streamer mode off.

## Settings

In **VitaLite Options -> Streamer**:

| Setting | What it does |
|---|---|
| Streamer Mode | Opens or closes the mirror. |
| Streamer Name | Shown in the mirror in place of your name: chat, overheads, menus, the login screen and the title. Up to 12 characters; blank shows `StreamerMode`. |
| Mirror Frame Rate | The most frames a second the mirror shows: 60 or 30. 30 costs less while streaming. |

## What the mirror hides

- Overlays, infoboxes, tooltips and world map icons from non-stock plugins.
- Chat messages they add.
- Menu entries they add.
- Notification flashes they start.
- Their sidebar tabs, title bar buttons and plugin list rows.
- VitaLite's recolours and its own settings switches. The mirror uses RuneLite's colours.
- Ground overlays drawn under objects show flat in the mirror, as stock RuneLite draws them.
- VitaLite's world select. While streamer mode is on, the login screen uses Jagex's world select in both windows.

The mirror shows stand-ins for the RuneLite plugins VitaLite never loads, such as Discord and Twitch, so it looks complete.

## Reordering while streaming

Some lists in VitaLite are reordered so each row sits where the mirror shows it. Hovering a row in VitaLite then highlights the same row in the mirror. Everything goes back to normal when streamer mode turns off.

| Where | Order while streaming |
|---|---|
| Sidebar tabs | Stock tabs first, the others after. |
| Title bar buttons | Stock buttons last, the others before them. |
| Plugin list | Stock plugins first, the others at the bottom. Pins and search still work. Discord and Twitch get placeholder rows (star and name only) so the rows line up with the mirror's. |

## The mirror window

- Drawn 1:1 at the client's size. It follows the client when the client resizes, and can't be resized or maximized by hand.
- The cursor, hover highlights and title bar buttons follow what you do in VitaLite.
- Minimizing the mirror pauses it, at no cost to the client.
- Works with the CPU renderer, the GPU plugin and 117 HD.
