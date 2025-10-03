package com.tonic.data.slayerrewards;

import com.tonic.api.widgets.SlayerRewardsAPI;
import com.tonic.api.widgets.WidgetAPI;
import net.runelite.api.gameval.InterfaceID;

public abstract class AbstractTabImpl
{
    public abstract RewardsTab getTab();

    public final void open()
    {
        getTab().open();
    }

    protected static void confirm(int index)
    {
        WidgetAPI.interact(1, InterfaceID.SlayerRewards.CONFIRM_BUTTON, index, -1);
    }
}
