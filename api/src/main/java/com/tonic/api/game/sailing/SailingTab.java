package com.tonic.api.game.sailing;

import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.WidgetAPI;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

/**
 * Sailing Side Panel Tabs Enum
 */
@RequiredArgsConstructor
public enum SailingTab
{
    FACILITIES(InterfaceID.SailingSidepanel.FACILITIES_TAB, 0),
    STATS(InterfaceID.SailingSidepanel.STATS_TAB, 1),
    CREWMATES(InterfaceID.SailingSidepanel.CREW_TAB, 2)

    ;

    private final int widgetId;
    private final int index;

    /**
     * Opens the tab
     */
    public void open()
    {
        if(!sidePanelVisible())
        {
            WidgetAPI.interact(1, InterfaceID.CombatInterface.SWITCH_BUTTON, -1);
        }
        if(isOpen())
        {
            return;
        }
        WidgetAPI.interact(1, widgetId, -1);
    }

    /**
     * Checks if the tab is currently open
     * @return true if open, false otherwise
     */
    public boolean isOpen()
    {
        return sidePanelVisible() && VarAPI.getVar(VarbitID.SAILING_SIDEPANEL_TABS) == index;
    }

    /**
     * Checks if the sailing side panel is visible
     * @return true if visible, false otherwise
     */
    public static boolean sidePanelVisible()
    {
        return VarAPI.getVar(VarbitID.SAILING_SIDEPANEL_VISIBLE) == 1;
    }
}