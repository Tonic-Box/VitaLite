package com.tonic.services.pathfinder.impl;

import com.tonic.services.pathfinder.abstractions.IStep;
import com.tonic.services.pathfinder.transports.Transport;
import net.runelite.api.coords.WorldPoint;

/**
 * One tile of a planned route, kept as its key: a world tile, or inside an instance either a tile of the scene or the world tile a transport lands on.
 */
public class Step implements IStep
{
    private final int position;
    private final Transport transport;
    private final LiveLayer live;

    /**
     * @param position the tile's key
     * @param transport the transport taken from it, or null
     * @param live the scene the route was planned over
     */
    public Step(int position, Transport transport, LiveLayer live)
    {
        this.position = position;
        this.transport = transport;
        this.live = live == null ? LiveLayer.NONE : live;
    }

    /**
     * @return where the tile stands in the scene, the copy nearest the player inside an instance
     */
    @Override
    public WorldPoint getPosition()
    {
        return live.live(position);
    }

    @Override
    public int getPackedPosition()
    {
        return position;
    }

    @Override
    public Transport getTransport()
    {
        return transport;
    }

    @Override
    public boolean hasTransport()
    {
        return transport != null;
    }
}
