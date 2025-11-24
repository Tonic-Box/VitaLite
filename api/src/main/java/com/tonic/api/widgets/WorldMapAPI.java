package com.tonic.api.widgets;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A utility class providing methods to interact with the floating world map in RuneLite.
 * <p>
 * This class includes functions to convert screen coordinates to world points,
 * check if the world map is open, draw markers and images on the map, and more.
 * It is designed to facilitate plugin development that requires interaction with
 * the world map interface.
 */
public class WorldMapAPI
{
    /**
     * Converts world map click coordinates to a WorldPoint.
     * <p>
     * This method handles the coordinate transformation from screen pixel coordinates
     * on the world map widget to the corresponding world coordinates, taking into
     * account map zoom level and current map position.
     *
     * @param client the RuneLite client instance
     * @param clickX the x-coordinate of the mouse click on the screen
     * @param clickY the y-coordinate of the mouse click on the screen
     * @return the corresponding WorldPoint, or null if the conversion fails
     *         (e.g., if the world map is not open or coordinates are invalid)
     */
    public static WorldPoint convertMapClickToWorldPoint(Client client, int clickX, int clickY) {
        if (client == null) {
            return null;
        }

        WorldMap worldMap = client.getWorldMap();
        if (worldMap == null) {
            return null;
        }

        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (mapWidget == null) {
            return null;
        }

        // Get map properties
        float zoom = worldMap.getWorldMapZoom();
        Point mapWorldPosition = worldMap.getWorldMapPosition();
        Rectangle mapBounds = mapWidget.getBounds();

        if(!mapBounds.contains(clickX, clickY)) {
            return null; // Click is outside the map bounds
        }

        // Calculate the center point of the map in screen coordinates
        WorldPoint mapCenterWorldPoint = new WorldPoint(mapWorldPosition.getX(), mapWorldPosition.getY(), 0);
        Integer centerScreenX = mapWorldPointToScreenX(client, mapCenterWorldPoint);
        Integer centerScreenY = mapWorldPointToScreenY(client, mapCenterWorldPoint);

        if (centerScreenX == null || centerScreenY == null) {
            return null;
        }

        // Calculate the offset from the center in screen coordinates
        int deltaX = clickX - centerScreenX;
        int deltaY = clickY - centerScreenY;

        // Convert screen pixel offset to world tile offset
        int worldDeltaX = (int) (deltaX / zoom);
        int worldDeltaY = (int) (-deltaY / zoom); // Y is inverted

        // Calculate final world coordinates
        int worldX = mapWorldPosition.getX() + worldDeltaX;
        int worldY = mapWorldPosition.getY() + worldDeltaY;

        // Validate coordinates are reasonable (basic bounds check)
        if (worldX < 0 || worldY < 0 || worldX > 10000 || worldY > 10000) {
            return null;
        }

        return new WorldPoint(worldX, worldY, 0);
    }

    /**
     * Converts world map click coordinates to a WorldPoint with specific plane.
     * <p>
     * This is an overload of {@link #convertMapClickToWorldPoint(Client, int, int)}
     * that allows specifying the plane/floor level for the resulting WorldPoint.
     *
     * @param client the RuneLite client instance
     * @param clickX the x-coordinate of the mouse click on the screen
     * @param clickY the y-coordinate of the mouse click on the screen
     * @param plane the plane/floor level (0-3, where 0 is ground level)
     * @return the corresponding WorldPoint with the specified plane, or null if conversion fails
     */
    public static WorldPoint convertMapClickToWorldPoint(Client client, int clickX, int clickY, int plane) {
        WorldPoint basePoint = convertMapClickToWorldPoint(client, clickX, clickY);
        if (basePoint == null) {
            return null;
        }

        // Clamp plane to valid range
        int validPlane = Math.max(0, Math.min(3, plane));

        return new WorldPoint(basePoint.getX(), basePoint.getY(), validPlane);
    }

    /**
     * Converts a WorldPoint to the corresponding screen X coordinate on the world map.
     * <p>
     * This is useful for drawing overlays or determining if a world point is visible
     * on the current map view.
     *
     * @param client the RuneLite client instance
     * @param worldPoint the world point to convert
     * @return the screen X coordinate, or null if the conversion fails
     */
    public static Integer mapWorldPointToScreenX(Client client, WorldPoint worldPoint) {
        if (client == null || worldPoint == null) {
            return null;
        }

        WorldMap worldMap = client.getWorldMap();
        if (worldMap == null) {
            return null;
        }

        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (mapWidget == null) {
            return null;
        }

        float pixelsPerTile = worldMap.getWorldMapZoom();
        Rectangle worldMapRect = mapWidget.getBounds();
        Point worldMapPosition = worldMap.getWorldMapPosition();

        int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
        int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

        int xGraphDiff = (int) (xTileOffset * pixelsPerTile);
        xGraphDiff += (int) (pixelsPerTile - Math.ceil(pixelsPerTile / 2));
        xGraphDiff += (int) worldMapRect.getX();

        return xGraphDiff;
    }

    /**
     * Converts a WorldPoint to the corresponding screen Y coordinate on the world map.
     * <p>
     * This is useful for drawing overlays or determining if a world point is visible
     * on the current map view.
     *
     * @param client the RuneLite client instance
     * @param worldPoint the world point to convert
     * @return the screen Y coordinate, or null if the conversion fails
     */
    public static Integer mapWorldPointToScreenY(Client client, WorldPoint worldPoint) {
        if (client == null || worldPoint == null) {
            return null;
        }

        WorldMap worldMap = client.getWorldMap();
        if (worldMap == null) {
            return null;
        }

        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (mapWidget == null) {
            return null;
        }

        float pixelsPerTile = worldMap.getWorldMapZoom();
        Rectangle worldMapRect = mapWidget.getBounds();
        Point worldMapPosition = worldMap.getWorldMapPosition();

        int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);
        int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
        int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;

        int yGraphDiff = (int) (yTileOffset * pixelsPerTile);
        yGraphDiff -= (int) (pixelsPerTile - Math.ceil(pixelsPerTile / 2));
        yGraphDiff = worldMapRect.height - yGraphDiff;
        yGraphDiff += (int) worldMapRect.getY();

        return yGraphDiff;
    }

    /**
     * Checks if the world map is currently open and ready for coordinate conversion.
     *
     * @param client the RuneLite client instance
     * @return true if the world map is open and ready for use, false otherwise
     */
    public static boolean isWorldMapOpen(Client client) {
        if (client == null) {
            return false;
        }

        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        return WidgetAPI.isVisible(mapWidget);
    }

    /**
     * Checks if the given screen coordinates are within the world map bounds.
     *
     * @param client the RuneLite client instance
     * @param screenX the screen X coordinate to check
     * @param screenY the screen Y coordinate to check
     * @return true if the coordinates are within the map bounds, false otherwise
     */
    public static boolean isClickWithinMapBounds(Client client, int screenX, int screenY) {
        if (client == null) {
            return false;
        }

        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (mapWidget == null) {
            return false;
        }

        Rectangle mapBounds = mapWidget.getBounds();
        return mapBounds.contains(screenX, screenY);
    }

    /**
     * Draws a path of colored squares on the floating world map at the specified world points.
     * <p>
     * This method renders a series of colored squares at the given world coordinates on the
     * world map interface. It ensures that the squares are only drawn within the visible
     * map area and takes into account any UI elements that may obscure parts of the map.
     *
     * @param graphics the Graphics2D context to draw on
     * @param worldPoints the list of WorldPoints where squares should be drawn
     * @param color the color of the squares to draw
     */
    public static void drawPath(Graphics2D graphics, List<WorldPoint> worldPoints, Color color)
    {
        if (WidgetAPI.get(InterfaceID.Worldmap.MAP_CONTAINER) == null) {
            return;
        }

        Area mapClipArea = getWorldMapClipArea(WidgetAPI.get(InterfaceID.Worldmap.MAP_CONTAINER).getBounds());
        for(WorldPoint point : worldPoints)
        {
            drawOnMap(graphics, mapClipArea, point, color);
        }
    }

    private static Area getWorldMapClipArea(Rectangle baseRectangle) {
        final Widget overview = WidgetAPI.get(InterfaceID.Worldmap.OVERVIEW_CONTAINER);
        final Widget surfaceSelector = WidgetAPI.get(InterfaceID.Worldmap.MAPLIST_BOX_GRAPHIC0);

        Area clipArea = new Area(baseRectangle);

        if (WidgetAPI.isVisible(overview)) {
            clipArea.subtract(new Area(overview.getBounds()));
        }

        if (WidgetAPI.isVisible(surfaceSelector)) {
            clipArea.subtract(new Area(surfaceSelector.getBounds()));
        }

        return clipArea;
    }

    private static void drawOnMap(Graphics2D graphics, Area mapClipArea, WorldPoint point, Color color) {
        Client client = Static.getClient();
        WorldMap worldMap = client.getWorldMap();
        if (worldMap == null) {
            return;
        }

        Point start = mapWorldPointToGraphicsPoint(point);
        Point end = mapWorldPointToGraphicsPoint(point.dx(1).dy(-1));

        if (start == null || end == null) {
            return;
        }

        if (!mapClipArea.contains(start.getX(), start.getY()) || !mapClipArea.contains(end.getX(), end.getY())) {
            return;
        }

        graphics.setColor(color);
        graphics.fillRect(start.getX(), start.getY(), end.getX() - start.getX(), end.getY() - start.getY());
    }

    private static Point mapWorldPointToGraphicsPoint(WorldPoint worldPoint)
    {
        Client client = Static.getClient();
        //RenderOverview ro = client.getRenderOverview();
        WorldMap worldMap = client.getWorldMap();

        if (!worldMap.getWorldMapData().surfaceContainsPosition(worldPoint.getX(), worldPoint.getY()))
        {
            return null;
        }

        float pixelsPerTile = worldMap.getWorldMapZoom();

        Widget map = WidgetAPI.get(InterfaceID.Worldmap.MAP_CONTAINER);
        if (map != null)
        {
            Rectangle worldMapRect = map.getBounds();

            int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
            int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

            Point worldMapPosition = worldMap.getWorldMapPosition();

            //Offset in tiles from anchor sides
            int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
            int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;
            int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

            int xGraphDiff = ((int) (xTileOffset * pixelsPerTile));
            int yGraphDiff = (int) (yTileOffset * pixelsPerTile);

            //Center on tile.
            yGraphDiff -= (int) (pixelsPerTile - Math.ceil(pixelsPerTile / 2));
            xGraphDiff += (int) (pixelsPerTile - Math.ceil(pixelsPerTile / 2));

            yGraphDiff = worldMapRect.height - yGraphDiff;
            yGraphDiff += (int) worldMapRect.getY();
            xGraphDiff += (int) worldMapRect.getX();

            return new Point(xGraphDiff - 10, yGraphDiff);
        }
        return null;
    }

    private static void drawLine(Graphics g1, Point a, Point b, Color color) {
        Graphics2D g = (Graphics2D) g1.create();
        g.setColor(color);
        int x1 = a.getX();
        int y1 = a.getY();
        int x2 = b.getX();
        int y2 = b.getY();

        x1 += (x1>x2)?-3:((x1<x2)?3:0);
        y1 += (y1>y2)?-3:((y1<y2)?3:0);
        x2 += (x1>x2)?3:((x1<x2)?-3:0);
        y2 += (y1>y2)?3:((y1<y2)?-3:0);

        double dx = x2 - x1, dy = y2 - y1;
        double angle = Math.atan2(dy, dx);
        int len = (int) Math.sqrt(dx*dx + dy*dy);
        AffineTransform at = AffineTransform.getTranslateInstance(x1, y1);
        at.concatenate(AffineTransform.getRotateInstance(angle));
        g.transform(at);

        g.drawLine(0, 0, len, 0);
    }

    /**
     * Draws a BufferedImage at a WorldPoint on the floating world map with customizable offset.
     * <p>
     * This method renders an image at the specified world coordinates with a configurable
     * offset from the tile position. The offset allows precise positioning relative to
     * the world point. The image will only be drawn if it falls within the visible map
     * area and the world map is currently open.
     *
     * @param graphics the Graphics2D context to draw on
     * @param worldPoint the world point to use as the reference position
     * @param image the BufferedImage to draw
     * @param width the desired width of the image in pixels
     * @param height the desired height of the image in pixels
     * @param offsetX the horizontal offset from the world point in pixels (negative = left, positive = right)
     * @param offsetY the vertical offset from the world point in pixels (negative = up, positive = down)
     */
    public static void drawImageAtWorldPoint(Graphics2D graphics, WorldPoint worldPoint, BufferedImage image, int width, int height, int offsetX, int offsetY) {
        if (graphics == null || worldPoint == null || image == null) {
            return;
        }

        Client client = Static.getClient();
        if (client == null) {
            return;
        }

        // Check if world map is open
        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (!WidgetAPI.isVisible(mapWidget)) {
            return;
        }

        // Get screen coordinates for the world point
        Integer screenX = mapWorldPointToScreenX(client, worldPoint);
        Integer screenY = mapWorldPointToScreenY(client, worldPoint);

        if (screenX == null || screenY == null) {
            return;
        }

        // Calculate position with offset
        int drawX = screenX + offsetX;
        int drawY = screenY + offsetY;

        // Get the map clipping area to ensure we only draw within map bounds
        Area mapClipArea = getWorldMapClipArea(mapWidget.getBounds());

        // Create a bounding rectangle for the image
        Rectangle imageBounds = new Rectangle(drawX, drawY, width, height);

        // Check if image is at least partially within the map area
        if (!mapClipArea.intersects(imageBounds)) {
            return;
        }

        // Save the current clip
        Shape oldClip = graphics.getClip();

        // Set clip to map area to prevent drawing outside map bounds
        graphics.setClip(mapClipArea);

        // Draw the image at the specified offset from the world point
        graphics.drawImage(image, drawX, drawY, width, height, null);

        // Restore the original clip
        graphics.setClip(oldClip);
    }

    /**
     * Convenience method to draw a BufferedImage centered over a WorldPoint.
     * <p>
     * This is a helper method that automatically calculates the offset needed
     * to center the image over the world point.
     *
     * @param graphics the Graphics2D context to draw on
     * @param worldPoint the world point where the image should be centered
     * @param image the BufferedImage to draw
     * @param width the desired width of the image in pixels
     * @param height the desired height of the image in pixels
     */
    public static void drawImageAtWorldPointCentered(Graphics2D graphics, WorldPoint worldPoint,
                                                     BufferedImage image, int width, int height) {
        // Calculate offsets to center the image
        int offsetX = -width / 2;
        int offsetY = -height / 2;

        drawImageAtWorldPoint(graphics, worldPoint, image, width, height, offsetX, offsetY);
    }

    /**
     * Draws a smooth, fluid teardrop/pin marker pointing to a WorldPoint on the floating world map.
     * <p>
     * This method renders a Google Maps-style marker with smooth curves using Bezier paths.
     * The point is positioned exactly on the specified world coordinates.
     *
     * @param graphics the Graphics2D context to draw on
     * @param worldPoint the world point where the marker point should be positioned
     * @param color the fill color of the marker
     * @param size the size of the marker (recommended: 20-40 for good visibility)
     */
    public static void drawMapMarker(Graphics2D graphics, WorldPoint worldPoint, Color color, int size) {
        if (graphics == null || worldPoint == null || color == null) {
            return;
        }

        Client client = Static.getClient();
        if (client == null) {
            return;
        }

        // Check if world map is open
        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (!WidgetAPI.isVisible(mapWidget)) {
            return;
        }

        // Get screen coordinates for the world point
        Integer screenX = mapWorldPointToScreenX(client, worldPoint);
        Integer screenY = mapWorldPointToScreenY(client, worldPoint);

        if (screenX == null || screenY == null) {
            return;
        }

        screenX -= 7;
        screenY += 3;

        // Get the map clipping area
        Area mapClipArea = getWorldMapClipArea(mapWidget.getBounds());

        // Check if the marker position is within the map area
        if (!mapClipArea.contains(screenX, screenY)) {
            return;
        }

        // Save the current state
        Shape oldClip = graphics.getClip();
        Object oldAntialiasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Stroke oldStroke = graphics.getStroke();

        // Set clip to map area and enable antialiasing for smooth curves
        graphics.setClip(mapClipArea);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate dimensions
        double scale = size / 30.0; // Base size is 30

        // Create the smooth teardrop shape
        Path2D.Double markerPath = createFluidMarkerPath(screenX, screenY, scale);

        // Draw shadow
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.translate(2 * scale, 3 * scale); // Shadow offset
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));

        // Gradient shadow for more depth
        GradientPaint shadowGradient = new GradientPaint(
                screenX, (float)(screenY - 20 * scale), new Color(0, 0, 0, 60),
                screenX, screenY, new Color(0, 0, 0, 30)
        );
        g2.setPaint(shadowGradient);
        g2.fill(markerPath);
        g2.dispose();

        // Draw main marker with gradient for depth
        GradientPaint markerGradient = new GradientPaint(
                (float)(screenX - 10 * scale), (float)(screenY - 30 * scale),
                brighter(color, 0.3f),
                (float)(screenX + 10 * scale), (float)(screenY - 10 * scale),
                color
        );
        graphics.setPaint(markerGradient);
        graphics.fill(markerPath);

        // Draw inner circle (the "hole" in the pin)
        double innerCircleRadius = 5 * scale;
        double innerCircleY = screenY - 28 * scale;
        Ellipse2D.Double innerCircle = new Ellipse2D.Double(
                screenX - innerCircleRadius,
                innerCircleY - innerCircleRadius,
                innerCircleRadius * 2,
                innerCircleRadius * 2
        );

        // Inner circle with slight transparency
        graphics.setColor(new Color(255, 255, 255, 200));
        graphics.fill(innerCircle);

        // Draw border
        graphics.setColor(darker(color, 0.4f));
        graphics.setStroke(new BasicStroke((float)(1.2 * scale)));
        graphics.draw(markerPath);

        // Draw inner circle border
        graphics.setColor(new Color(255, 255, 255, 150));
        graphics.setStroke(new BasicStroke((float)(0.8 * scale)));
        graphics.draw(innerCircle);

        // Add highlight for glossy effect
        Graphics2D g3 = (Graphics2D) graphics.create();
        g3.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

        // Create highlight shape
        Path2D.Double highlight = new Path2D.Double();
        highlight.moveTo(screenX - 8 * scale, screenY - 32 * scale);
        highlight.quadTo(
                screenX, screenY - 35 * scale,
                screenX + 8 * scale, screenY - 32 * scale
        );
        highlight.quadTo(
                screenX + 6 * scale, screenY - 28 * scale,
                screenX, screenY - 26 * scale
        );
        highlight.quadTo(
                screenX - 6 * scale, screenY - 28 * scale,
                screenX - 8 * scale, screenY - 32 * scale
        );
        highlight.closePath();

        GradientPaint highlightGradient = new GradientPaint(
                screenX, (float)(screenY - 35 * scale), new Color(255, 255, 255, 180),
                screenX, (float)(screenY - 26 * scale), new Color(255, 255, 255, 0)
        );
        g3.setPaint(highlightGradient);
        g3.fill(highlight);
        g3.dispose();

        // Restore original state
        graphics.setClip(oldClip);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
        graphics.setStroke(oldStroke);
    }

    private static Path2D.Double createFluidMarkerPath(int x, int y, double scale) {
        Path2D.Double path = new Path2D.Double();

        // Start from the point tip
        path.moveTo(x, y);

        // Left curve up to the bulb
        path.curveTo(
                x - 3 * scale, y - 8 * scale,   // Control point 1
                x - 8 * scale, y - 15 * scale,  // Control point 2
                x - 11 * scale, y - 22 * scale  // End point
        );

        // Left side of bulb
        path.curveTo(
                x - 14 * scale, y - 26 * scale,  // Control point 1
                x - 14 * scale, y - 31 * scale,  // Control point 2
                x - 12 * scale, y - 35 * scale   // End point
        );

        // Top curve of bulb
        path.curveTo(
                x - 9 * scale, y - 39 * scale,   // Control point 1
                x - 4 * scale, y - 41 * scale,   // Control point 2
                x, y - 41 * scale                 // Top center
        );

        // Right side - mirror of left
        path.curveTo(
                x + 4 * scale, y - 41 * scale,   // Control point 1
                x + 9 * scale, y - 39 * scale,   // Control point 2
                x + 12 * scale, y - 35 * scale   // End point
        );

        // Right side of bulb
        path.curveTo(
                x + 14 * scale, y - 31 * scale,  // Control point 1
                x + 14 * scale, y - 26 * scale,  // Control point 2
                x + 11 * scale, y - 22 * scale   // End point
        );

        // Right curve down to the point
        path.curveTo(
                x + 8 * scale, y - 15 * scale,   // Control point 1
                x + 3 * scale, y - 8 * scale,    // Control point 2
                x, y                              // Back to point tip
        );

        path.closePath();
        return path;
    }

    /**
     * Helper method to create a brighter version of a color.
     */
    private static Color brighter(Color color, float factor) {
        int r = Math.min(255, (int)(color.getRed() + (255 - color.getRed()) * factor));
        int g = Math.min(255, (int)(color.getGreen() + (255 - color.getGreen()) * factor));
        int b = Math.min(255, (int)(color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(r, g, b, color.getAlpha());
    }

    /**
     * Helper method to create a darker version of a color.
     */
    private static Color darker(Color color, float factor) {
        int r = Math.max(0, (int)(color.getRed() * (1 - factor)));
        int g = Math.max(0, (int)(color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int)(color.getBlue() * (1 - factor)));
        return new Color(r, g, b, color.getAlpha());
    }

    /**
     * Convenience method to draw a standard-sized red marker.
     *
     * @param graphics the Graphics2D context to draw on
     * @param worldPoint the world point where the marker should be positioned
     */
    public static void drawRedMapMarker(Graphics2D graphics, WorldPoint worldPoint) {
        drawMapMarker(graphics, worldPoint, new Color(220, 20, 60), 30); // Crimson red, size 30
    }

    /**
     * Convenience method to draw a standard-sized cyan marker.
     *
     * @param graphics the Graphics2D context to draw on
     * @param worldPoint the world point where the marker should be positioned
     */
    public static void drawGreenMapMarker(Graphics2D graphics, WorldPoint worldPoint) {
        drawMapMarker(graphics, worldPoint, Color.GREEN, 30); // Crimson red, size 30
    }
}
