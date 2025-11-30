package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.game.VarAPI;
import com.tonic.queries.WidgetQuery;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

public class BoatAPI
{
    // === Boat Resistances ===
    public static boolean isRapidResistant()
    {
        //TileType.F_TEMPOR_STORM_WATER
        return VarAPI.getVarp(VarbitID.SAILING_SIDEPANEL_BOAT_RAPIDRESISTANCE) > 0;
    }

    public static boolean isStormResistant()
    {
        //TileType.F_TEMPOR_STORM_WATER
        return VarAPI.getVarp(VarbitID.SAILING_SIDEPANEL_BOAT_STORMRESISTANCE) > 0;
    }

    public static boolean isFetidWaterResistant()
    {
        return VarAPI.getVarp(VarbitID.SAILING_SIDEPANEL_BOAT_FETIDWATER_RESISTANT) > 0;
    }

    public static boolean isCrystalFleckedResistant()
    {
        return VarAPI.getVarp(VarbitID.SAILING_SIDEPANEL_BOAT_CRYSTALFLECKED_RESISTANT) > 0;
    }

    public static boolean isTangledKelpResistant()
    {
        return VarAPI.getVarp(VarbitID.SAILING_SIDEPANEL_BOAT_TANGLEDKELP_RESISTANT) > 0;
    }

    public static boolean isIceResistant()
    {
        return VarAPI.getVarp(VarbitID.SAILING_SIDEPANEL_BOAT_ICYSEAS_RESISTANT) > 0;
    }

    // === Other ===

    public static int getBoatMaxHealth()
    {
        return Static.invoke(() -> {
            Widget bar = new WidgetQuery(InterfaceID.SailingSidepanel.HEALTH_BAR)
                    .withTextContains("/")
                    .first();
            if(bar == null)
                return -1;
            return Integer.parseInt(bar.getText().split("/")[1].trim());
        });
    }

    public static int getBoatHealth()
    {
        return Static.invoke(() -> {
            Widget bar = new WidgetQuery(InterfaceID.SailingSidepanel.HEALTH_BAR)
                    .withTextContains("/")
                    .first();
            if(bar == null)
                return -1;
            return Integer.parseInt(bar.getText().split("/")[0].trim());
        });
    }
}
