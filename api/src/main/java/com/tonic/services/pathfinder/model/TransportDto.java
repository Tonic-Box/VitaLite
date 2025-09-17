package com.tonic.services.pathfinder.model;

import com.tonic.services.pathfinder.Transport;
import com.tonic.services.pathfinder.TransportLoader;
import com.tonic.services.pathfinder.requirements.Requirements;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
public class TransportDto
{
    WorldPoint source;
    WorldPoint destination;
    String action;
    Integer objectId;
    Requirements requirements;

    public Transport toTransport()
    {
        return null; //TransportLoader.objectTransport(source, destination, objectId, action, requirements);
    }
}