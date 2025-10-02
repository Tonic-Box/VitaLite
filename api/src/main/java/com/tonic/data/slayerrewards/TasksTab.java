package com.tonic.data.slayerrewards;

import com.tonic.api.game.GameAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.SlayerRewardsAPI;
import com.tonic.services.ClickManager;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.InterfaceID;

public class TasksTab extends AbstractTabImpl
{
    @Override
    public RewardsTab getTab() {
        return RewardsTab.TASKS;
    }

    public boolean cancel()
    {
        open();

        if(SlayerRewardsAPI.getPoints() < 30)
            return false;

        clickButton(0);
        confirm();
        return true;
    }

    public boolean block()
    {
        open();
        if(SlayerRewardsAPI.getPoints() < 40)
            return false;

        clickButton(2);
        confirm();
        return true;
    }

    private void clickButton(int index)
    {
        ClickManager.click();
        GameAPI.invokeMenuAction(
                1,
                MenuAction.CC_OP.getId(),
                index,
                InterfaceID.SlayerRewards.TASKS,
                -1
        );
        Delays.tick();
    }

    private void confirm()
    {
        ClickManager.click();
        GameAPI.invokeMenuAction(
                1,
                MenuAction.CC_OP.getId(),
                58,
                InterfaceID.SlayerRewards.CONFIRM_BUTTON,
                -1
        );
        Delays.tick();
    }
}
