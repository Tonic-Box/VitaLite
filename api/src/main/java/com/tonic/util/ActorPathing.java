package com.tonic.util;

import com.tonic.Static;
import com.tonic.services.GameManager;
import com.tonic.services.pathfinder.Pathfinder;
import com.tonic.services.pathfinder.collision.CollisionMap;
import com.tonic.services.pathfinder.collision.Flags;
import com.tonic.services.pathfinder.local.LocalCollisionMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ActorPathing
{
    public static boolean hasLineOfWalk(Actor actor, WorldPoint destination, List<WorldPoint> blacklist) {
        return hasLineOfWalk(actor, destination, blacklist, null);
    }

    public static boolean hasLineOfWalk(Actor actor, WorldPoint destination, List<WorldPoint> blacklist, @Nullable LocalCollisionMap localMap) {
        return hasLineOfWalk(actor.getWorldLocation(), destination, actor.getWorldArea().getWidth(), actor.getWorldArea().getHeight(), blacklist, localMap);
    }

    public static boolean hasLineOfWalk(WorldPoint start, WorldPoint end, int actorWidth, int actorHeight, List<WorldPoint> blacklist) {
        return hasLineOfWalk(start, end, actorWidth, actorHeight, blacklist, null);
    }

    public static boolean hasLineOfWalk(WorldPoint start, WorldPoint end, int actorWidth, int actorHeight, List<WorldPoint> blacklist, @Nullable LocalCollisionMap localMap) {
        WorldArea area = new WorldArea(start, actorWidth, actorHeight);
        if(area.distanceTo(end) < 1)
            return true;
        List<WorldPoint> path = dumbPathing(start, end, actorWidth, actorHeight, blacklist, localMap);
        return !path.isEmpty() && path.get(path.size() - 1).equals(end);
    }

    /**
     * Calculates the pathing for targeting
     * @param actor actor
     * @param destination the destination tile of the other actor to be targeted
     * @param blacklist tiles to avoid
     * @return Pare.of(true destination next to target, path)
     */
    @Nullable
    public static Pair<WorldPoint,List<WorldPoint>> dumbTargeting(final Actor actor, final WorldPoint destination, final List<WorldPoint> blacklist)
    {
        return dumbTargeting(actor, destination, blacklist, null);
    }

    /**
     * Calculates the pathing for targeting
     * @param actor actor
     * @param destination the destination tile of the other actor to be targeted
     * @param blacklist tiles to avoid
     * @param localMap collision map for inside instances
     * @return Pare.of(true destination next to target, path)
     */
    @Nullable
    public static Pair<WorldPoint,List<WorldPoint>> dumbTargeting(final Actor actor, final WorldPoint destination, final List<WorldPoint> blacklist, @Nullable LocalCollisionMap localMap)
    {
        blacklist.addAll(actorTiles(actor));
        return dumbTargetingNoNpcBlocking(actor, destination, blacklist, localMap);
    }

    public static Pair<WorldPoint,List<WorldPoint>> dumbTargetingNoNpcBlocking(final Actor actor, final WorldPoint destination, final List<WorldPoint> blacklist)
    {
        return dumbTargetingNoNpcBlocking(actor, destination, blacklist, null);
    }

    public static Pair<WorldPoint,List<WorldPoint>> dumbTargetingNoNpcBlocking(final Actor actor, final WorldPoint destination, final List<WorldPoint> blacklist, @Nullable LocalCollisionMap localMap)
    {
        final WorldPoint start = actor.getWorldLocation();
        int width = actor.getWorldArea().getWidth();
        int height = actor.getWorldArea().getHeight();
        final List<WorldPoint> path = dumbPathing(start, destination, width, height, blacklist, localMap);
        if(path.isEmpty())
            return null;

        if(!destination.equals(path.get(path.size() - 1)))
            return Pair.of(destination, path);

        if(path.size() < 2)
            return null;

        final WorldPoint trueEnd = path.get(path.size() - 2);
        return Pair.of(trueEnd,dumbPathing(start, trueEnd, width, height, blacklist, localMap));
    }

    /**
     * Calcumates the dumb path of an actor to the destination tile while considering other actors blocking
     *
     * @param actor actor
     * @param destination destibnation tile
     * @param blacklist list of hardcoded worldpoints to avoid
     * @return path
     */
    public static List<WorldPoint> dumbPathing(final Actor actor, final WorldPoint destination, final List<WorldPoint> blacklist) {
        return dumbPathing(actor, destination, blacklist, null);
    }

    /**
     * Calcumates the dumb path of an actor to the destination tile while considering other actors blocking
     *
     * @param actor actor
     * @param destination destibnation tile
     * @param blacklist list of hardcoded worldpoints to avoid
     * @param localMap collision map for inside instances
     * @return path
     */
    public static List<WorldPoint> dumbPathing(final Actor actor, final WorldPoint destination, final List<WorldPoint> blacklist, @Nullable LocalCollisionMap localMap) {
        blacklist.addAll(actorTiles(actor));
        final WorldArea area = actor.getWorldArea();
        return dumbPathing(actor.getWorldLocation(), destination, area.getWidth(), area.getHeight(), blacklist, localMap);
    }

    public static List<WorldPoint> dumbPathing(final WorldPoint start, final WorldPoint destination, final int actorWidth, final int actorHeight, final List<WorldPoint> blacklist)
    {
        return dumbPathing(start, destination, actorWidth, actorHeight, blacklist, null);
    }

    /**
     * Calcumates the dumb path of an actor from start to the destination
     * @param start start tile
     * @param destination destination tile
     * @param actorWidth width of the actor
     * @param actorHeight height of the actor
     * @param blacklist list of hardcoded worldpoints to avoid
     * @return path
     */
    public static List<WorldPoint> dumbPathing(final WorldPoint start, final WorldPoint destination, final int actorWidth, final int actorHeight, final List<WorldPoint> blacklist, @Nullable LocalCollisionMap localMap) {
        IntArrayList blist = new IntArrayList();
        if(blacklist != null && !blacklist.isEmpty())
        {
            blacklist.forEach(p -> blist.add(WorldPointUtil.compress(p)));
        }

        List<WorldPoint> path = new ArrayList<>();
        int plane = start.getPlane();
        if (plane != destination.getPlane()) {
            return path;
        }

        int curX = start.getX();
        int curY = start.getY();
        int destX = destination.getX();
        int destY = destination.getY();

        while (curX != destX || curY != destY) {
            int difX = destX - curX;
            int difY = destY - curY;
            int dx = Integer.signum(difX);
            int dy = Integer.signum(difY);

            if(((difX == 1 && difY == 1) || (difX == 1 && difY == -1)) && cantStep(localMap, curX, curY, plane, 1, 0)) {
                break;
            } else if(((difX == -1 && difY == -1) || (difX == -1 && difY == 1)) && cantStep(localMap, curX, curY, plane, -1, 0)) {
                break;
            }

            else if (canStep(localMap, curX, curY, plane, dx, dy, actorWidth, actorHeight, blist)) {
                curX += dx;
                curY += dy;
            } else if (dx != 0 && dy != 0 && canStep(localMap, curX, curY, plane, dx, 0, actorWidth, actorHeight, blist)) {
                curX += dx;
            } else if (dx != 0 && dy != 0 && canStep(localMap, curX, curY, plane, 0, dy, actorWidth, actorHeight, blist)) {
                curY += dy;
            } else {
                break;
            }

            path.add(new WorldPoint(curX, curY, plane));
        }
        return path;
    }

    private static boolean canStep(final LocalCollisionMap localMap, final int currX, final int currY, final int curZ, final int dx, final int dy, final int actorWidth, final int actorHeight, final IntArrayList blacklist)
    {
        for (int x = 0; x < actorWidth; x++) {
            for (int y = 0; y < actorHeight; y++) {
                if(blacklist.contains(WorldPointUtil.compress(currX + x, currY + y, curZ)))
                    return false;
                else if (cantStep(localMap, currX + x, currY + y, curZ, dx, dy)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean loacalCantStep(LocalCollisionMap localMap, final int currX, final int currY, final int curZ, final int dx, final int dy)
    {
        final short x = (short) currX;
        final short y = (short) currY;
        final byte plane = (byte) curZ;

        switch((dx + 1) * 3 + (dy + 1))
        {
            case 0:
                return localMap.sw(x, y, plane);
            case 1:
                return localMap.w(x, y, plane);
            case 2:
                return localMap.nw(x, y, plane);
            case 3:
                return localMap.s(x, y, plane);
            case 5:
                return localMap.n(x, y, plane);
            case 6:
                return localMap.se(x, y, plane);
            case 7:
                return localMap.e(x, y, plane);
            case 8:
                return localMap.ne(x, y, plane);
        }

        return true;
    }

    private static boolean cantStep(final LocalCollisionMap localMap, final int currX, final int currY, final int curZ, final int dx, final int dy)
    {
        if(localMap != null)
        {
            return loacalCantStep(localMap, currX, currY, curZ, dx, dy);
        }

        CollisionMap map = Pathfinder.getCollisionMap();

        final short x = (short) currX;
        final short y = (short) currY;
        final byte plane = (byte) curZ;

        final byte flags = map.all(x, y, plane);
        switch (flags)
        {
            case Flags.ALL:
                return false;
            case Flags.NONE:
                return true;
        }

        switch((dx + 1) * 3 + (dy + 1))
        {
            case 0:
                return (flags & Flags.SOUTHWEST) == 0;
            case 1:
                return (flags & Flags.WEST) == 0;
            case 2:
                return (flags & Flags.NORTHWEST) == 0;
            case 3:
                return (flags & Flags.SOUTH) == 0;
            case 5:
                return (flags & Flags.NORTH) == 0;
            case 6:
                return (flags & Flags.SOUTHEAST) == 0;
            case 7:
                return (flags & Flags.EAST) == 0;
            case 8:
                return (flags & Flags.NORTHEAST) == 0;
        }

        return true;
    }

    public static List<WorldPoint> actorTiles(final Actor target) {
        List<WorldPoint> actorTiles = new ArrayList<>();
        for (NPC npc : GameManager.npcList() ) {
            if (npc == null || npc == target)
                continue;
            actorTiles.addAll(npc.getWorldArea().toWorldPointList());
        }

        if(target instanceof Player)
            return actorTiles;

        Client client = Static.getClient();
        for (Player player : GameManager.playerList()) {
            if (player == null || player == client.getLocalPlayer())
                continue;
            actorTiles.add(player.getWorldLocation());
        }
        return actorTiles;
    }
}