package com.tonic.services.pathfinder.transports;

import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.coords.WorldPoint;

import java.util.function.Supplier;

/**
 * A transport too involved for the JSON, defined in code: its handler is built by a factory each time it is used, and it is never written to the transports file.
 */
public class CodeTransport extends Transport
{
    private final transient Supplier<StepHandler> handler;
    private final transient boolean teleportLike;

    /**
     * @param name what it is called
     * @param location the tile it leaves from
     * @param destination the tile it lands on
     * @param duration how many ticks it takes
     * @param requirements what the player needs to use it, or null
     * @param handler builds a fresh handler for each use
     */
    public CodeTransport(String name, WorldPoint location, WorldPoint destination, int duration, Requirements requirements, Supplier<StepHandler> handler)
    {
        this(name, location, destination, duration, requirements, false, handler);
    }

    /**
     * @param name what it is called
     * @param location the tile it leaves from
     * @param destination the tile it lands on
     * @param duration how many ticks it takes
     * @param requirements what the player needs to use it, or null
     * @param teleportLike whether it is a network the walker leaves alone when told not to teleport, such as fairy rings
     * @param handler builds a fresh handler for each use
     */
    public CodeTransport(String name, WorldPoint location, WorldPoint destination, int duration, Requirements requirements, boolean teleportLike, Supplier<StepHandler> handler)
    {
        super(name, -1, true, location, destination, -1, null, duration, requirements);
        this.handler = handler;
        this.teleportLike = teleportLike;
    }

    @Override
    public boolean allowedWithoutTeleports()
    {
        return !teleportLike;
    }

    @Override
    public StepHandler buildHandler()
    {
        return handler.get();
    }
}
