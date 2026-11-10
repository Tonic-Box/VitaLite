package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.game.sailing.SailingAPI;
import com.tonic.api.game.sailing.SailingRenderUtil;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.services.pathfinder.local.LocalCollisionMap;
import com.tonic.util.TileDrawingUtil;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TileOverlays extends Overlay
{
    private static final int SEA_RADIUS = 16;
    private static final int TRACK_TICKS = 8;
    private static final int[][] SIDES = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
    private int interactableTick = -1;
    private List<Edge> interactableEdges = new ArrayList<>();
    private final GameManager manager;
    public TileOverlays(GameManager manager)
    {
        this.manager = manager;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if(SailingAPI.isOnBoat())
        {
            if(Static.getVitaConfig().getDrawBoatHull())
            {
                SailingRenderUtil.renderPlayerBoatHull(graphics);
            }

            if(Static.getVitaConfig().getDrawBoatDeck())
            {
                SailingRenderUtil.renderPlayerBoatDeck(graphics);
            }

            if(Static.getVitaConfig().getDrawSeaCells())
            {
                SailingRenderUtil.renderSeaCells(graphics, SEA_RADIUS);
            }

            if(Static.getVitaConfig().getDrawSeaGraph())
            {
                SailingRenderUtil.renderSeaGraph(graphics);
            }

            if(Static.getVitaConfig().getDrawBoatRoute())
            {
                SailingRenderUtil.renderRoute(graphics);
            }

            if(Static.getVitaConfig().getDrawBoatTrack())
            {
                SailingRenderUtil.renderPredictedTrack(graphics, TRACK_TICKS);
            }
        }
        if(Static.getVitaConfig().shouldDrawInteractable())
        {
            drawInteractableFrom(graphics);
        }

        if(Static.getVitaConfig().shouldDrawCollision())
        {
            drawCollisionMap(graphics);
        }

        var testPoints = manager.getTestPoints();
        boolean onBoat = SailingAPI.isOnBoat();
        if(testPoints != null && !testPoints.isEmpty())
        {
            drawPath(graphics, testPoints, Color.MAGENTA, onBoat);
        }

        if(onBoat || !Static.getVitaConfig().shouldDrawWalkerPath())
            return null;

        var pathPoints = manager.getPathPoints();

        if(pathPoints != null && !pathPoints.isEmpty())
        {
            drawPath(graphics, pathPoints, Color.CYAN, onBoat);
        }
        return null;
    }

    private void drawPath(Graphics2D graphics, List<WorldPoint> points, Color color, boolean asLine)
    {
        if(asLine)
        {
            drawWorldLine(graphics, points, color);
        }
        else
        {
            drawWorldTiles(graphics, points, color);
        }
    }

    private void drawWorldLine(Graphics2D graphics, List<WorldPoint> points, Color color)
    {
        final Client client = Static.getClient();
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(2.0f));
        net.runelite.api.Point last = null;
        for(WorldPoint point : points)
        {
            LocalPoint localPoint = LocalPoint.fromWorld(client, point);
            net.runelite.api.Point canvas = localPoint == null ? null : Perspective.localToCanvas(client, localPoint, point.getPlane());
            if(canvas != null && last != null)
            {
                graphics.drawLine(last.getX(), last.getY(), canvas.getX(), canvas.getY());
            }
            last = canvas;
        }
    }

    private void drawWorldTiles(Graphics2D graphics, List<WorldPoint> points, Color color)
    {
        if(points == null || points.isEmpty())
            return;

        final Client client = Static.getClient();
        final WorldPoint playerLocation = WorldPointUtil.getTopWorldViewLocation();
        final int MAX_DRAW_DISTANCE = 32;

        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
        Stroke stroke = new BasicStroke(2.0f);

        for(WorldPoint point : points)
        {
            if(point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE)
                continue;

            LocalPoint localPoint = LocalPoint.fromWorld(client, point);
            if(localPoint == null)
                continue;

            Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
            if(polygon == null)
                continue;

            OverlayUtil.renderPolygon(graphics, polygon, color, fillColor, stroke);
        }
    }

    /**
     * Draws, for every object with actions, the edge of each tile it can be interacted with from on the side facing it,
     * and the outline of tiles inside its footprint it can be used from
     * @param graphics the overlay graphics
     */
    public void drawInteractableFrom(Graphics2D graphics)
    {
        Client client = Static.getClient();
        if(interactableTick != client.getTickCount())
        {
            interactableEdges = interactableEdges();
            interactableTick = client.getTickCount();
        }

        graphics.setColor(Color.RED);
        graphics.setStroke(TileDrawingUtil.stroke(2));
        for(Edge edge : interactableEdges)
        {
            net.runelite.api.Point from = Perspective.localToCanvas(client, edge.from, edge.plane);
            net.runelite.api.Point to = Perspective.localToCanvas(client, edge.to, edge.plane);
            if(from != null && to != null)
            {
                graphics.drawLine(from.getX(), from.getY(), to.getX(), to.getY());
            }
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
        int sceneX = tile.getX() - view.getBaseX();
        int sceneY = tile.getY() - view.getBaseY();
        for(int[] side : SIDES)
        {
            if(inside || footprint.contains2D(tile.dx(side[0]).dy(side[1])))
            {
                edges.add(Edge.of(view, sceneX, sceneY, side, tile.getPlane()));
            }
        }
    }

    private static final class Edge
    {
        private final LocalPoint from;
        private final LocalPoint to;
        private final int plane;

        private Edge(LocalPoint from, LocalPoint to, int plane)
        {
            this.from = from;
            this.to = to;
            this.plane = plane;
        }

        private static Edge of(WorldView view, int sceneX, int sceneY, int[] side, int plane)
        {
            int half = Perspective.LOCAL_HALF_TILE_SIZE;
            int centreX = (sceneX << Perspective.LOCAL_COORD_BITS) + half;
            int centreY = (sceneY << Perspective.LOCAL_COORD_BITS) + half;
            int fromX = side[0] == 0 ? centreX - half : centreX + side[0] * half;
            int fromY = side[1] == 0 ? centreY - half : centreY + side[1] * half;
            int toX = side[0] == 0 ? centreX + half : fromX;
            int toY = side[1] == 0 ? centreY + half : fromY;
            return new Edge(new LocalPoint(fromX, fromY, view), new LocalPoint(toX, toY, view), plane);
        }
    }

    public void drawCollisionMap(Graphics2D graphics) {
        Client client = Static.getClient();
        WorldView wv = PlayerEx.getLocal().getWorldView();
        if(wv.getCollisionMaps() == null || wv.getCollisionMaps()[wv.getPlane()] == null)
            return;

        int[][] c_flags = wv.getCollisionMaps()[wv.getPlane()].getFlags();
        WorldPoint point;
        LocalPoint localPoint;
        Tile tile;
        LocalCollisionMap map = new LocalCollisionMap();
        Color wall = Color.RED;
        Color fill = new Color(255, 0, 0, 80);
        Stroke stroke = new BasicStroke(1.0f);
        for(int x = 0; x < c_flags.length; x++)
        {
            for(int y = 0; y < c_flags[x].length; y++)
            {
                point = WorldPoint.fromScene(wv, x, y, wv.getPlane());
                localPoint = LocalPoint.fromScene(x, y, wv);
                byte flags = map.all((short)point.getX(), (short)point.getY(), (byte)point.getPlane());
                tile = new Tile(flags);
                tile.render(graphics, localPoint, Perspective.getCanvasTilePoly(client, localPoint), wall, fill, stroke);
            }
        }
    }

    @RequiredArgsConstructor
    private static class Tile
    {
        private final byte flag;
        /**
         * no blocking
         * @return true if there are no walls
         */
        public boolean none()
        {
            return flag == Flags.ALL;
        }

        /**
         * full blocking
         * @return true if all walls are blocking
         */
        public boolean full()
        {
            return flag == Flags.NONE;
        }

        /**
         * west wall blocking
         */
        public boolean westWall()
        {
            return (flag & Flags.WEST) == 0;
        }

        /**
         * east wall blocking
         */
        public boolean eastWall()
        {
            return (flag & Flags.EAST) == 0;
        }

        /**
         * north wall blocking
         * @return true if there is a wall to the north
         */
        public boolean southWall()
        {
            return (flag & Flags.SOUTH) == 0;
        }

        /**
         * south wall blocking
         * @return true if there is a wall to the south
         */
        public boolean northWall()
        {
            return (flag & Flags.NORTH) == 0;
        }

        /**
         * Get the walls for this cell
         * @return a list of walls
         */
        public java.util.List<Wall> getWalls()
        {
            final List<Wall> walls = new ArrayList<>();
            if(eastWall())
            {
                walls.add(new Wall(Wall.Direction.EAST));
            }
            if(southWall())
            {
                walls.add(new Wall(Wall.Direction.SOUTH));
            }
            if(westWall())
            {
                walls.add(new Wall(Wall.Direction.WEST));
            }
            if(northWall())
            {
                walls.add(new Wall(Wall.Direction.NORTH));
            }
            return walls;
        }

        public void render(Graphics2D g2d, LocalPoint localPoint, Shape poly, Color color, Color fillColor, Stroke borderStroke)
        {
            if(full())
            {
                TileDrawingUtil.renderPolygon(g2d, poly, color, fillColor, borderStroke);
            }
            for(Wall wall : getWalls()) {
                TileDrawingUtil.renderWall(g2d, localPoint, color, wall.getDirection());
            }
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
