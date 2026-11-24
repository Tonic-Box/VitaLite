package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.data.DwarvenCart;

import java.util.function.Consumer;

/**
 * The dwarven carts: from each station to Keldagrim and from Keldagrim back to each station.
 */
public final class DwarvenCarts
{
    private static final String NAME = "Dwarven cart";
    private static final int DURATION_BACK = 22;
    private static final int DURATION_THERE = 21;

    private DwarvenCarts()
    {
    }

    /**
     * Adds a ride from every cart station to Keldagrim and one from Keldagrim to every station.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (DwarvenCart cart : DwarvenCart.values())
        {
            add.accept(new CodeTransport(NAME, cart.getLocation(), cart.getDestination(), DURATION_BACK, cart.getRequirements(), cart::rideBack));
        }

        for (DwarvenCart cart : DwarvenCart.values())
        {
            add.accept(new CodeTransport(NAME, DwarvenCart.KELDEGRIM_WORLDPOINT, cart.getLocation(), DURATION_THERE, cart.getRequirements(), cart::rideThere));
        }
    }
}
