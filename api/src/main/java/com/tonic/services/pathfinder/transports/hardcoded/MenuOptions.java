package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.Static;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.util.TextUtil;
import com.tonic.util.handler.HandlerBuilder;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Picks a destination from the travel menu that spirit trees and minecart conductors open.
 */
final class MenuOptions
{
    static final int MENU_TIMEOUT = 20;

    private MenuOptions()
    {
    }

    static int resumeMenu(String destination, int next)
    {
        int slot = menuSlot(destination);
        if (slot == -1)
        {
            return HandlerBuilder.END_EXECUTION;
        }
        DialogueAPI.resumePause(InterfaceID.Menu.LJ_LAYER1, slot);
        return next;
    }

    static int menuSlot(String destination)
    {
        String wanted = destination.toLowerCase().replace('_', ' ');
        return Static.invoke(() ->
        {
            Widget menu = WidgetAPI.get(InterfaceID.Menu.LJ_LAYER1);
            Widget[] lines = menu == null ? null : menu.getChildren();
            if (lines == null)
            {
                return -1;
            }
            for (Widget line : lines)
            {
                if (line != null && line.getText() != null && TextUtil.sanitize(line.getText()).toLowerCase().contains(wanted))
                {
                    return line.getIndex();
                }
            }
            return -1;
        });
    }
}
