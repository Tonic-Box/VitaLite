package com.tonic.services.pathfinder;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.services.pathfinder.sailing.BoatPathing;
import com.tonic.api.game.sailing.SailingAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.collision.CollisionMap;
import com.tonic.services.pathfinder.collision.GlobalCollisionMap;
import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.services.pathfinder.objects.ObjectMap;
import com.tonic.services.pathfinder.sailing.graph.NavGraph;
import com.tonic.services.pathfinder.tiletype.TileTypeMap;
import com.tonic.util.IntPair;
import com.tonic.util.ThreadPool;
import com.tonic.util.handler.StepHandler;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A worldWalker
 */
public class Walker
{
    static {
        try {
            collisionMap = GlobalCollisionMap.load();
            objectMap = ObjectMap.load();
            tileTypeMap = TileTypeMap.load();
            navGraph = NavGraph.load();
        } catch (Exception e) {
            Logger.error("[Pathfinder] Failed to load collision map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Getter
    private static CollisionMap collisionMap;
    @Getter
    private static ObjectMap objectMap;
    @Getter
    private static TileTypeMap tileTypeMap;
    @Getter
    private static NavGraph navGraph;
    private static boolean running = false;
    private static WalkerPath currentPath = null;

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

    public static void sailTo(WorldPoint target)
    {
        StepHandler handler = BoatPathing.travelTo(target);
        running = true;
        while(handler.step())
        {
            if(!running)
            {
                GameManager.clearPathPoints();
                SailingAPI.unSetSails();
                return;
            }
            Delays.tick();
        }
    }

    /**
     * Walk to a target point
     * @param target target point
     */
    public static void walkTo(WorldPoint target)
    {
        walk(target);
    }

    public static boolean walkTo(WorldPoint target, BooleanSupplier stopCondition)
    {
        return walk(target, stopCondition);
    }

    /**
     * Walk to one of the specified areas (closest)
     * @param areas target areas
     */
    public static void walkTo(List<WorldArea> areas)
    {
        walk(areas);
    }

    public static boolean walkTo(List<WorldArea> areas, BooleanSupplier stopCondition)
    {
        return walk(areas, stopCondition);
    }

    private static void walk(WorldPoint target) {
        walk(target, () -> false);
    }

    private static void walk(List<WorldArea> targets) {
        walk(targets, () -> false);
    }

    private static boolean walk(WorldPoint target, BooleanSupplier stopCondition) {
        target = collisionMap.nearestWalkableEuclidean(target, 5);
        WalkerPath walkerPath = WalkerPath.get(target);
        return walk(walkerPath, stopCondition);
    }

    private static boolean walk(List<WorldArea> targets, BooleanSupplier stopCondition) {
        WalkerPath walkerPath = WalkerPath.get(targets);
        return walk(walkerPath, stopCondition);
    } //Walker.walkTo(new WorldPoint(2570, 3245, 0));

    private static void walk(WalkerPath walkerPath)
    {
        walk(walkerPath, () -> false);
    }

    private static boolean walk(WalkerPath walkerPath, BooleanSupplier stopCondition)
    {
        currentPath = walkerPath;
        if(running)
        {
            Logger.warn("[Pathfinder] Already walking!");
            return false;
        }
        try
        {
            WorldPoint end = currentPath.getSteps().get(currentPath.getSteps().size() - 1).getPosition();
            running = true;
            if(stopCondition.getAsBoolean())
            {
                cancel();
                return true;
            }
            while(currentPath.step())
            {
                if(stopCondition.getAsBoolean())
                {
                    cancel();
                    return true;
                }
                if(!running)
                {
                    GameManager.clearPathPoints();
                    cancel();
                    return false;
                }
                Delays.tick();
            }
            int timeout = 50;
            WorldPoint worldPoint = Static.invoke(() -> PlayerEx.getLocal().getWorldPoint());
            while(!worldPoint.equals(end) && timeout > 0)
            {
                if(stopCondition.getAsBoolean())
                {
                    cancel();
                    return true;
                }
                Delays.tick();
                if(PlayerEx.getLocal().isIdle())
                {
                    timeout--;
                    if(!SceneAPI.isReachable(PlayerEx.getLocal().getWorldPoint(), end))
                    {
                        walkTo(end);
                        GameManager.clearPathPoints();
                        return false;
                    }
                    MovementAPI.walkToWorldPoint(end);
                    Delays.tick();
                }
                worldPoint = Static.invoke(() -> PlayerEx.getLocal().getWorldPoint());
                if(!running)
                {
                    currentPath.shutdown();
                    GameManager.clearPathPoints();
                    return false;
                }
            }
        }
        finally {
            currentPath.shutdown();
            currentPath = null;
            GameManager.clearPathPoints();
            running = false;
        }
        return false;
    }

    public static void cancel()
    {
        running = false;
        if(currentPath != null)
        {
            currentPath.cancel();
            currentPath = null;
        }
        GameManager.clearPathPoints();
    }

    public static boolean isWalking()
    {
        return running;
    }
}
