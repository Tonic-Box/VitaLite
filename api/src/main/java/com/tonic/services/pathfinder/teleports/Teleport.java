package com.tonic.services.pathfinder.teleports;

import com.tonic.util.WorldPointUtil;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Value
public class Teleport
{
    WorldPoint destination;
    int radius;
    List<Runnable> handlers;

    public Teleport(WorldPoint destination, int radius, List<Runnable> handlers){
        this.destination = destination;
        this.radius = radius;
        this.handlers = handlers;
    }

    public Teleport(WorldPoint destination, int radius, Runnable handler){
        this.destination = destination;
        this.radius = radius;
        this.handlers = new ArrayList<>() {{
            add(handler);
        }};
    }

    public static List<Teleport> buildTeleportLinks()
    {
        return new ArrayList<>(TeleportLoader.buildTeleports());
    }

    public Teleport copy()
    {
        return new Teleport(
                WorldPointUtil.fromCompressed(WorldPointUtil.compress(destination)),
                radius,
                new ArrayList<>(handlers)
        );
    }
}