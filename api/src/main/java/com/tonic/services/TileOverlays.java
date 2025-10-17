package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.threaded.Delays;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.services.pathfinder.local.LocalCollisionMap;
import com.tonic.util.ThreadPool;
import com.tonic.util.TileDrawingUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TileOverlays extends Overlay
{
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
        if(Static.getVitaConfig().shouldDrawCollision())
        {
            drawCollisionMap(graphics);
        }

        var testPoints = manager.getTestPoints();
        if(testPoints != null && !testPoints.isEmpty())
        {
            drawWorldTiles(graphics, testPoints, Color.MAGENTA);
        }

        if(!Static.getVitaConfig().shouldDrawWalkerPath())
            return null;

        var pathPoints = manager.getPathPoints();

        if(pathPoints != null && !pathPoints.isEmpty())
        {
            drawWorldTiles(graphics, pathPoints, Color.CYAN);
        }
        return null;
    }

    private void drawWorldTiles(Graphics2D graphics, List<WorldPoint> points, Color color)
    {
        if(points == null || points.isEmpty())
            return;

        final Client client = Static.getClient();
        final WorldView worldView = client.getTopLevelWorldView();
        final WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        final int MAX_DRAW_DISTANCE = 32;

        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
        Stroke stroke = new BasicStroke(2.0f);

        for(WorldPoint point : points)
        {
            if(point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE)
                continue;

            LocalPoint localPoint = LocalPoint.fromWorld(worldView, point);
            if(localPoint == null)
                continue;

            Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
            if(polygon == null)
                continue;

            OverlayUtil.renderPolygon(graphics, polygon, color, fillColor, stroke);
        }
    }

    public void drawCollisionMap(Graphics2D graphics) {
        Client client = Static.getClient();
        WorldView wv = client.getTopLevelWorldView();
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
                tile = new Tile(flags, point);
                tile.render(graphics, localPoint, Perspective.getCanvasTilePoly(client, localPoint), wall, fill, stroke);
            }
        }
    }

    @RequiredArgsConstructor
    private static class Tile
    {
        private final byte flag;
        private final WorldPoint point;
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
