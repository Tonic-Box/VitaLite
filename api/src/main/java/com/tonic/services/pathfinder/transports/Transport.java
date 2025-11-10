package com.tonic.services.pathfinder.transports;

import com.tonic.util.handler.StepHandler;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.util.WorldPointUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
@AllArgsConstructor
public class Transport
{
    int source;
    int destination;
    int sourceRadius;
    int destinationRadius;
    int duration;
    StepHandler handler;
    Requirements requirements;
    int id;

    public Transport(WorldPoint source,
                     WorldPoint destination,
                     int sourceRadius,
                     int destinationRadius,
                     StepHandler handler,
                     int id
    )
    {
        this.source = WorldPointUtil.compress(source);
        this.destination = WorldPointUtil.compress(destination);
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = handler;
        this.requirements = new Requirements();
        this.duration = 1;
        this.id = id;
    }

    public Transport(WorldPoint source,
                     WorldPoint destination,
                     int sourceRadius,
                     int destinationRadius,
                     StepHandler handler,
                     Requirements requirements,
                     int id
    )
    {
        this.source = WorldPointUtil.compress(source);
        this.destination = WorldPointUtil.compress(destination);
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = handler;
        this.requirements = requirements;
        this.duration = 1;
        this.id = id;
    }

    public Transport(int source,
                     int destination,
                     int sourceRadius,
                     int destinationRadius,
                     StepHandler handler,
                     int id
    )
    {
        this.source = source;
        this.destination = destination;
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = handler;
        this.requirements = new Requirements();
        this.duration = 1;
        this.id = id;
    }

    public Transport(int source,
                     int destination,
                     int sourceRadius,
                     int destinationRadius,
                     StepHandler handler,
                     int id,
                     int duration
    )
    {
        this.source = source;
        this.destination = destination;
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = handler;
        this.requirements = new Requirements();
        this.duration = duration;
        this.id = id;
    }

    public Transport(int source,
                     int destination,
                     int sourceRadius,
                     int destinationRadius,
                     StepHandler handler,
                     Requirements requirements,
                     int id
    )
    {
        this.source = source;
        this.destination = destination;
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = handler;
        this.requirements = requirements;
        this.duration = 1;
        this.id = id;
    }

    public Transport(WorldPoint source,
                     WorldPoint destination,
                     int sourceRadius,
                     int destinationRadius,
                     StepHandler handler,
                     int delayAfter,
                     int id
    )
    {
        this.source = WorldPointUtil.compress(source);
        this.destination = WorldPointUtil.compress(destination);
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = handler;
        this.requirements = new Requirements();
        this.duration = delayAfter;
        this.id = id;
    }
}