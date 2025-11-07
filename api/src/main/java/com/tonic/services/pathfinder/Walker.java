package com.tonic.services.pathfinder;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.services.ClickVisualizationOverlay;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.abstractions.IPathfinder;
import com.tonic.services.pathfinder.abstractions.IStep;
import com.tonic.services.pathfinder.collision.CollisionMap;
import com.tonic.services.pathfinder.collision.GlobalCollisionMap;
import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.services.pathfinder.objects.ObjectMap;
import com.tonic.util.IntPair;
import com.tonic.util.Location;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import java.util.List;

/**
 * A worldWalker
 */
public class Walker
{
    static {
        try {
            collisionMap = GlobalCollisionMap.load();
            objectMap = ObjectMap.load();
        } catch (Exception e) {
            Logger.error("[Pathfinder] Failed to load collision map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Getter
    private static CollisionMap collisionMap;
    @Getter
    private static ObjectMap objectMap;
    private static boolean running = false;

    private Walker()
    {

    }

    public static class Setting
    {
        public static IntPair toggleRunRange = new IntPair(25, 35);
        public static IntPair consumeStaminaRange = new IntPair(50, 60);

        public static int toggleRunThreshold = toggleRunRange.randomEnclosed();
        public static int consumeStaminaThreshold = consumeStaminaRange.randomEnclosed();
    }

    /**
     * Walk to a target point
     * @param target target point
     */
    public static void walkTo(WorldPoint target)
    {
        walk(target);
    }

    /**
     * Walk to one of the specified areas (closest)
     * @param areas target areas
     */
    public static void walkTo(List<WorldArea> areas)
    {
        walk(areas);
    }

    private static void walk(WorldPoint target) {
        WalkerPath walkerPath = WalkerPath.get(target);
        walk(walkerPath);
    }

    private static void walk(List<WorldArea> targets) {
        WalkerPath walkerPath = WalkerPath.get(targets);
        walk(walkerPath);
    }

    private static void walk(WalkerPath walkerPath)
    {
        Client client = Static.getClient();
        try
        {
            WorldPoint end = walkerPath.getSteps().get(walkerPath.getSteps().size() - 1).getPosition();
            running = true;
            while(walkerPath.step())
            {
                if(!running)
                {
                    GameManager.clearPathPoints();
                    return;
                }
                Delays.tick();
            }
            int timeout = 50;
            WorldPoint worldPoint = Static.invoke(() -> client.getLocalPlayer().getWorldLocation());
            while(!worldPoint.equals(end) && timeout > 0)
            {
                Delays.tick();
                if(PlayerAPI.isIdle(client.getLocalPlayer()))
                {
                    timeout--;
                    if(!Location.isReachable(client.getLocalPlayer().getWorldLocation(), end))
                    {
                        walkTo(end);
                        GameManager.clearPathPoints();
                        return;
                    }
                    ClickVisualizationOverlay.recordWalkClick(end);
                    MovementAPI.walkToWorldPoint(end);
                    Delays.tick();
                }
                worldPoint = Static.invoke(() -> client.getLocalPlayer().getWorldLocation());
                if(!running)
                {
                    walkerPath.shutdown();
                    GameManager.clearPathPoints();
                    return;
                }
            }
        }
        finally {
            walkerPath.shutdown();
            GameManager.clearPathPoints();
            running = false;
        }
    }

    public static void cancel()
    {
        running = false;
        GameManager.clearPathPoints();
    }

    public static boolean isWalking()
    {
        return running;
    }
}
