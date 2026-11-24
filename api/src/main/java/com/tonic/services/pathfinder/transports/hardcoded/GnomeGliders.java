package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.NpcQuery;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.data.GnomeGlider;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.gameval.InterfaceID;

import java.util.function.Consumer;

/**
 * The gnome glider network: every glider except the Digsite's flies to every other.
 */
public final class GnomeGliders
{
    private static final String NAME = "Gnome glider";
    private static final int DURATION = 4;

    private GnomeGliders()
    {
    }

    /**
     * Adds a flight from every glider but the Digsite's to every other glider.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (GnomeGlider glider : GnomeGlider.values())
        {
            if (glider == GnomeGlider.DIG_SITE)
            {
                continue;
            }
            for (GnomeGlider destination : GnomeGlider.values())
            {
                if (glider == destination)
                {
                    continue;
                }

                add.accept(new CodeTransport(NAME, glider.getLocation(), destination.getLocation(), DURATION, destination.getRequirements(), true, () -> fly(glider, destination)));
            }
        }
    }

    private static StepHandler fly(GnomeGlider glider, GnomeGlider destination)
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    NpcEx npc = new NpcQuery().withName(glider.getNpcName()).first();
                    NpcAPI.interact(npc, "Glider");
                    return 1;
                })
                .addDelayUntil(1, () -> WidgetAPI.get(InterfaceID.Glidermap.UNIVERSE) != null)
                .add(2, () ->
                {
                    WidgetAPI.interact(1, destination.getIndex(), -1, -1);
                    return 3;
                })
                .addDelay(3, 4)
                .build();
    }
}
