package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.game.QuestAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.NpcQuery;
import com.tonic.services.pathfinder.requirements.OtherRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.Distance;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;

import java.util.function.Consumer;

/**
 * The ferry between Rellekka and Waterbirth Island, both ways.
 */
public final class Waterbirth
{
    private static final String NAME = "Jarvald";
    private static final int DURATION = 10;
    private static final int NPC_RADIUS = 10;
    private static final int FARE = 1000;
    private static final WorldPoint ISLAND = new WorldPoint(2544, 3760, 0);
    private static final WorldPoint ISLAND_LANDING = new WorldPoint(2547, 3759, 0);
    private static final WorldPoint RELLEKKA = new WorldPoint(2620, 3682, 0);

    private Waterbirth()
    {
    }

    /**
     * Adds the ferry to Rellekka and the ferry to Waterbirth Island.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        Requirements requirements = new Requirements();
        requirements.addRequirement(new OtherRequirement(() -> QuestAPI.isCompleted(Quest.THE_FREMENNIK_TRIALS) || InventoryAPI.count(ItemID.COINS) >= FARE));
        requirements.addRequirement(new WorldRequirement(true));

        add.accept(new CodeTransport(NAME, ISLAND, RELLEKKA, DURATION, requirements, () -> ferry(ISLAND, RELLEKKA, NpcID.VIKING_DAGGANOTH_CAVE_WATERBIRTH_ISLAND, "Rellekka")));
        add.accept(new CodeTransport(NAME, RELLEKKA, ISLAND_LANDING, DURATION, requirements, () -> ferry(RELLEKKA, ISLAND_LANDING, NpcID.VIKING_DAGGANOTH_CAVE_FERRYMAN_ISLAND, "Waterbirth Island")));
    }

    private static StepHandler ferry(WorldPoint source, WorldPoint destination, int npcId, String action)
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    NpcEx npc = new NpcQuery()
                            .withIds(npcId)
                            .within(source, NPC_RADIUS)
                            .first();
                    if (npc != null)
                    {
                        NpcAPI.interact(npc, action);
                        return 1;
                    }
                    return 0;
                })
                .addDelayUntil(1, () ->
                {
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    return Distance.pathDistanceTo(worldPoint, destination) < 5;
                })
                .build();
    }
}
