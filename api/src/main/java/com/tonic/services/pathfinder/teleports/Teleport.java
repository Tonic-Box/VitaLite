package com.tonic.services.pathfinder.teleports;

import com.tonic.Static;
import com.tonic.api.handlers.GenericHandlerBuilder;
import com.tonic.util.WorldPointUtil;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepContext;
import com.tonic.util.handler.StepHandler;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.List;

/**
 * A way to teleport the walker can start a route with: what fires it and where it lands. Each use builds a fresh handler that fires it, then waits for the player to land within its radius, answering the warnings a teleport raises, and gives up when nothing happens.
 */
public class Teleport
{
    /**
     * How many ticks of nothing happening at all are allowed before the teleport is given up on; one visibly under way is waited out.
     */
    public static final int ARRIVAL_TIMEOUT = 12;

    /**
     * How many ticks a teleport may take before it is called stuck even though the player is still doing something.
     */
    public static final int CAST_LIMIT = 60;

    private static final String IDLE = "TELEPORT_IDLE";
    private static final String SPENT = "TELEPORT_SPENT";
    private static final String ARRIVE = "TELEPORT_ARRIVE";

    @Getter
    private final String name;
    @Getter
    private final WorldPoint destination;
    @Getter
    private final int radius;
    private final List<Runnable> fire;

    /**
     * @param name what it is called, unique among the teleports
     * @param destination where it lands
     * @param radius how close to the destination counts as landed
     * @param fire what fires it, one step per runnable
     */
    public Teleport(String name, WorldPoint destination, int radius, List<Runnable> fire)
    {
        this.name = name;
        this.destination = destination;
        this.radius = radius;
        this.fire = fire;
    }

    /**
     * @param name what it is called, unique among the teleports
     * @param destination where it lands
     * @param radius how close to the destination counts as landed
     * @param fire what fires it
     */
    public Teleport(String name, WorldPoint destination, int radius, Runnable fire)
    {
        this(name, destination, radius, Collections.singletonList(fire));
    }

    /**
     * @return the teleports usable now
     */
    public static List<Teleport> buildTeleportLinks()
    {
        return TeleportLoader.buildTeleports();
    }

    /**
     * Builds a fresh handler that fires the teleport and waits for it to land, giving up after ARRIVAL_TIMEOUT ticks of nothing happening or CAST_LIMIT ticks in all.
     *
     * @return the handler
     */
    public StepHandler buildHandler()
    {
        GenericHandlerBuilder builder = GenericHandlerBuilder.get();
        for (Runnable step : fire)
        {
            builder.add(step);
        }
        return builder.add(ARRIVE, this::arriving).build();
    }

    private Integer arriving(StepContext context)
    {
        if (arrived())
        {
            return HandlerBuilder.END_EXECUTION;
        }
        int elapsed = context.getOrDefault(SPENT, 0) + 1;
        context.put(SPENT, elapsed);
        if (elapsed >= CAST_LIMIT)
        {
            return HandlerBuilder.END_EXECUTION;
        }
        if (casting() || TeleportWarnings.answer())
        {
            context.remove(IDLE);
            return HandlerBuilder.jump(ARRIVE, context);
        }
        int left = context.getOrDefault(IDLE, ARRIVAL_TIMEOUT);
        if (left <= 1)
        {
            return HandlerBuilder.END_EXECUTION;
        }
        context.put(IDLE, left - 1);
        return HandlerBuilder.jump(ARRIVE, context);
    }

    /**
     * @return whether the player, or the world tile it stands on a copy of, is within the radius of the destination on its plane
     */
    public boolean arrived()
    {
        WorldPoint at = Static.invoke(() ->
        {
            Client client = Static.getClient();
            Player player = client.getLocalPlayer();
            return player == null ? null : WorldPointUtil.fromInstance(player.getWorldLocation());
        });
        return at != null && at.getPlane() == destination.getPlane() && at.distanceTo2D(destination) <= radius;
    }

    private static boolean casting()
    {
        return Static.invoke(() ->
        {
            Client client = Static.getClient();
            Player player = client.getLocalPlayer();
            return player != null && player.getAnimation() != -1;
        });
    }
}
