package com.tonic.services.pathfinder.transports.data;

import com.tonic.services.pathfinder.requirements.*;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;

@Getter
public enum SpiritTree
{
    TREE_GNOME_VILLAGE(new WorldPoint(2542, 3170, 0)),
    GNOME_STRONGHOLD(new WorldPoint(2461, 3444, 0)),
    BATTLEFIELD_OF_KHAZARD(new WorldPoint(2555, 3259, 0)),
    GRAND_EXCHANGE(new WorldPoint(3185, 3508, 0)),
    FELDIP_HILLS(new WorldPoint(2488, 2850, 0));

    SpiritTree(WorldPoint location, Requirement... requirements)
    {
        this.location = location;
        this.requirements = new Requirements();
        this.requirements.addRequirement(new QuestRequirement(Quest.TREE_GNOME_VILLAGE, QuestState.FINISHED));
        this.requirements.addRequirements(requirements);
        this.requirements.addRequirement(new WorldRequirement(true));
    }

    private final WorldPoint location;
    private final Requirements requirements;
}