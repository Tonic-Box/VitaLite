package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.Comparison;
import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.VarRequirement;
import com.tonic.services.pathfinder.requirements.VarType;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import com.tonic.services.pathfinder.transports.CodeTransport;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepHandler;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The boats between Port Sarim, Port Piscarilius and Land's End, crossing the gangplank off the ship on landing.
 */
public final class BoatAndPlank
{
    private static final WorldPoint PORT_SARIM = new WorldPoint(3055, 3245, 0);
    private static final WorldPoint PORT_PISCARILIUS = new WorldPoint(1824, 3691, 0);
    private static final WorldPoint LANDS_END = new WorldPoint(1504, 3399, 0);
    private static final String HERBERT = "Cabin Boy Herbert";
    private static final int NPC_RADIUS = 10;
    private static final int DELAY = 4;
    private static final int DURATION = 5;
    private static final int DONE = 99;

    private BoatAndPlank()
    {
    }

    /**
     * Adds Cabin Boy Herbert's boats from Port Sarim and Port Piscarilius and Magoro's from Land's End.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        Requirements herbert = new Requirements();
        herbert.addRequirement(new VarRequirement(Comparison.NOT_EQUAL, VarType.VARBIT, VarbitID.ZEAH_PLAYERHASVISITED, 0));
        herbert.addRequirement(new QuestRequirement(Quest.A_KINGDOM_DIVIDED, QuestState.IN_PROGRESS, QuestState.FINISHED));
        herbert.addRequirement(new WorldRequirement(true));

        Requirements magoro = new Requirements();
        magoro.addRequirement(new WorldRequirement(true));

        add.accept(new CodeTransport(HERBERT, PORT_SARIM, new WorldPoint(1824, 3695, 1), DURATION, herbert, () -> sail(() -> named(HERBERT, PORT_SARIM), "Port Piscarilius")));
        add.accept(new CodeTransport(HERBERT, PORT_SARIM, LANDS_END, DURATION, herbert, () -> sail(() -> named(HERBERT, PORT_SARIM), "Land's End")));
        add.accept(new CodeTransport(HERBERT, PORT_PISCARILIUS, PORT_SARIM, DURATION, herbert, () -> sail(() -> named(HERBERT, PORT_PISCARILIUS), "Port Sarim")));
        add.accept(new CodeTransport(HERBERT, PORT_PISCARILIUS, LANDS_END, DURATION, herbert, () -> sail(() -> named(HERBERT, PORT_PISCARILIUS), "Land's End")));
        add.accept(new CodeTransport("Magoro", LANDS_END, PORT_SARIM, DURATION, magoro, () -> sail(() -> withId(NpcID.ZEAH_MAGORO, LANDS_END), "Port Sarim")));
        add.accept(new CodeTransport("Magoro", LANDS_END, new WorldPoint(1824, 3691, 0), DURATION, magoro, () -> sail(() -> withId(NpcID.ZEAH_MAGORO, LANDS_END), "Port Piscarilius")));
    }

    private static NpcEx named(String name, WorldPoint source)
    {
        return new NpcQuery()
                .withName(name)
                .within(source, NPC_RADIUS)
                .first();
    }

    private static NpcEx withId(int id, WorldPoint source)
    {
        return new NpcQuery()
                .withIds(id)
                .within(source, NPC_RADIUS)
                .first();
    }

    private static StepHandler sail(Supplier<NpcEx> npcFinder, String action)
    {
        return HandlerBuilder.get()
                .add(0, () ->
                {
                    NpcEx npc = npcFinder.get();
                    if (npc != null)
                    {
                        NpcAPI.interact(npc, action);
                        return 1;
                    }
                    return 0;
                })
                .addDelayUntil(1, () -> !MovementAPI.isMoving())
                .addDelay(2, DELAY)
                .add(3, () ->
                {
                    WorldPoint worldPoint = PlayerEx.getLocal().getWorldPoint();
                    if (worldPoint.getPlane() != 1)
                    {
                        return DONE;
                    }
                    TileObjectEx plank = new TileObjectQuery()
                            .withAction("Cross")
                            .nearest();

                    if (plank == null)
                    {
                        return DONE;
                    }
                    TileObjectAPI.interact(plank, "Cross");
                    return 4;
                })
                .addDelay(4, 1)
                .build();
    }
}
