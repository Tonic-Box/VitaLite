package com.tonic.services.pathfinder.transports;

import com.tonic.api.widgets.WidgetAPI;
import net.runelite.api.gameval.InterfaceID;

/**
 * The server's "are you sure" warnings a crossing can raise: the cws_warning screens, answered with their "don't ask again" toggle, and the Wilderness ditch's warning, which has none and is answered "Enter Wilderness".
 */
public final class CrossingWarnings
{
    private static final int[] TOGGLES = {
            InterfaceID.CwsWarning1.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning2.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning3.CWS_TOGGLE_BUTTON,
            InterfaceID.CwsWarning4.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning5.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning6.CWS_TOGGLE_BUTTON,
            InterfaceID.CwsWarning8.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning9.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning10.CWS_TOGGLE_BUTTON,
            InterfaceID.CwsWarning11.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning12.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning13.CWS_TOGGLE_BUTTON,
            InterfaceID.CwsWarning14.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning15.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning16.CWS_TOGGLE_BUTTON,
            InterfaceID.CwsWarning17.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning18.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning19.CWS_TOGGLE_BUTTON,
            InterfaceID.CwsWarning20.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning21.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning22.CWS_TOGGLE_BUTTON,
            InterfaceID.CwsWarning23.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning24.CWS_TOGGLE_BUTTON, InterfaceID.CwsWarning25.CWS_TOGGLE_BUTTON
    };

    private CrossingWarnings()
    {
    }

    /**
     * Clicks the "don't ask again" toggle of the warning on screen, if one is, so clicking the crossing again goes through.
     *
     * @return whether a toggle was clicked
     */
    public static boolean dontAskAgain()
    {
        for (int toggle : TOGGLES)
        {
            if (WidgetAPI.isVisible(toggle))
            {
                WidgetAPI.interact(1, toggle, -1);
                return true;
            }
        }
        return false;
    }

    /**
     * Clicks "Enter Wilderness" on the ditch's warning, if it is on screen; the server then carries the crossing out.
     *
     * @return whether it was clicked
     */
    public static boolean enterTheWilderness()
    {
        if (!wildernessWarning())
        {
            return false;
        }
        WidgetAPI.interact(1, InterfaceID.WildernessWarningscreen.YES, -1);
        return true;
    }

    /**
     * @return whether the ditch's warning is on screen
     */
    public static boolean wildernessWarning()
    {
        return WidgetAPI.isVisible(InterfaceID.WildernessWarningscreen.YES);
    }
}
