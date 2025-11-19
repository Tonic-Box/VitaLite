package com.tonic.api.game;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/**
 * Game API methods
 */
public class GameAPI
{
    /**
     * Logs out of the game
     */
    public static void logout()
    {
        Client client = Static.getClient();
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        Widget logoutButton = WidgetAPI.get(InterfaceID.Logout.LOGOUT);
        if (logoutButton != null)
        {
            WidgetAPI.interact(1, InterfaceID.Logout.LOGOUT, -1, -1);
        }

        logoutButton = WidgetAPI.get(InterfaceID.Worldswitcher.LOGOUT);
        if (logoutButton != null)
        {
            WidgetAPI.interact(1, InterfaceID.Worldswitcher.LOGOUT, -1, -1);
        }
    }

    /**
     * Gets the current wilderness level, or 0 if not in the wilderness
     * @return the wilderness level
     */
    public static int getWildyLevel()
    {
        Widget wildyLevelWidget = WidgetAPI.get(InterfaceID.PvpIcons.WILDERNESSLEVEL);
        if (!WidgetAPI.isVisible(wildyLevelWidget))
        {
            return 0;
        }

        // Dmm
        if (wildyLevelWidget.getText().contains("Guarded") || wildyLevelWidget.getText().contains("Protection"))
        {
            return 0;
        }

        if (wildyLevelWidget.getText().contains("Deadman"))
        {
            return Integer.MAX_VALUE;
        }
        String widgetText = wildyLevelWidget.getText();
        if (widgetText.isEmpty())
        {
            return 0;
        }
        if (widgetText.equals("Level: --"))
        {
            Client client = Static.getClient();
            Player local = client.getLocalPlayer();
            WorldView worldView = client.getTopLevelWorldView();
            int y = WorldPoint.fromLocal(worldView,
                    local.getLocalLocation().getX(),
                    local.getLocalLocation().getY(),
                    worldView.getPlane()).getY();
            return 2 + (y - 3528) / 8;
        }
        String levelText = widgetText.contains("<br>") ? widgetText.substring(0, widgetText.indexOf("<br>")) : widgetText;
        return Integer.parseInt(levelText.replace("Level: ", ""));
    }

    /**
     * Checks if the player is logged in
     * @return true if logged in
     */
    public static boolean isLoggedIn()
    {
        Client client = Static.getClient();
        return client.getGameState() == GameState.LOGGED_IN || client.getGameState() == GameState.LOADING;
    }

    /**
     * Checks if the player is on the login screen
     * @return true if on the login screen
     */
    public static boolean isOnLoginScreen()
    {
        Client client = Static.getClient();
        return client.getGameState() == GameState.LOGIN_SCREEN
                || client.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR
                || client.getGameState() == GameState.LOGGING_IN;
    }

    /**
     * Note: The character summary subtab must be opened for this to update otherwise it returns a cached value.
     *       For accounts that have never opened it, the value will be {@code -1}.
     *       If the tab is already open, it won't update. You have to open another subtab and then reopen it.
     * @return The accounts playtime in minutes
     */
    public static int getPlaytimeInMinutes()
    {
        return VarAPI.getVarcInteger(VarClientID.ACCOUNT_SUMMARY_PLAYTIME);
    }

    /**
     * Gets the number of membership days remaining
     * @return the number of membership days remaining
     */
    public static int getMembershipDays()
    {
        return VarAPI.getVarp(VarPlayerID.ACCOUNT_CREDIT);
    }

    /**
     * Checks if the player is in a cutscene
     * @return true if in a cutscene
     */
    public static boolean isInCutscene()
    {
        return VarAPI.getVar(VarbitID.CUTSCENE_STATUS) > 0;
    }

    /**
     * @return true if the game screen is currently fading
     */
    public static boolean isScreenFading()
    {
        return WidgetAPI.isVisible(InterfaceID.FadeOverlay.FADER);
    }

    /**
     * Invokes a menu action
     * @param identifier identifier
     * @param opcode opcode
     * @param param0 param0
     * @param param1 param1
     * @param itemId itemId
     */
    public static void invokeMenuAction(int identifier, int opcode, int param0, int param1, int itemId)
    {
        TClient client = Static.getClient();
        boolean lock = Static.invoke(() -> {
            ClickManager.click(ClickType.GENERIC);
            client.invokeMenuAction("", "", identifier, opcode, param0, param1, itemId, -1, -1);
            return true;
        });
    }

    /**
     * Invokes a menu action
     * @param identifier identifier
     * @param opcode opcode
     * @param param0 param0
     * @param param1 param1
     * @param itemId itemId
     */
    public static void invokeMenuAction(int identifier, int opcode, int param0, int param1, int itemId, int worldViewId)
    {
        TClient client = Static.getClient();
        boolean lock = Static.invoke(() -> {
            ClickManager.click(ClickType.GENERIC);
            client.invokeMenuAction("", "", identifier, opcode, param0, param1, itemId, worldViewId, -1, -1);
            return true;
        });
    }
}
