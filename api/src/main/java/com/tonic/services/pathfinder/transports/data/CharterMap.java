package com.tonic.services.pathfinder.transports.data;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum CharterMap
{
    CIVITAS_ILLA_FORTIS(CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.ALDARIN,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CORSAIR_COVE,
            CharterShip.LANDS_END,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PHASMATYS,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.PORT_SARIM,
            CharterShip.SUNSET_COAST,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    BRIMHAVEN(CharterShip.BRIMHAVEN,
            CharterShip.ALDARIN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.LANDS_END,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PHASMATYS,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.PORT_SARIM,
            CharterShip.SUNSET_COAST,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    CATHERBY(CharterShip.CATHERBY,
            CharterShip.ALDARIN,
            CharterShip.BRIMHAVEN,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.LANDS_END,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PHASMATYS,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.PORT_SARIM,
            CharterShip.SUNSET_COAST,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    CORSAIR_COVE(CharterShip.CORSAIR_COVE,
            CharterShip.ALDARIN,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.LANDS_END,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PHASMATYS,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.PORT_SARIM,
            CharterShip.SUNSET_COAST,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    LANDS_END(CharterShip.LANDS_END,
            CharterShip.ALDARIN,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PHASMATYS,
            CharterShip.SUNSET_COAST,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    MUSA_POINT(CharterShip.MUSA_POINT,
            CharterShip.ALDARIN,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.LANDS_END,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PHASMATYS,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.SUNSET_COAST,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    PORT_KHAZARD(CharterShip.PORT_KHAZARD,
            CharterShip.ALDARIN,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.LANDS_END,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_PHASMATYS,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.PORT_SARIM,
            CharterShip.SUNSET_COAST,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    PORT_PHASMATYS(CharterShip.PORT_PHASMATYS,
            CharterShip.ALDARIN,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.LANDS_END,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.PORT_SARIM,
            CharterShip.SUNSET_COAST,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    PORT_PISCARILIUS(CharterShip.PORT_PISCARILIUS,
            CharterShip.ALDARIN,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PHASMATYS,
            CharterShip.SUNSET_COAST,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    PORT_SARIM(CharterShip.PORT_SARIM,
            CharterShip.ALDARIN,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PHASMATYS,
            CharterShip.SUNSET_COAST,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    ALDARIN(CharterShip.ALDARIN,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.LANDS_END,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PHASMATYS,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.PORT_SARIM,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),
    SUNSET_COAST(CharterShip.SUNSET_COAST,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.LANDS_END,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_KHAZARD,
            CharterShip.PORT_PHASMATYS,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.PORT_SARIM,
            CharterShip.PRIFDDINAS,
            CharterShip.PORT_TYRAS
    ),

    PORT_TYRAS(CharterShip.PORT_TYRAS,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.LANDS_END,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_PHASMATYS,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.PORT_SARIM,
            CharterShip.PRIFDDINAS,
            CharterShip.ALDARIN,
            CharterShip.SUNSET_COAST
    ),

    PRIFDDINAS(CharterShip.PRIFDDINAS,
            CharterShip.BRIMHAVEN,
            CharterShip.CATHERBY,
            CharterShip.CIVITAS_ILLA_FORTIS,
            CharterShip.CORSAIR_COVE,
            CharterShip.LANDS_END,
            CharterShip.MUSA_POINT,
            CharterShip.PORT_PHASMATYS,
            CharterShip.PORT_PISCARILIUS,
            CharterShip.PORT_SARIM,
            CharterShip.PORT_TYRAS,
            CharterShip.ALDARIN,
            CharterShip.SUNSET_COAST
    ),

    ;

    CharterMap(CharterShip charterShip, CharterShip... destinations)
    {
        this.charterShip = charterShip;
        this.destinations = destinations;
    }

    private final  CharterShip charterShip;
    private final CharterShip[] destinations;

    public WorldPoint getLocation()
    {
        return charterShip.getLocation();
    }

    public WorldPoint getArival()
    {
        return charterShip.getArival();
    }

    public int getWidgetId()
    {
        return charterShip.getWidgetId();
    }

    public List<CharterShip> getDestinations()
    {
        List<CharterShip> destinations = new ArrayList<>();
        for(CharterShip destination : this.destinations)
        {
            if(destination.getRequirements().fulfilled())
                destinations.add(destination);
        }
        return destinations;
    }
}