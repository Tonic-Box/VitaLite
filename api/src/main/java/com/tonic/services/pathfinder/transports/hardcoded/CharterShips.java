package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.NpcQuery;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.data.CharterMap;
import com.tonic.services.pathfinder.transports.data.CharterShip;
import com.tonic.util.DialogueNode;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;

import java.util.function.Consumer;

/**
 * The charter ships: from each port's crew to every port its charter map lists.
 */
public final class CharterShips
{
    private static final String NAME = "Charter ship";
    private static final int DURATION = 8;

    private CharterShips()
    {
    }

    /**
     * Adds a charter from every port to each destination its map lists.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        for (CharterMap map : CharterMap.values())
        {
            CharterShip charterShip = map.getCharterShip();
            for (CharterShip destination : map.getDestinations())
            {
                add.accept(new CodeTransport(NAME, charterShip.getLocation(), destination.getArival(), DURATION, destination.getRequirements(), () -> charter(destination)));
            }
        }
    }

    private static StepHandler charter(CharterShip destination)
    {
        DialogueNode node = DialogueNode.get()
                .node("Yes, and don't");
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    NpcEx npc = new NpcQuery().withAction("Charter").sortNearest().first();
                    NpcAPI.interact(npc, "Charter");
                    return 1;
                })
                .addDelayUntil(1, () ->
                {
                    Client client = Static.getClient();
                    return client.getWidget(InterfaceID.SailingMenu.UNIVERSE) != null;
                })
                .add(2, () ->
                {
                    int index = destination.getIndex();
                    if (index == -1)
                    {
                        return 2;
                    }
                    WidgetAPI.interact(1, InterfaceID.CharteringMenuSide.LIST_CONTENT, index, -1);
                    return 3;
                })
                .add(3, () -> 4)
                .addDelayUntil(4, () -> !node.processStep())
                .addDelay(5, 8)
                .build();
    }
}
