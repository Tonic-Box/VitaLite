package com.tonic.services.pathfinder.transports.data;

import com.tonic.Static;
import com.tonic.api.game.QuestAPI;
import com.tonic.services.pathfinder.requirements.*;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;

@Getter
public enum SpiritTree
{
    TREE_GNOME_VILLAGE(new WorldPoint(2542, 3170, 0), 0, new QuestRequirement(Quest.TREE_GNOME_VILLAGE, QuestState.FINISHED)),
    GNOME_STRONGHOLD(new WorldPoint(2461, 3444, 0), 1, new QuestRequirement(Quest.THE_GRAND_TREE, QuestState.FINISHED)),
    BATTLEFIELD_OF_KHAZARD(new WorldPoint(2555, 3259, 0), 2),
    GRAND_EXCHANGE(new WorldPoint(3185, 3508, 0), 3),
    FELDIP_HILLS(new WorldPoint(2488, 2850, 0), 4),

    ;

    SpiritTree(WorldPoint location, int index, Requirement... requirements)
    {
        Client client = Static.getClient();
        this.location = location;
        this.index = index;
        this.requirements = new Requirements();
        this.requirements.addRequirement(new OtherRequirement(() -> QuestAPI.getState(Quest.TREE_GNOME_VILLAGE).equals(QuestState.FINISHED) || QuestAPI.getState(Quest.THE_GRAND_TREE).equals(QuestState.FINISHED)));
        this.requirements.addRequirements(requirements);
        this.requirements.addRequirement(new WorldRequirement(true));
    }

    private final WorldPoint location;
    private final int index;
    private final Requirements requirements;
}