package com.tonic.services.pathfinder.abstractions;

import com.tonic.services.pathfinder.transports.Transport;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

public interface IStep
{
    WorldPoint getPosition();
    Transport getTransport();
    int getPackedPosition();
    boolean hasTransport();
    static List<WorldPoint> toWorldPoints(List<? extends IStep> steps)
    {
        return steps.stream().map(IStep::getPosition).collect(Collectors.toList());
    }
}
