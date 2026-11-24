package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.NpcQuery;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.data.BarnabyMap;
import com.tonic.services.pathfinder.transports.data.BarnabyShip;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;

import java.util.function.Consumer;

/**
 * Captain Barnaby's ships between Ardougne, Brimhaven and Rimmington.
 */
public final class Barnaby
{
    private static final String NAME = "Captain Barnaby";
    private static final int DURATION = 7;

    private Barnaby()
    {
    }

    /**
     * Adds a voyage from every Barnaby port to each of the other two.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (BarnabyMap map : BarnabyMap.values())
        {
            BarnabyShip barnabyShip = map.getBarnabyShip();
            for (BarnabyShip destination : map.getDestinations())
            {
                add.accept(new CodeTransport(NAME, barnabyShip.getLocation(), destination.getArival(), DURATION, map.getRequirements(), () -> sail(destination)));
            }
        }
    }

    private static StepHandler sail(BarnabyShip destination)
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    NpcEx npc = new NpcQuery().withName("Captain Barnaby").first();
                    NpcAPI.interact(npc, destination.getOption());
                })
                .addDelayUntil(1, () -> !MovementAPI.isMoving())
                .addDelay(2, 7)
                .build();
    }
}
