package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.services.pathfinder.sailing.BoatCollisionAPI;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.*;
import java.util.Collection;

/**
 * Static utility class for rendering boat hull overlays.
 * Provides methods to outline the ship's hull in its current rotation.
 */
public class SailingRenderUtil
{
    // Default colors for hull rendering
    private static final Color DEFAULT_HULL_COLOR = new Color(255, 165, 0); // Orange
    private static final Color DEFAULT_HULL_FILL = new Color(255, 165, 0, 50); // Semi-transparent orange
    private static final Color DEFAULT_DECK_COLOR = new Color(0, 255, 0); // Green
    private static final Color DEFAULT_DECK_FILL = new Color(0, 255, 0, 30); // Semi-transparent green

    private static final Stroke DEFAULT_STROKE = new BasicStroke(2.0f);
    private static final Stroke THICK_STROKE = new BasicStroke(3.0f);

    /**
     * Renders the player's boat hull outline with default colors.
     * Hull tiles are shown in orange, representing the collision footprint.
     *
     * @param graphics the Graphics2D context
     */
    public static void renderPlayerBoatHull(Graphics2D graphics)
    {
        renderPlayerBoatHull(graphics, DEFAULT_HULL_COLOR, DEFAULT_HULL_FILL, DEFAULT_STROKE);
    }

    /**
     * Renders the player's boat hull outline with custom colors.
     * The hull is automatically rotated based on the boat's current orientation.
     *
     * @param graphics the Graphics2D context
     * @param outlineColor the color for the hull outline
     * @param fillColor the fill color (use alpha for transparency)
     * @param stroke the stroke style for the outline
     */
    public static void renderPlayerBoatHull(Graphics2D graphics, Color outlineColor, Color fillColor, Stroke stroke)
    {
        // Get player boat hull (collision tiles in main world, already rotated)
        Collection<WorldPoint> hullTiles = BoatCollisionAPI.getPlayerBoatCollision();

        if (hullTiles == null || hullTiles.isEmpty()) {
            return;
        }

        renderWorldPointCollection(graphics, hullTiles, outlineColor, fillColor, stroke);
    }

    /**
     * Renders the player's boat deck (walkable areas) with default colors.
     * Deck tiles are shown in green, representing where you can walk on the boat.
     *
     * @param graphics the Graphics2D context
     */
    public static void renderPlayerBoatDeck(Graphics2D graphics)
    {
        renderPlayerBoatDeck(graphics, DEFAULT_DECK_COLOR, DEFAULT_DECK_FILL, DEFAULT_STROKE);
    }

    /**
     * Renders the player's boat deck (walkable areas) with custom colors.
     *
     * @param graphics the Graphics2D context
     * @param outlineColor the color for the deck outline
     * @param fillColor the fill color (use alpha for transparency)
     * @param stroke the stroke style for the outline
     */
    public static void renderPlayerBoatDeck(Graphics2D graphics, Color outlineColor, Color fillColor, Stroke stroke)
    {
        // Get player boat deck (walkable tiles in main world, already rotated)
        Collection<WorldPoint> deckTiles = BoatCollisionAPI.getPlayerBoatDeck();

        if (deckTiles == null || deckTiles.isEmpty()) {
            return;
        }

        renderWorldPointCollection(graphics, deckTiles, outlineColor, fillColor, stroke);
    }

    /**
     * Renders both hull and deck together with default colors.
     * Hull in orange, deck in green.
     *
     * @param graphics the Graphics2D context
     */
    public static void renderPlayerBoatComplete(Graphics2D graphics)
    {
        // Render hull first (background)
        renderPlayerBoatHull(graphics);

        // Render deck on top
        renderPlayerBoatDeck(graphics);
    }

    /**
     * Renders the boat's center point as a marker.
     * Useful for debugging boat positioning.
     *
     * @param graphics the Graphics2D context
     * @param color the color for the center marker
     */
    public static void renderBoatCenter(Graphics2D graphics, Color color)
    {
        WorldPoint boatCenter = BoatCollisionAPI.getPlayerBoatWorldPoint();
        if (boatCenter == null) {
            return;
        }

        Client client = Static.getClient();
        LocalPoint localPoint = LocalPoint.fromWorld(client, boatCenter);
        if (localPoint == null) {
            return;
        }

        Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
        if (polygon == null) {
            return;
        }

        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
        OverlayUtil.renderPolygon(graphics, polygon, color, fillColor, THICK_STROKE);
    }

    /**
     * DEBUG: Renders boat collision data with diagnostic info.
     * Shows raw boat-local coordinates before transformation to help debug offset issues.
     *
     * @param graphics the Graphics2D context
     */
    public static void renderBoatCollisionDebug(Graphics2D graphics)
    {
        var boat = BoatCollisionAPI.getPlayerBoat();
        if (boat == null) {
            return;
        }

        var boatView = boat.getWorldView();
        if (boatView == null) {
            return;
        }

        Client client = Static.getClient();

        // Get boat center
        WorldPoint boatCenter = BoatCollisionAPI.getPlayerBoatWorldPoint();
        if (boatCenter == null) {
            return;
        }

        // Render boat center in MAGENTA
        renderBoatCenter(graphics, Color.MAGENTA);

        // Get collision data
        var collisionMaps = boatView.getCollisionMaps();
        if (collisionMaps == null) {
            return;
        }

        int plane = boatView.getPlane();
        int sizeX = boatView.getSizeX();
        int sizeY = boatView.getSizeY();
        int baseX = boatView.getBaseX();
        int baseY = boatView.getBaseY();

        // Get collision map dimensions
        int[][] flags = collisionMaps[plane].getFlags();
        int collisionSizeX = flags.length;
        int collisionSizeY = flags[0].length;

        // Render grid of collision tiles with their flag values
        for (int x = 0; x < collisionSizeX; x++) {
            for (int y = 0; y < collisionSizeY; y++) {
                int flag = flags[x][y];

                // Check different collision types
                boolean hasObject = (flag & net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0;
                boolean hasFull = (flag & net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0;
                boolean hasFloor = (flag & net.runelite.api.CollisionDataFlag.BLOCK_MOVEMENT_FLOOR) != 0;

                boolean hasAnyCollision = hasObject || hasFull || hasFloor;

                if (hasAnyCollision) {
                    // Transform this tile to world space
                    LocalPoint boatLocal = LocalPoint.fromScene(x, y, boatView);
                    LocalPoint mainWorldLocal = boat.transformToMainWorld(boatLocal);

                    if (mainWorldLocal != null) {
                        WorldPoint worldPoint = WorldPoint.fromLocal(client, mainWorldLocal);

                        // Render with different colors based on collision type
                        LocalPoint renderLocal = LocalPoint.fromWorld(client, worldPoint);
                        if (renderLocal != null) {
                            Polygon polygon = Perspective.getCanvasTilePoly(client, renderLocal);
                            if (polygon != null) {
                                // Color code: RED=object, YELLOW=full, BLUE=floor
                                Color outlineColor = Color.RED;
                                Color fillColor = new Color(255, 0, 0, 100);

                                if (hasFull) {
                                    outlineColor = Color.YELLOW;
                                    fillColor = new Color(255, 255, 0, 100);
                                } else if (hasFloor) {
                                    outlineColor = Color.BLUE;
                                    fillColor = new Color(0, 0, 255, 100);
                                }

                                OverlayUtil.renderPolygon(graphics, polygon, outlineColor, fillColor, DEFAULT_STROKE);

                                // Draw text showing boat-local coords and flag value
                                String label = String.format("(%d,%d)\n0x%X", x, y, flag);
                                Point textPt = Perspective.getCanvasTextLocation(client, graphics, renderLocal, label, 0);
                                if (textPt != null) {
                                    graphics.setColor(Color.WHITE);
                                    graphics.drawString(String.format("(%d,%d)", x, y), textPt.getX(), textPt.getY());
                                    graphics.drawString(String.format("0x%X", flag), textPt.getX(), textPt.getY() + 12);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Renders a specific WorldPoint as a single tile outline.
     * Useful for highlighting specific positions relative to the boat.
     *
     * @param graphics the Graphics2D context
     * @param point the world point to render
     * @param color the outline color
     */
    public static void renderWorldPointTile(Graphics2D graphics, WorldPoint point, Color color)
    {
        if (point == null) {
            return;
        }

        Client client = Static.getClient();
        LocalPoint localPoint = LocalPoint.fromWorld(client, point);
        if (localPoint == null) {
            return;
        }

        Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
        if (polygon == null) {
            return;
        }

        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
        OverlayUtil.renderPolygon(graphics, polygon, color, fillColor, DEFAULT_STROKE);
    }

    /**
     * Internal helper to render a collection of WorldPoints as tile outlines.
     * Each tile is rendered individually with the specified colors and stroke.
     *
     * @param graphics the Graphics2D context
     * @param points collection of world points to render
     * @param outlineColor the color for tile outlines
     * @param fillColor the fill color for tiles
     * @param stroke the stroke style
     */
    private static void renderWorldPointCollection(Graphics2D graphics, Collection<WorldPoint> points, Color outlineColor, Color fillColor, Stroke stroke)
    {
        if (points == null || points.isEmpty()) {
            return;
        }

        Client client = Static.getClient();

        for (WorldPoint point : points) {
            // Convert world point to local point
            LocalPoint localPoint = LocalPoint.fromWorld(client, point);
            if (localPoint == null) {
                continue;
            }

            // Get canvas polygon for this tile
            Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
            if (polygon == null) {
                continue;
            }

            // Render the tile
            OverlayUtil.renderPolygon(graphics, polygon, outlineColor, fillColor, stroke);
        }
    }
}
