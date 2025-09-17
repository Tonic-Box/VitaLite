package com.tonic.services.pathfinder;

import com.tonic.services.pathfinder.requirements.Requirements;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
@AllArgsConstructor
public class Transport
{
    WorldPoint source;
    WorldPoint destination;
    int sourceRadius;
    int destinationRadius;
    Runnable handler;
    Requirements requirements;

    public Transport(WorldPoint source,
                     WorldPoint destination,
                     int sourceRadius,
                     int destinationRadius,
                     Runnable handler
    )
    {
        this.source = source;
        this.destination = destination;
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = handler;
        this.requirements = new Requirements();
    }
}