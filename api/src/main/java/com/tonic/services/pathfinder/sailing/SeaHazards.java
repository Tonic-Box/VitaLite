package com.tonic.services.pathfinder.sailing;

import com.tonic.services.pathfinder.sailing.sea.BoatHull;
import com.tonic.services.pathfinder.sailing.sea.BoatPose;
import com.tonic.services.pathfinder.sailing.sea.SeaPlanner;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a boat's hull must keep off over its next ticks on a race or a voyage, read from the sea each tick
 */
final class SeaHazards
{
    private static final int SHARK_MARGIN = 1;
    private static final int LIGHTNING_CLOUD_SIZE = 4;
    private static final int STRIKE_SLACK = 1;

    private final SeaWorld world;
    private final CloudStrikes clouds;
    private final int horizon;

    public SeaHazards(SeaWorld world, CloudStrikes clouds, int horizon)
    {
        if (world == null || clouds == null)
        {
            throw new IllegalArgumentException("world and clouds cannot be null");
        }
        if (horizon < 0)
        {
            throw new IllegalArgumentException("horizon cannot be negative");
        }
        this.world = world;
        this.clouds = clouds;
        this.horizon = horizon;
    }

    /**
     * Gets what the hull keeps off from now, tick by tick from this tick
     * @return the obstacles over the horizon
     */
    public SeaPlanner.Obstacles now()
    {
        List<Set<WorldPoint>> byTick = new ArrayList<>();
        for (int tick = 0; tick <= horizon; tick++)
        {
            byTick.add(new HashSet<>());
        }
        Set<WorldPoint> always = new HashSet<>();
        addBoats(always, byTick);
        addStrikes(byTick);
        addSharks(byTick);
        return new SeaPlanner.Obstacles(always, byTick);
    }

    private void addBoats(Set<WorldPoint> always, List<Set<WorldPoint>> byTick)
    {
        for (SeaWorld.Vessel other : world.vessels())
        {
            if (!other.npcOwned())
            {
                continue;
            }
            BoatPose next = other.next();
            int nowX = other.now().fineX();
            int nowY = other.now().fineY();
            BoatHull theirs = other.hull();
            if (nowX == next.fineX() && nowY == next.fineY())
            {
                always.addAll(theirs.tiles(next));
                continue;
            }
            for (int tick = 0; tick <= horizon; tick++)
            {
                byTick.get(tick).addAll(theirs.tiles(new BoatPose(nowX + (next.fineX() - nowX) * tick, nowY + (next.fineY() - nowY) * tick, next.angle())));
            }
        }
    }

    private void addStrikes(List<Set<WorldPoint>> byTick)
    {
        int tick = world.tick();
        for (CloudStrikes.Strike strike : clouds.due(tick, horizon + STRIKE_SLACK))
        {
            int due = strike.tick() - tick;
            for (int at = Math.max(0, due - STRIKE_SLACK); at <= Math.min(horizon, due + STRIKE_SLACK); at++)
            {
                addSquare(byTick.get(at), strike.tile().getX(), strike.tile().getY(), LIGHTNING_CLOUD_SIZE, 0);
            }
        }
    }

    private void addSharks(List<Set<WorldPoint>> byTick)
    {
        for (SeaWorld.Creature npc : world.npcs())
        {
            if (!isShark(npc.id()))
            {
                continue;
            }
            int size = Math.max(1, npc.size());
            for (Set<WorldPoint> tiles : byTick)
            {
                addSquare(tiles, npc.tile().getX(), npc.tile().getY(), size, SHARK_MARGIN);
            }
        }
    }

    private static boolean isShark(int id)
    {
        return id == NpcID.SAILING_HAMMERHEAD_SHARK || id == NpcID.SAILING_TIGER_SHARK || id == NpcID.SAILING_GREAT_WHITE_SHARK;
    }

    private static void addSquare(Set<WorldPoint> tiles, int southWestX, int southWestY, int size, int margin)
    {
        for (int x = southWestX - margin; x < southWestX + size + margin; x++)
        {
            for (int y = southWestY - margin; y < southWestY + size + margin; y++)
            {
                tiles.add(new WorldPoint(x, y, 0));
            }
        }
    }
}
