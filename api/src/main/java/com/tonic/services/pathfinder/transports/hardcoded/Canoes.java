package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.data.CanoeStation;

import java.util.function.Consumer;

/**
 * The River Lum canoes: every trip each canoe can make between the four stations.
 */
public final class Canoes
{
    private Canoes()
    {
    }

    /**
     * Adds every canoe trip between the stations.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (CodeTransport transport : CanoeStation.getTravelMatrix())
        {
            add.accept(transport);
        }
    }
}
