package com.tonic.services.pathfinder;

import net.runelite.api.coords.WorldPoint;
import java.util.List;


public class LongTransport extends Transport
{
    public LongTransport(WorldPoint source, WorldPoint destination, int sourceRadius, int destinationRadius, List<Runnable> handler) {
        super(source, destination, sourceRadius, destinationRadius, null);
        this.handler = handler;
        this.duration = handler.size();
    }
}
