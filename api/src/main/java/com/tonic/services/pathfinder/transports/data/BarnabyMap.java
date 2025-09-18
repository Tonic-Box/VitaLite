package com.tonic.services.pathfinder.transports.data;

import com.tonic.services.pathfinder.requirements.ItemRequirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import lombok.Getter;
import net.runelite.api.ItemID;

@Getter
public enum BarnabyMap
{
    ARDOUGNE(BarnabyShip.ARDOUGNE, BarnabyShip.BRIMHAVEN, BarnabyShip.RIMMINGTON),
    BRIMHAVEN(BarnabyShip.BRIMHAVEN, BarnabyShip.ARDOUGNE, BarnabyShip.RIMMINGTON),
    RIMMINGTON(BarnabyShip.RIMMINGTON, BarnabyShip.ARDOUGNE, BarnabyShip.BRIMHAVEN)

    ;

    BarnabyMap(BarnabyShip barnabyShip, BarnabyShip... destinations)
    {
        this.barnabyShip = barnabyShip;
        this.destinations = destinations;
        this.requirements = new Requirements();
        requirements.getItemRequirements().add(new ItemRequirement(false, 30, ItemID.COINS_995));
        requirements.getWorldRequirements().add(new WorldRequirement(true));
    }

    private final  BarnabyShip barnabyShip;
    private final BarnabyShip[] destinations;
    private final Requirements requirements;
}