package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.queries.WidgetQuery;
import com.tonic.util.TextUtil;
import lombok.Getter;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import static net.runelite.api.gameval.VarbitID.*;

/**
 * API for retrieving boat statistics in sailing.
 * NOTE: These will only work if currently on a boat.
 */
public class BoatStatsAPI
{
    @Getter
    private static int rapidResistance = 0;
    @Getter
    private static int stormResistance = 0;
    @Getter
    private static int fetidWaterResistance = 0;
    @Getter
    private static int crystalFleckedResistance = 0;
    @Getter
    private static int tangledKelpResistance = 0;
    @Getter
    private static int iceResistance = 0;


    // === Boat Resistances ===
    public static boolean isRapidResistant()
    {
        //TileType.F_TEMPOR_STORM_WATER
        return rapidResistance > 0;
    }

    public static boolean isStormResistant()
    {
        //TileType.F_TEMPOR_STORM_WATER
        return stormResistance > 0;
    }

    public static boolean isFetidWaterResistant()
    {
        return fetidWaterResistance > 0;
    }

    public static boolean isCrystalFleckedResistant()
    {
        return crystalFleckedResistance > 0;
    }

    public static boolean isTangledKelpResistant()
    {
        return tangledKelpResistance > 0;
    }

    public static boolean isIceResistant()
    {
        return iceResistance > 0;
    }

    public static void update(VarbitChanged event)
    {
        switch(event.getVarbitId())
        {
            case SAILING_SIDEPANEL_BOAT_RAPIDRESISTANCE:
                rapidResistance = event.getValue();
                break;
            case SAILING_SIDEPANEL_BOAT_STORMRESISTANCE:
                stormResistance = event.getValue();
                break;
            case SAILING_SIDEPANEL_BOAT_FETIDWATER_RESISTANT:
                fetidWaterResistance = event.getValue();
                break;
            case SAILING_SIDEPANEL_BOAT_CRYSTALFLECKED_RESISTANT:
                crystalFleckedResistance = event.getValue();
                break;
            case SAILING_SIDEPANEL_BOAT_TANGLEDKELP_RESISTANT:
                tangledKelpResistance = event.getValue();
                break;
            case SAILING_SIDEPANEL_BOAT_ICYSEAS_RESISTANT:
                iceResistance = event.getValue();
                break;
        }
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
