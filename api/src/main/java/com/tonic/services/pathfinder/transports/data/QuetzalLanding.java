package com.tonic.services.pathfinder.transports.data;

import com.tonic.api.game.VarAPI;
import com.tonic.services.pathfinder.requirements.*;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

@Getter
public enum QuetzalLanding
{
    CIVITAS_ILLA_FORTIS(1, "Civitas illa Fortis", new WorldPoint(1697, 3140, 0), true),
    THE_TEOMAT(2, "The Teomat", new WorldPoint(1437, 3171, 0), true),
    SUNSET_COAST(3, "Sunset Coast", new WorldPoint(1548, 2995, 0), true),
    HUNTER_GUILD(4, "Hunter Guild", new WorldPoint(1585, 3053, 0), true),
    CAM_TORUM_ENTRANCE(5, "Cam Torum Entrance", new WorldPoint(1446, 3108, 0), false),
    COLOSSAL_WYRM_REMAINS(6, "Colossal Wyrm Remains", new WorldPoint(1670, 2933, 0), false),
    OUTER_FORTIS(7, "Outer Fortis", new WorldPoint(1700, 3037, 0), false),
    FORTIS_COLOSSEUM(8, "Fortis Colosseum", new WorldPoint(1779, 3111, 0), false),
    ALDARIN(9, "Aldarin", new WorldPoint(1389, 2901, 0), true),
    QUETZACALLI_GORGE(10, "Quetzacalli Gorge", new WorldPoint(1510, 3222, 0), true),
    SALVAGER_OVERLOOK(11, "Salvager Overlook", new WorldPoint(1613, 3300, 0), false),
    TAL_TEKLAN(12, "Tal Teklan", new WorldPoint(1226, 3091, 0), true),
    AUBURNVALE(13, "Auburnvale", new WorldPoint(1411, 3361, 0), true),
    KASTORI(14, "Kastori", new WorldPoint(1344, 3022, 0), false)
    ;

    private final int id;
    private final String name;
    private final WorldPoint location;
    private final boolean autoUnlocked;
    private final Requirements requirements;

    QuetzalLanding(int id, String name, WorldPoint location, boolean autoUnlocked)
    {
        this.id = id;
        this.name = name;
        this.location = location;
        this.autoUnlocked = autoUnlocked;
        this.requirements = new Requirements();
        this.requirements.addRequirement(new WorldRequirement(true));
        this.requirements.addRequirement(new QuestRequirement(Quest.TWILIGHTS_PROMISE, QuestState.FINISHED));
        this.requirements.addRequirement(new VarRequirement(Comparison.GREATER_THAN_EQUAL, VarType.VARBIT, VarbitID.QUETZAL_FORTIS, 2));
        this.requirements.addRequirement(new OtherRequirement(this::isUnlocked));
    }

    public boolean isUnlocked()
    {
        return autoUnlocked || (VarAPI.getVarp(VarPlayerID.QUETZALS_UNLOCKED) & (1 << id)) != 0;
    }
}
