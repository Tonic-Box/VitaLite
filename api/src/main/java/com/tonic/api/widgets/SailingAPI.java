package com.tonic.api.widgets;

import com.tonic.api.game.GameAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.data.wrappers.PlayerEx;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

/**
 * Sailing API
 */
public class SailingAPI
{
    /**
     * Sets sails to start navigating
     */
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

    /**
     * Unsets sails to stop navigating
     */
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

    /**
     * Checks if player is currently navigating
     * @return true if navigating, false otherwise
     */
    public boolean isNavigating()
    {
        return VarAPI.getVar(VarbitID.SAILING_BOAT_FACILITY_LOCKEDIN) == 3;
    }

    /**
     * Sets the heading of the boat
     * @param heading Heading value (0-15)
     */
    public void setHeading(Heading heading)
    {
        GameAPI.invokeMenuAction(heading.getValue(), 60, 0, 0, 0, PlayerEx.getLocal().getWorldViewId());
    }

    /**
     * Boat Heading Enum
     */
    @RequiredArgsConstructor
    @Getter
    public enum Heading
    {
        SOUTH(0),
        SOUTH_WEST_SOUTH(1),
        SOUTH_WEST(2),
        SOUTH_WEST_WEST(3),
        WEST(4),
        NORTH_WEST_WEST(5),
        NORTH_WEST(6),
        NORTH_WEST_NORTH(7),
        NORTH(8),
        NORTH_EAST_NORTH(9),
        NORTH_EAST(10),
        NORTH_EAST_EAST(11),
        EAST(12),
        SOUTH_EAST_EAST(13),
        SOUTH_EAST(14),
        SOUTH_EAST_SOUTH(15)
        ;

        private final int value;
    }

    /**
     * Boat Move Mode Enum
     */
    @RequiredArgsConstructor
    public enum MoveMode
    {
        NONE(0),
        NORMAL(2),
        REVERSE(3)

        ;

        private final int value;

        /**
         * Checks if this move mode is currently active
         * @return true if active, false otherwise
         */
        public boolean isActive()
        {
            return VarAPI.getVar(VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE) == value;
        }

        /**
         * Gets the current move mode
         * @return current MoveMode
         */
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

    /**
     * Sailing Side Panel Tabs Enum
     */
    @RequiredArgsConstructor
    public enum Tab
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
}
