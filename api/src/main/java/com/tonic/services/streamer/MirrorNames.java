package com.tonic.services.streamer;

import com.tonic.Static;
import com.tonic.api.TClient;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.BeforeRender;
import net.runelite.client.eventbus.EventBus;

import java.util.Objects;

/**
 * Keeps StreamerName told of the player's names while streamer mode is on: the logged-in player's name, kept after logging out, the Jagex character name the login screen shows, which switching accounts changes, the launcher's display name and the name typed into the login screen. Refreshed on the client thread before each frame.
 */
final class MirrorNames
{
    private static EventBus.Subscriber rendering;
    private static String player;
    private static boolean known;
    private static String lastUsername;
    private static String lastPlayer;
    private static String lastLoginName;
    private static String lastLauncherName;

    private MirrorNames()
    {
    }

    /**
     * Starts refreshing the player's names, as streamer mode starts.
     */
    static void start()
    {
        known = false;
        rendering = Static.getInjector().getInstance(EventBus.class).register(BeforeRender.class, event -> refresh(), 0);
    }

    /**
     * Stops refreshing the player's names, as streamer mode stops.
     */
    static void stop()
    {
        if (rendering != null)
        {
            Static.getInjector().getInstance(EventBus.class).unregister(rendering);
            rendering = null;
        }
    }

    private static void refresh()
    {
        Client client = Static.getClient();
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            Player local = client.getLocalPlayer();
            if (local != null && local.getName() != null)
            {
                player = local.getName();
            }
        }
        String username = client.getUsername();
        String loginName = ((TClient) client).getLoginDisplayName();
        String launcherName = client.getLauncherDisplayName();
        if (known && Objects.equals(username, lastUsername) && Objects.equals(player, lastPlayer) && Objects.equals(loginName, lastLoginName) && Objects.equals(launcherName, lastLauncherName))
        {
            return;
        }
        known = true;
        lastUsername = username;
        lastPlayer = player;
        lastLoginName = loginName;
        lastLauncherName = launcherName;
        StreamerName.setNames(username, player, loginName, launcherName);
    }
}
