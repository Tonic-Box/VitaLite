package com.tonic.services.pathfinder.transports.data;

import com.tonic.services.pathfinder.requirements.ItemRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldPoint;

@AllArgsConstructor
@Getter
public enum MinecartNetwork
{
    ARCEUUS(new WorldPoint(1670, 3833, 0), "Loinur"),
    FARMING_GUILD(new WorldPoint(1218, 3737, 0), "Ferain"),
    HOSIDIUS_SOUTH(new WorldPoint(1808, 3479, 0), "Elnes"),
    HOSIDIUS_WEST(new WorldPoint(1655, 3543, 0), "Traxi"),
    KINGSTOWN(new WorldPoint(1700, 3659, 0), "Hordal"),
    KOUREND_WOODLAND(new WorldPoint(1572, 3466, 0), "Buneir"),
    LOVAKENGJ(new WorldPoint(1518, 3733, 0), "Miriam"),
    MOUNT_QUIDAMORTUM(new WorldPoint(1255, 3548, 0), "Stuliette"),
    NORTHERN_TUNDRAS(new WorldPoint(1647, 3929, 0), "Lassin"),
    PORT_PISCARILIUS(new WorldPoint(1761, 3710, 0), "Raeli"),
    SHAYZIEN_EAST(new WorldPoint(1590, 3620, 0), "Mogrim"),
    SHAYZIEN_WEST(new WorldPoint(1415, 3577, 0), "Hatna")

    ;
    private final WorldPoint location;
    private final String npcName;
    private final int index = ordinal();

    @Getter
    private static final Requirements requirements;

    static
    {
        requirements = new Requirements();
        requirements.addRequirement(new ItemRequirement(false, 20, ItemID.COINS_995));
        requirements.addRequirement(new WorldRequirement(true));
    }
}