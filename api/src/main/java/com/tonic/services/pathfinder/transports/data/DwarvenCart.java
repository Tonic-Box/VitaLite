package com.tonic.services.pathfinder.transports.data;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.threaded.DialogueNode;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.data.TileObjectEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.requirements.*;
import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;

@Getter
public enum DwarvenCart
{
    GRAND_EXCHANGE(new WorldPoint(3141, 3504, 0), new WorldPoint(2923, 10170, 0), null, "Grand Exchange", 0),
    ICE_MOUNTAIN(new WorldPoint(2995, 9835, 0), new WorldPoint(2923, 10174, 0), "Cart conductor", "Ice Mountain", 150),
    WHITE_WOLF_MOUNTAIN(new WorldPoint(2876, 9868, 0), new WorldPoint(2919, 10168, 0), "Cart conductor", "White Wolf Mountain", 150)
    ;

    DwarvenCart(WorldPoint location, WorldPoint keldegrimLocation, String npcName, String destinationName, int tripBackCost, Requirement... requirements)
    {
        this.location = location;
        this.keldegrimLocation = keldegrimLocation;
        this.npcName = npcName;
        this.destinationName = destinationName;
        this.requirements = new Requirements();
        this.requirements.addRequirement(new QuestRequirement(Quest.THE_GIANT_DWARF, QuestState.IN_PROGRESS, QuestState.FINISHED));
        this.requirements.addRequirement(new WorldRequirement(true));
        if(tripBackCost > 0)
            this.requirements.addRequirement(new ItemRequirement(false, tripBackCost, ItemID.COINS_995));
        this.requirements.addRequirements(requirements);
    }

    private final WorldPoint location;
    private final WorldPoint destination = new WorldPoint(2909, 10174, 0);
    private final WorldPoint keldegrimLocation;
    private final String npcName;
    private final String destinationName;
    private final Requirements requirements;

    public final static WorldPoint KELDEGRIM_WORLDPOINT = new WorldPoint(2906, 10173, 0);

    public void rideBack()
    {
        if(npcName == null)
        {
            TileObjectEx object = new TileObjectQuery<>()
                    .withNameContains("Trapdoor")
                    .first();
            TileObjectAPI.interact(object, "Travel");
            return;
        }

        NPC npc = new NpcQuery().withName(npcName).nearest();
        NpcAPI.interact(npc, "Tickets");
        while (!DialogueAPI.dialoguePresent())
        {
            Delays.tick();
        }
        DialogueNode.get()
                .node("Keldagrim")
                .process();
        TileObjectEx object = new TileObjectQuery<>()
                .withNameContains("Train cart")
                .first();
        TileObjectAPI.interact(object, "Ride");
    }

    public void rideThere()
    {
        if(npcName != null)
        {
            NPC npc = new NpcQuery().withName("Cart conductor").nearest();
            NpcAPI.interact(npc, "Tickets");
            while (!DialogueAPI.dialoguePresent())
            {
                Delays.tick();
            }
            DialogueNode.get()
                    .node(destinationName)
                    .process();
        }

        MovementAPI.walkToWorldPoint(keldegrimLocation);
        Delays.tick();
        Delays.waitUntil(() -> !MovementAPI.isMoving());
        TileObjectEx object = new TileObjectQuery<>()
                .withNameContains("Train cart")
                .first();
        TileObjectAPI.interact(object, "Ride");
    }
}