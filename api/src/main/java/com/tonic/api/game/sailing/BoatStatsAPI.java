package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.queries.WidgetQuery;
import com.tonic.util.TextUtil;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * API for retrieving boat statistics in sailing.
 * NOTE: These will only work if currently on a boat.
 */
public class BoatStatsAPI
{
    // === Boat Resistances ===
    public static boolean isRapidResistant()
    {
        //TileType.F_TEMPOR_STORM_WATER
        return readStat("Rapid") > 0;
    }

    public static boolean isStormResistant()
    {
        //TileType.F_TEMPOR_STORM_WATER
        return readStat("Storm") > 0;
    }

    public static boolean isFetidWaterResistant()
    {
        return readStat("Fetid water") > 0;
    }

    public static boolean isCrystalFleckedResistant()
    {
        return readStat("Crystal") > 0;
    }

    public static boolean isTangledKelpResistant()
    {
        return readStat("Tangled kelp") > 0;
    }

    public static boolean isIceResistant()
    {
        return readStat("Ice") > 0;
    }

    public static int readStat(String title)
    {
        title += " resistance";
        String finalTitle = title;
        return Static.invoke(() -> {
            try
            {
                Widget stats = WidgetAPI.get(InterfaceID.SailingSidepanel.STATS_ROWS);
                if(stats == null || stats.getChildren() == null)
                    return -1;
                for(int i = 0; i < stats.getChildren().length; i++)
                {
                    Widget w = stats.getChildren()[i];
                    if(w == null)
                        continue;
                    if(TextUtil.sanitize(w.getText()).contains(finalTitle))
                    {
                        Widget value = stats.getChildren()[i + 1];
                        return Integer.parseInt(TextUtil.sanitize(value.getText()));
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return -1;
            }
            return -1;
        });
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
