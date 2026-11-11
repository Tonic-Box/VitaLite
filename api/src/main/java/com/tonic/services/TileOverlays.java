package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.game.sailing.SailingAPI;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.ground.GroundCanvas;
import com.tonic.services.ground.GroundOverlay;
import com.tonic.services.ground.TileSide;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The walker's path, test points and interactable sides drawn on the ground.
 */
public class TileOverlays implements GroundOverlay
{
    private static final int MAX_DRAW_DISTANCE = 32;
    private static final int[][] SIDES = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
    private static final TileSide[] SIDE_NAMES = {TileSide.NORTH, TileSide.EAST, TileSide.SOUTH, TileSide.WEST};
    private int interactableTick = -1;
    private List<Edge> interactableEdges = new ArrayList<>();
    private final GameManager manager;

    public TileOverlays(GameManager manager)
    {
        this.manager = manager;
    }

    @Override
    public void paint(GroundCanvas canvas)
    {
        if(Static.getVitaConfig().shouldDrawInteractable())
        {
            drawInteractableFrom(canvas);
        }

        var testPoints = manager.getTestPoints();
        boolean onBoat = SailingAPI.isOnBoat();
        if(testPoints != null && !testPoints.isEmpty())
        {
            drawPath(canvas, testPoints, Color.MAGENTA, onBoat);
        }

        if(onBoat || !Static.getVitaConfig().shouldDrawWalkerPath())
            return;

        var pathPoints = manager.getPathPoints();
        if(pathPoints != null && !pathPoints.isEmpty())
        {
            drawPath(canvas, pathPoints, Color.CYAN, false);
        }
    }

    private void drawPath(GroundCanvas canvas, List<WorldPoint> points, Color color, boolean asLine)
    {
        if(asLine)
        {
            canvas.path(points, color, GroundCanvas.THIN);
        }
        else
        {
            drawWorldTiles(canvas, points, color);
        }
    }

    private void drawWorldTiles(GroundCanvas canvas, List<WorldPoint> points, Color color)
    {
        final WorldPoint playerLocation = WorldPointUtil.getTopWorldViewLocation();
        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);

        for(WorldPoint point : points)
        {
            if(point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE)
                continue;

            canvas.markTile(point.getX(), point.getY(), point.getPlane(), color, fillColor, GroundCanvas.THIN);
        }
    }

    /**
     * Draws, for every object with actions, the side of each tile it can be interacted with from that faces it,
     * and every side of the tiles inside its footprint it can be used from
     * @param canvas the ground canvas
     */
    public void drawInteractableFrom(GroundCanvas canvas)
    {
        Client client = Static.getClient();
        if(interactableTick != client.getTickCount())
        {
            interactableEdges = interactableEdges();
            interactableTick = client.getTickCount();
        }

        for(Edge edge : interactableEdges)
        {
            canvas.onView(edge.worldView).tileEdge(edge.x, edge.y, edge.plane, edge.side, Color.RED, GroundCanvas.THIN);
        }
    }

    private static List<Edge> interactableEdges()
    {
        List<Edge> edges = new ArrayList<>();
        for(TileObjectEx obj : GameManager.objectList())
        {
            if(!obj.isInteractable())
                continue;

            WorldView view = obj.getWorldView();
            if(view == null)
                continue;

            WorldArea footprint = obj.getWorldArea();
            for(WorldPoint tile : obj.interactableFrom())
            {
                addEdges(edges, view, footprint, tile);
            }
        }
        return edges;
    }

    private static void addEdges(List<Edge> edges, WorldView view, WorldArea footprint, WorldPoint tile)
    {
        boolean inside = footprint.contains2D(tile);
        for(int i = 0; i < SIDES.length; i++)
        {
            int[] side = SIDES[i];
            if(inside || footprint.contains2D(tile.dx(side[0]).dy(side[1])))
            {
                edges.add(new Edge(view.getId(), tile.getX(), tile.getY(), tile.getPlane(), SIDE_NAMES[i]));
            }
        }
    }

    private static final class Edge
    {
        private final int worldView;
        private final int x;
        private final int y;
        private final int plane;
        private final TileSide side;

        private Edge(int worldView, int x, int y, int plane, TileSide side)
        {
            this.worldView = worldView;
            this.x = x;
            this.y = y;
            this.plane = plane;
            this.side = side;
        }
    }

    @Getter
    public static class Wall {
        private final Direction direction;

        /**
         * Creates a new wall.
         *
         * @param direction  the direction of the wall
         */
        public Wall(Direction direction) {
            this.direction = direction;
        }

        /**
         * Represents the direction of the wall.
         */
        public enum Direction {
            WEST, EAST, NORTH, SOUTH
        }
    }
}
