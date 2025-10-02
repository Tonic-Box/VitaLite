package com.tonic.api.game;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.PacketInteractionType;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

public class GameAPI
{
    public static void logout()
    {
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

    public static boolean isLoggedIn()
    {
        Client client = Static.getClient();
        return client.getGameState() == GameState.LOGGED_IN || client.getGameState() == GameState.LOADING;
    }

    public static boolean isOnLoginScreen()
    {
        Client client = Static.getClient();
        return client.getGameState() == GameState.LOGIN_SCREEN
                || client.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR
                || client.getGameState() == GameState.LOGGING_IN;
    }

    public static int getRealSkillLevel(Skill skill)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getRealSkillLevel(skill));
    }

    public static int getBoostedSkillLevel(Skill skill)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getBoostedSkillLevel(skill));
    }

    public static int getSkillExperience(Skill skill)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getSkillExperience(skill));
    }

    public static int getMembershipDays()
    {
        return VarAPI.getVarp(VarPlayerID.ACCOUNT_CREDIT);
    }

    public static boolean isInCutscene()
    {
        return VarAPI.getVar(VarbitID.CUTSCENE_STATUS) > 0;
    }

    public static void invokeMenuAction(int identifier, int opcode, int param0, int param1, int itemId)
    {
        TClient client = Static.getClient();
        boolean lock = Static.invoke(() -> {
            ClickManager.click(PacketInteractionType.UNBOUND_INTERACT);
            client.invokeMenuAction("", "", identifier, opcode, param0, param1, itemId, -1, -1);
            return true;
        });
    }
}
