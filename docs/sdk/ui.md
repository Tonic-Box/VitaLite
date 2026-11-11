[SDK index](../SDK-DOCS.md) · [← Services](services.md) · [Utilities →](utilities.md)

# UI

## Components

`com.tonic.model.ui.components`: Swing components in VitaLite's dark style, for plugin panels and windows.

| Class | What it is |
|---|---|
| `VPluginPanel` | A sidebar panel base, used instead of RuneLite's `PluginPanel`. Turn it into one RuneLite accepts with `PluginPanelConverter.toPluginPanel(panel)`. |
| `FancyButton` | A flat button. |
| `FancyDropdown` | A dropdown built from a list, an array or an enum class. |
| `ToggleSlider` | An on/off switch. |
| `FancySpinner` / `FancyDualSpinner` | A number field with min and max, and a pair of them for a range. |
| `FancyCard` | A header card with a title and a tagline. |
| `CollapsiblePanel` | A titled section that expands and collapses. |
| `OptionPanel` | A settings row: title, description and a `ToggleSlider`. |
| `VitaFrame` | A window with VitaLite's own title bar. |

## Overlays and drawing

| Class | What it is |
|---|---|
| `VitaOverlay` | `com.tonic.ui`. A movable info box overlay. Fill it with lines of text using `clear()` and `newLine(...)`. |
| `TileDrawingUtil` | `com.tonic.util`. Draw tile outlines and wall lines in overlays (moved onto the ground with the setting on). |
| `MiniMapAPI` / `WorldMapAPI` | `com.tonic.api.widgets`. Draw paths and markers on the minimap and world map. |
| `SailingRenderUtil` | `com.tonic.api.game.sailing`. Draw the boat's exact hull, deck, route, predicted track, the sea map and the nav graph. |

## Ground overlays

`com.tonic.services.ground`. Shapes drawn on the ground in the 3D scene, so trees, walls, NPCs and players standing in front of them hide them. Works with the software renderer, the GPU plugin and 117 HD.

- **RuneLite overlays need no changes.** With *Ground Overlays Under Objects* on (VitaLite options, Scene), any polygon or line an `ABOVE_SCENE`, `UNDER_WIDGETS` or `ABOVE_WIDGETS` overlay draws through ground points (`Perspective.getCanvasTilePoly`, `getCanvasTileAreaPoly`, `localToCanvas` at ground height) is moved onto the ground. Text, hulls and images stay on top. Outlines become borders inside the tile, 2.5 game units wide per pixel of stroke.
- **Drawing your own.** Implement `GroundOverlay` and register it with `GroundOverlays.add(overlay)`. Remove it with `GroundOverlays.remove(overlay)`.

| Method on `GroundCanvas` | What it draws |
|---|---|
| `fillTile` / `outlineTile` / `markTile` | A tile's fill, a border inside it, or both as RuneLite's tile markers look. |
| `fillArea` / `outlineArea` | A rectangle of whole tiles. |
| `tileEdge` | A line along one `TileSide` of a tile, inside it. |
| `line` / `path` | A straight line, or a line through the centres of a list of tiles. |
| `fillPolygon` / `outlinePolygon` | Any polygon whose sides don't cross, in world tiles. |
| `onView` | The canvas of another world view, such as a boat, moving with it. |

Widths are in game units, 128 to a tile (`GroundCanvas.THIN` is 6, `REGULAR` is 12). Colours use their alpha as opacity. Return a value from `dependsOn()` to keep the shapes from the last frame while it stays equal.

```java
GroundOverlays.add(canvas -> canvas.markTile(3222, 3218, 0, Color.CYAN, new Color(0, 255, 255, 50), GroundCanvas.REGULAR));
```

With the setting off, `GroundOverlay` shapes are drawn flat on top, as other overlays are.

[SDK index](../SDK-DOCS.md) · [← Services](services.md) · [Utilities →](utilities.md)
