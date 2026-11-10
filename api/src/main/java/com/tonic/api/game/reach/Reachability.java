package com.tonic.api.game.reach;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * The tiles the local player can walk to now, flooded over its own world view's live collision once a tick
 */
public final class Reachability
{
    private static volatile Snapshot snapshot;

    private Reachability()
    {
    }

    /**
     * Whether the local player can walk to a tile of its own world view
     * @param point the tile, in the coordinates of the player's world view
     * @return whether it can be walked to now
     */
    public static boolean isReachable(WorldPoint point)
    {
        if (point == null)
        {
            return false;
        }

        Snapshot current = current();
        if (current == null || point.getPlane() != current.map.getPlane())
        {
            return false;
        }

        int x = current.map.sceneX(point);
        int y = current.map.sceneY(point);
        return current.map.inside(x, y) && current.walkable[x][y];
    }

    /**
     * @return the id of the world view the reachable tiles lie in, or Integer.MIN_VALUE when there is none
     */
    public static int viewId()
    {
        Snapshot current = current();
        return current == null ? Integer.MIN_VALUE : current.viewId;
    }

    private static Snapshot current()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            Player player = client.getLocalPlayer();
            WorldView view = player == null ? null : player.getWorldView();
            LocalPoint at = player == null ? null : player.getLocalLocation();
            if (view == null || at == null)
            {
                return null;
            }

            int tick = client.getTickCount();
            Snapshot last = snapshot;
            if (last != null && last.tick == tick && last.viewId == view.getId() && last.map.getPlane() == view.getPlane())
            {
                return last;
            }

            SceneCollision map = SceneCollision.of(view, view.getPlane());
            if (map == null)
            {
                return null;
            }

            Snapshot next = new Snapshot(tick, view.getId(), map, SceneFlood.flood(map, at.getSceneX(), at.getSceneY()));
            snapshot = next;
            return next;
        });
    }

    private static final class Snapshot
    {
        private final int tick;
        private final int viewId;
        private final SceneCollision map;
        private final boolean[][] walkable;

        private Snapshot(int tick, int viewId, SceneCollision map, boolean[][] walkable)
        {
            this.tick = tick;
            this.viewId = viewId;
            this.map = map;
            this.walkable = walkable;
        }
    }
}
