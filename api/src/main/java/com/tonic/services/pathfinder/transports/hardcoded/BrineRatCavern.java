package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.Static;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.services.pathfinder.requirements.ItemRequirement;
import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.WorldPointUtil;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

import java.util.function.Consumer;

/**
 * Into the Brine Rat Cavern under Rellekka by digging beside the Windswept Tree, arriving anywhere in its first rooms.
 */
public final class BrineRatCavern
{
    private static final WorldPoint DIG_SPOT = new WorldPoint(2748, 3732, 0);
    private static final WorldPoint BRINE_RATS = new WorldPoint(2712, 10132, 0);
    private static final WorldArea FIRST_ROOMS = new WorldArea(2689, 10116, 51, 33, 0);
    private static final String DIG = "Dig";
    private static final int DIG_TICKS = 10;

    private BrineRatCavern()
    {
    }

    /**
     * Adds the dig down into the cavern.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        Requirements requirements = new Requirements();
        requirements.addRequirement(new WorldRequirement(true));
        requirements.addRequirement(new QuestRequirement(Quest.OLAFS_QUEST, QuestState.FINISHED));
        requirements.addRequirement(new ItemRequirement(false, 1, ItemID.SPADE));
        add.accept(new CodeTransport(DIG, DIG_SPOT, BRINE_RATS, DIG_TICKS, requirements, BrineRatCavern::dig));
    }

    private static StepHandler dig()
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    ItemEx spade = InventoryAPI.getItem(ItemID.SPADE);
                    if (spade == null)
                    {
                        return 0;
                    }
                    InventoryAPI.interact(spade, DIG);
                    return 1;
                })
                .addDelayUntil(1, DIG_TICKS, BrineRatCavern::inFirstRooms, () ->
                {
                })
                .build();
    }

    private static boolean inFirstRooms()
    {
        WorldPoint at = Static.invoke(() ->
        {
            Client client = Static.getClient();
            Player player = client.getLocalPlayer();
            return player == null ? null : WorldPointUtil.fromInstance(player.getWorldLocation());
        });
        return at == null || FIRST_ROOMS.contains(at);
    }
}
