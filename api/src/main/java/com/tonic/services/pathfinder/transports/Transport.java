package com.tonic.services.pathfinder.transports;

import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.util.WorldPointUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
@AllArgsConstructor
public class Transport
{
    int source;
    int destination;
    int sourceRadius;
    int destinationRadius;
    int duration;
    List<Runnable> handler;
    Requirements requirements;

    public Transport(WorldPoint source,
                     WorldPoint destination,
                     int sourceRadius,
                     int destinationRadius,
                     Runnable handler
    )
    {
        this.source = WorldPointUtil.compress(source);
        this.destination = WorldPointUtil.compress(destination);
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = new ArrayList<>();
        this.handler.add(handler);
        this.requirements = new Requirements();
        this.duration = 1;
    }

    public Transport(WorldPoint source,
                     WorldPoint destination,
                     int sourceRadius,
                     int destinationRadius,
                     Runnable handler,
                     Requirements requirements
    )
    {
        this.source = WorldPointUtil.compress(source);
        this.destination = WorldPointUtil.compress(destination);
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = new ArrayList<>();
        this.handler.add(handler);
        this.requirements = requirements;
        this.duration = 1;
    }

    public Transport(int source,
                     int destination,
                     int sourceRadius,
                     int destinationRadius,
                     Runnable handler
    )
    {
        this.source = source;
        this.destination = destination;
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = new ArrayList<>();
        this.handler.add(handler);
        this.requirements = new Requirements();
        this.duration = 1;
    }

    public Transport(int source,
                     int destination,
                     int sourceRadius,
                     int destinationRadius,
                     Runnable handler,
                     Requirements requirements
    )
    {
        this.source = source;
        this.destination = destination;
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = new ArrayList<>();
        this.handler.add(handler);
        this.requirements = requirements;
        this.duration = 1;
    }

    public Transport(WorldPoint source,
                     WorldPoint destination,
                     int sourceRadius,
                     int destinationRadius,
                     List<Runnable> handler,
                     int delayAfter
    )
    {
        this.source = WorldPointUtil.compress(source);
        this.destination = WorldPointUtil.compress(destination);
        this.sourceRadius = sourceRadius;
        this.destinationRadius = destinationRadius;
        this.handler = handler;
        this.requirements = new Requirements();
        this.duration = delayAfter;
    }
}