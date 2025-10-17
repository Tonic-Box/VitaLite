package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.PacketInteractionType;
import net.runelite.api.Client;
import net.runelite.api.Player;

/**
 * Player API
 */
public class PlayerAPI extends ActorAPI
{
    /**
     * interact with a player by option number
     * @param player player
     * @param option option number
     */
    public static void interact(Player player, int option)
    {
        interact(player.getId(), option);
    }

    /**
     * interacts with a player by first matching action
     * @param player player
     * @param actions actions list
     */
    public static void interact(Player player, String... actions)
    {
        Client client = Static.getClient();
        String[] playerActions = Static.invoke(client::getPlayerOptions);
        for (String action : actions)
        {
            for(int i = 0; i < playerActions.length; i++)
            {
                if(playerActions[i].equalsIgnoreCase(action.toLowerCase()))
                {
                    interact(player, i);
                    return;
                }
            }
        }
    }

    /**
     * interact with a player by option number
     * @param index player index
     * @param option option number
     */
    public static void interact(int index, int option)
    {
        TClient client = Static.getClient();
        Static.invoke(() ->
        {
            ClickManager.click(PacketInteractionType.PLAYER_INTERACT);
            client.getPacketWriter().playerActionPacket(option, index, false);
        });
    }

    /**
     * @return The current player
     */
    public static Player getLocal()
    {
        Client client = Static.getClient();
        return client.getLocalPlayer();
    }
}
