package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.TObjectComposition;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.data.ObjectBlockAccessFlags;
import com.tonic.data.Walls;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.services.pathfinder.local.CollisionUtil;
import com.tonic.services.pathfinder.local.LocalCollisionMap;
import com.tonic.util.RelativePosition;
import com.tonic.util.ThreadPool;
import com.tonic.util.TileDrawingUtil;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        if(Static.getVitaConfig().shouldDrawInteractable())
        {
            drawInteractableFrom(graphics);
        }

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
        final WorldView worldView = PlayerEx.getLocal().getWorldView();
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

    public void drawInteractableFrom(Graphics2D graphics2D)
    {
        Client client = Static.getClient();

        for(TileObjectEx obj : GameManager.objectList())
        {
            if(obj.getType() != 2 || !obj.isInteractable())
                continue;

            ObjectComposition composition = client.getObjectDefinition(obj.getId());
            TObjectComposition tComp = (TObjectComposition) composition;

            int modelRotation = obj.getOrientation();
            int type = obj.getConfig() & 0x1F;

            int rotation = modelRotation;
            if (type == 2 || type == 6 || type == 8) {
                rotation -= 4;
            } else if (type == 7) {
                rotation = (rotation - 2) & 0x3;
            }
            rotation = rotation & 0x3;

            WorldArea area = obj.getWorldArea();
            int width = area.getWidth();
            int height = area.getHeight();

            WorldPoint objPos = obj.getWorldPoint();
            int rotatedFlags = tComp.rotateBlockAccessFlags(rotation);

            int newX;
            int newY;
            int plane;

            if(obj.getTileObject() instanceof WallObject)
            {
                WallObject wall = (WallObject) obj.getTileObject();
                Walls walls = Walls.of(wall);
                if(walls.hasNorthWall())
                {
                    TileDrawingUtil.renderWall(graphics2D, LocalPoint.fromWorld(client, wall.getWorldLocation()), Color.RED, Wall.Direction.NORTH);
                }
                if(walls.hasEastWall())
                {
                    TileDrawingUtil.renderWall(graphics2D, LocalPoint.fromWorld(client, wall.getWorldLocation()), Color.RED, Wall.Direction.EAST);
                }
                if(walls.hasSouthWall())
                {
                    TileDrawingUtil.renderWall(graphics2D, LocalPoint.fromWorld(client, wall.getWorldLocation()), Color.RED, Wall.Direction.SOUTH);
                }
                if(walls.hasWestWall())
                {
                    TileDrawingUtil.renderWall(graphics2D, LocalPoint.fromWorld(client, wall.getWorldLocation()), Color.RED, Wall.Direction.WEST);
                }
                continue;
            }

            if ((rotatedFlags & ObjectBlockAccessFlags.BLOCK_NORTH) == 0) {
                for(int x = 0; x < width; x++) {
                    newX = objPos.getX() + x;
                    newY = objPos.getY() + height - 1;
                    plane = objPos.getPlane();
                    if(!LocalCollisionMap.canStep(newX, newY + 1, plane))
                        continue;
                    if(Walls.of(newX, newY, plane).hasNorthWall())
                        continue;
                    WorldPoint wallPoint = new WorldPoint(newX, newY, plane);
                    LocalPoint localPoint = LocalPoint.fromWorld(client, wallPoint);
                    if(localPoint == null)
                        continue;
                    Shape poly = Perspective.getCanvasTilePoly(client, localPoint);
                    if(poly != null) {
                        TileDrawingUtil.renderWall(graphics2D, localPoint, Color.RED, Wall.Direction.NORTH);
                    }
                }
            }

            if ((rotatedFlags & ObjectBlockAccessFlags.BLOCK_EAST) == 0) {
                for(int y = 0; y < height; y++) {
                    newX = objPos.getX() + width - 1;
                    newY = objPos.getY() + y;
                    plane = objPos.getPlane();
                    if(!LocalCollisionMap.canStep(newX + 1, newY, plane))
                        continue;
                    if(Walls.of(newX, newY, plane).hasEastWall())
                        continue;
                    WorldPoint wallPoint = new WorldPoint(newX, newY, plane);
                    LocalPoint localPoint = LocalPoint.fromWorld(client, wallPoint);
                    if(localPoint == null)
                        continue;
                    Shape poly = Perspective.getCanvasTilePoly(client, localPoint);
                    if(poly != null) {
                        TileDrawingUtil.renderWall(graphics2D, localPoint, Color.RED, Wall.Direction.EAST);
                    }
                }
            }

            if ((rotatedFlags & ObjectBlockAccessFlags.BLOCK_SOUTH) == 0) {
                for(int x = 0; x < width; x++) {
                    newX = objPos.getX() + x;
                    newY = objPos.getY();
                    plane = objPos.getPlane();
                    if(!LocalCollisionMap.canStep(newX, newY - 1, plane))
                        continue;
                    if(Walls.of(newX, newY, plane).hasSouthWall())
                        continue;
                    WorldPoint wallPoint = new WorldPoint(newX, newY, plane);
                    LocalPoint localPoint = LocalPoint.fromWorld(client, wallPoint);
                    if(localPoint == null)
                        continue;
                    Shape poly = Perspective.getCanvasTilePoly(client, localPoint);
                    if(poly != null) {
                        TileDrawingUtil.renderWall(graphics2D, localPoint, Color.RED, Wall.Direction.SOUTH);
                    }
                }
            }

            if ((rotatedFlags & ObjectBlockAccessFlags.BLOCK_WEST) == 0) {
                for(int y = 0; y < height; y++) {
                    newX = objPos.getX();
                    newY = objPos.getY() + y;
                    plane = objPos.getPlane();
                    if(!LocalCollisionMap.canStep(newX - 1, newY, plane))
                        continue;
                    if(Walls.of(newX, newY, plane).hasWestWall())
                        continue;
                    WorldPoint wallPoint = new WorldPoint(newX, newY, plane);
                    LocalPoint localPoint = LocalPoint.fromWorld(client, wallPoint);
                    if(localPoint == null)
                        continue;
                    Shape poly = Perspective.getCanvasTilePoly(client, localPoint);
                    if(poly != null) {
                        TileDrawingUtil.renderWall(graphics2D, localPoint, Color.RED, Wall.Direction.WEST);
                    }
                }
            }
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
