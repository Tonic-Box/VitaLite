package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.NpcAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.NpcQuery;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.data.MinecartNetwork;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;

import java.util.function.Consumer;

/**
 * The Kourend minecart network: every station's conductor rides to every other station.
 */
public final class Minecarts
{
    private static final String NAME = "Minecart";
    private static final int DURATION = 5;

    private Minecarts()
    {
    }

    /**
     * Adds a ride from every minecart station to every other.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (MinecartNetwork minecart : MinecartNetwork.values())
        {
            for (MinecartNetwork destination : MinecartNetwork.values())
            {
                if (minecart == destination)
                {
                    continue;
                }

                add.accept(new CodeTransport(NAME, minecart.getLocation(), destination.getLocation(), DURATION, MinecartNetwork.getRequirements(), () -> ride(minecart, destination)));
            }
        }
    }

    private static StepHandler ride(MinecartNetwork minecart, MinecartNetwork destination)
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    NpcEx npc = new NpcQuery().withName(minecart.getNpcName()).first();
                    NpcAPI.interact(npc, "Travel");
                    return 1;
                })
                .addDelayUntil(1, MenuOptions.MENU_TIMEOUT, () -> MenuOptions.menuSlot(destination.name()) != -1, () ->
                {
                })
                .add(2, () -> MenuOptions.resumeMenu(destination.name(), 3))
                .addDelay(3, 5)
                .build();
    }
}
