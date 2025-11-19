package com.tonic.api.widgets;

import com.tonic.api.game.VarAPI;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

public class SailingAPI
{
    public void setSails()
    {
        if(!isNavigating())
        {
            return;
        }
        Tab.FACILITIES.open();
        if(MoveMode.getCurrent() != MoveMode.NONE)
            return;

        WidgetAPI.interact(1, InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER, -1);
    }

    public void unSetSails()
    {
        if(!isNavigating())
        {
            return;
        }
        Tab.FACILITIES.open();
        if(MoveMode.getCurrent() == MoveMode.NONE)
            return;

        WidgetAPI.interact(1, InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER, -1);
    }

    public boolean isNavigating()
    {
        return VarAPI.getVar(VarbitID.SAILING_BOAT_FACILITY_LOCKEDIN) == 3;
    }

    @RequiredArgsConstructor
    public enum MoveMode
    {
        NONE(0),
        NORMAL(2),
        REVERSE(3)

        ;

        private final int value;

        public boolean isActive()
        {
            return VarAPI.getVar(VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE) == value;
        }

        public static MoveMode getCurrent()
        {
            int var = VarAPI.getVar(VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE);
            for(MoveMode mode : values())
            {
                if(mode.value == var)
                {
                    return mode;
                }
            }
            return null;
        }
    }

    @RequiredArgsConstructor
    public enum Tab
    {
        FACILITIES(InterfaceID.SailingSidepanel.FACILITIES_TAB, 0),
        STATS(InterfaceID.SailingSidepanel.STATS_TAB, 1),
        CREWMATES(InterfaceID.SailingSidepanel.CREW_TAB, 2)

        ;

        private final int widgetId;
        private final int index;

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

        public boolean isOpen()
        {
            return sidePanelVisible() && VarAPI.getVar(VarbitID.SAILING_SIDEPANEL_TABS) == index;
        }

        public static boolean sidePanelVisible()
        {
            return VarAPI.getVar(VarbitID.SAILING_SIDEPANEL_VISIBLE) == 1;
        }
    }
}
