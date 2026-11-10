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
| `TileDrawingUtil` | `com.tonic.util`. Draw tile outlines and wall lines in overlays. |
| `MiniMapAPI` / `WorldMapAPI` | `com.tonic.api.widgets`. Draw paths and markers on the minimap and world map. |
| `SailingRenderUtil` | `com.tonic.api.game.sailing`. Draw the boat's exact hull, deck, route, predicted track, the sea map and the nav graph. |

[SDK index](../SDK-DOCS.md) · [← Services](services.md) · [Utilities →](utilities.md)
