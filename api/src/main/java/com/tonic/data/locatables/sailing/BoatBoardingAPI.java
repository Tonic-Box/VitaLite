package com.tonic.data.locatables.sailing;

import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.WidgetAPI;
import net.runelite.api.gameval.InterfaceID;

public class BoatBoardingAPI
{
    public static boolean isOpen()
    {
        return WidgetAPI.isVisible(InterfaceID.SailingBoatSelection.UNIVERSE);
    }

    public static boolean boardRecent()
    {
        if(!isOpen())
        {
            return false;
        }
        DialogueAPI.resumePause(InterfaceID.SailingBoatSelection.BOATS_CLICK_LAYER, 2);
        return true;
    }
}
