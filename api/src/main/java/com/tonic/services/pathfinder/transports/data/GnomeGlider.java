package com.tonic.services.pathfinder.transports.data;

import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Requirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;

@Getter
public enum GnomeGlider
{
    AL_KHARID(new WorldPoint(3285, 3213, 0), "Captain Dalbur", 9043981),
    DIG_SITE(new WorldPoint(3321, 3431, 0), "Captain Errdo", 9043978), //none
    WOLF_MOUNTAIN(new WorldPoint(2850, 3498, 0), "Captain Bleemadge", 9043975),
    FELDIP_HILLS(
            new WorldPoint(2546, 2972, 0), "Gnormadium Avlafrim", 9043989,
            new QuestRequirement(Quest.ONE_SMALL_FAVOUR, QuestState.FINISHED)
    ),
    THE_GRAND_TREE(new WorldPoint(2464, 3501, 3), "Captain Errdo", 9043972),
    ;

    GnomeGlider(WorldPoint location, String npcName, int index, Requirement... requirements) {
        this.location = location;
        this.npcName = npcName;
        this.index = index;
        this.requirements = new Requirements();
        this.requirements.addRequirements(requirements);
        this.requirements.addRequirement(new QuestRequirement(Quest.THE_GRAND_TREE, QuestState.FINISHED));
        this.requirements.addRequirement(new WorldRequirement(true));
    }

    private final WorldPoint location;
    private final String npcName;
    private final int index;
    private final Requirements requirements;
}