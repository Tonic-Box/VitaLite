package com.tonic.services.pathfinder.transports.data;

import com.tonic.services.pathfinder.requirements.*;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

/**
 * Quetzal Transport System landing sites in Varlamore.
 * 8 destinations are unlocked by default.
 * 6 destinations must be built by the player.
 */
@Getter
public enum QuetzalTransport
{
    // Interface index order (0-13)
    CIVITAS_ILLA_FORTIS("Civitas illa Fortis", new WorldPoint(1697, 3140, 0), VarbitID.QUETZAL_FORTIS, 0),
    THE_TEOMAT("The Teomat", new WorldPoint(1437, 3171, 0), VarbitID.QUETZAL_TEOMAT, 1),
    SUNSET_COAST("Sunset Coast", new WorldPoint(1548, 2995, 0), VarbitID.QUETZAL_SUNSETCOAST, 2),
    HUNTER_GUILD("Hunter Guild", new WorldPoint(1585, 3053, 0), VarbitID.QUETZAL_HUNTERGUILD, 3),
    CAM_TORUM_ENTRANCE("Cam Torum entrance", new WorldPoint(1495, 3753, 0), VarbitID.QUETZAL_CAMTORUM, 4),
    COLOSSAL_WYRM_REMAINS("Colossal Wyrm Remains", new WorldPoint(1350, 3849, 0), VarbitID.QUETZAL_COLOSSALWYRM, 5),
    OUTER_FORTIS("Outer Fortis", new WorldPoint(1800, 3425, 0), VarbitID.QUETZAL_OUTERFORTIS, 6),
    FORTIS_COLOSSEUM("Fortis Colosseum", new WorldPoint(1791, 3107, 0), VarbitID.QUETZAL_COLOSSEUM, 7),
    ALDARIN("Aldarin", new WorldPoint(1389, 2901, 0), VarbitID.QUETZAL_ALDARIN, 8),
    QUETZACALLI_GORGE("Quetzacalli Gorge", new WorldPoint(1510, 3222, 0), VarbitID.QUETZAL_QUETZACALLIGORGE, 9),
    SALVAGER_OVERLOOK("Salvager Overlook", new WorldPoint(1150, 3480, 0), VarbitID.QUETZAL_SALVAGEROVERLOOK, 10),
    TAL_TEKLAN("Tal Teklan", new WorldPoint(1226, 3091, 0), VarbitID.QUETZAL_TALTEKLAN, 11),
    AUBURNVALE("Auburnvale", new WorldPoint(1411, 3361, 0), VarbitID.QUETZAL_AUBURNVALLEY, 12),
    KASTORI("Kastori", new WorldPoint(1585, 3053, 0), VarbitID.QUETZAL_KASTORI, 13);

    private final String name;
    private final WorldPoint location;
    private final Requirements requirements;
    private final int varbitId;
    private final int interfaceIndex;

    QuetzalTransport(String name, WorldPoint location, int varbitId, int interfaceIndex)
    {
        this.name = name;
        this.location = location;
        this.varbitId = varbitId;
        this.interfaceIndex = interfaceIndex;
        this.requirements = new Requirements();
        this.requirements.addRequirements(new QuestRequirement(Quest.TWILIGHTS_PROMISE, QuestState.FINISHED));
        this.requirements.addRequirements(new VarRequirement(Comparison.GREATER_THAN, VarType.VARBIT, varbitId, 0));
    }

    public int getIndex()
    {
        return interfaceIndex;
    }
}
