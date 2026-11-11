package com.tonic.services;

import com.tonic.Static;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.ground.GroundCanvas;
import com.tonic.services.ground.GroundOverlay;
import com.tonic.services.ground.TileSide;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.services.pathfinder.local.LocalCollisionMap;
import net.runelite.api.CollisionData;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

import java.awt.Color;
import java.util.Arrays;

/**
 * The walker's collision around the player drawn on the ground: blocked tiles filled, walls along the sides they block, repainted only when the scene or its collision changes.
 */
final class CollisionOverlay implements GroundOverlay
{
    private static final Color WALL = Color.RED;
    private static final Color FILL = new Color(255, 0, 0, 80);

    @Override
    public Object dependsOn()
    {
        WorldView view = shownView();
        if (view == null)
        {
            return false;
        }
        CollisionData collision = collisionOf(view);
        if (collision == null)
        {
            return false;
        }
        return Arrays.asList(view.getId(), view.getBaseX(), view.getBaseY(), view.getPlane(), Arrays.deepHashCode(collision.getFlags()));
    }

    @Override
    public void paint(GroundCanvas canvas)
    {
        WorldView view = shownView();
        if (view == null)
        {
            return;
        }
        CollisionData collision = collisionOf(view);
        if (collision == null)
        {
            return;
        }

        GroundCanvas onView = canvas.onView(view.getId());
        int plane = view.getPlane();
        int[][] flags = collision.getFlags();
        LocalCollisionMap map = new LocalCollisionMap();
        for (int x = 0; x < flags.length; x++)
        {
            for (int y = 0; y < flags[x].length; y++)
            {
                WorldPoint point = WorldPoint.fromScene(view, x, y, plane);
                byte flag = map.all((short) point.getX(), (short) point.getY(), (byte) point.getPlane());
                paintTile(onView, point, flag);
            }
        }
    }

    private static WorldView shownView()
    {
        if (!Static.getVitaConfig().shouldDrawCollision() || PlayerEx.getLocal() == null)
        {
            return null;
        }
        return PlayerEx.getLocal().getWorldView();
    }

    private static CollisionData collisionOf(WorldView view)
    {
        CollisionData[] maps = view.getCollisionMaps();
        if (maps == null)
        {
            return null;
        }
        return maps[view.getPlane()];
    }

    private static void paintTile(GroundCanvas canvas, WorldPoint point, byte flag)
    {
        if (flag == Flags.NONE)
        {
            canvas.markTile(point.getX(), point.getY(), point.getPlane(), WALL, FILL, GroundCanvas.THIN);
        }
        if ((flag & Flags.EAST) == 0)
        {
            canvas.tileEdge(point.getX(), point.getY(), point.getPlane(), TileSide.EAST, WALL, GroundCanvas.THIN);
        }
        if ((flag & Flags.SOUTH) == 0)
        {
            canvas.tileEdge(point.getX(), point.getY(), point.getPlane(), TileSide.SOUTH, WALL, GroundCanvas.THIN);
        }
        if ((flag & Flags.WEST) == 0)
        {
            canvas.tileEdge(point.getX(), point.getY(), point.getPlane(), TileSide.WEST, WALL, GroundCanvas.THIN);
        }
        if ((flag & Flags.NORTH) == 0)
        {
            canvas.tileEdge(point.getX(), point.getY(), point.getPlane(), TileSide.NORTH, WALL, GroundCanvas.THIN);
        }
    }
}
