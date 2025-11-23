package com.tonic.api.game.sailing;

import com.tonic.api.game.VarAPI;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.VarbitID;

/**
 * Boat Move Mode Enum
 */
@RequiredArgsConstructor
public enum MoveMode
{
    STILL(0),
    ON_BOAT(1),
    FORWARD(2),
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