package com.tonic.api.widgets;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;

import java.awt.*;

public class MinimapAPI
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

        Widget mapWidget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
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

        Widget mapWidget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if (mapWidget == null) {
            return null;
        }

        float pixelsPerTile = worldMap.getWorldMapZoom();
        Rectangle worldMapRect = mapWidget.getBounds();
        Point worldMapPosition = worldMap.getWorldMapPosition();

        int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
        int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

        int xGraphDiff = (int) (xTileOffset * pixelsPerTile);
        xGraphDiff += pixelsPerTile - Math.ceil(pixelsPerTile / 2);
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

        Widget mapWidget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
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
        yGraphDiff -= pixelsPerTile - Math.ceil(pixelsPerTile / 2);
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

        Widget mapWidget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        return mapWidget != null && !mapWidget.isHidden();
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

        Widget mapWidget = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if (mapWidget == null) {
            return false;
        }

        Rectangle mapBounds = mapWidget.getBounds();
        return mapBounds.contains(screenX, screenY);
    }
}
