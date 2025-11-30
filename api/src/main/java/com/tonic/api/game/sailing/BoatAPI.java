package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.queries.WidgetQuery;
import com.tonic.util.TextUtil;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

public class BoatAPI
{
    // === Boat Resistances ===
    public static boolean isRapidResistant()
    {
        //TileType.F_TEMPOR_STORM_WATER
        return readRes("Rapid") > 0;
    }

    public static boolean isStormResistant()
    {
        //TileType.F_TEMPOR_STORM_WATER
        return readRes("Storm") > 0;
    }

    public static boolean isFetidWaterResistant()
    {
        return readRes("Fetid water") > 0;
    }

    public static boolean isCrystalFleckedResistant()
    {
        return readRes("Crystal") > 0;
    }

    public static boolean isTangledKelpResistant()
    {
        return readRes("Tangled kelp") > 0;
    }

    public static boolean isIceResistant()
    {
        return readRes("Ice") > 0;
    }

    private static int readRes(String title)
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
                    Widget row = stats.getChild(i);
                    if(row == null)
                        continue;
                    if(finalTitle.contains(TextUtil.sanitize(row.getText())))
                    {
                        Widget value = stats.getChild(i + 1);
                        if(value == null)
                            return -1;
                        return Integer.parseInt(value.getText().trim());
                    }
                }
            }
            catch (Exception e)
            {
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
