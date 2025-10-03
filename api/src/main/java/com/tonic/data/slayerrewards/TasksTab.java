package com.tonic.data.slayerrewards;

import com.tonic.api.widgets.SlayerRewardsAPI;
import com.tonic.api.widgets.WidgetAPI;
import net.runelite.api.gameval.InterfaceID;

public class TasksTab extends AbstractTabImpl
{
    @Override
    public RewardsTab getTab() {
        return RewardsTab.TASKS;
    }

    public boolean cancel()
    {
        if(SlayerRewardsAPI.getPoints() < 30)
            return false;
        confirm(58);
        return true;
    }

    public boolean block()
    {
        if(SlayerRewardsAPI.getPoints() < 40)
            return false;
        confirm(59);
        return true;
    }
}
