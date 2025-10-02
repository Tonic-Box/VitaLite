package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.PlayerQuery;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.PacketInteractionType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import org.apache.commons.lang3.ArrayUtils;

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
     * interactShop with a player by option string
     * @param player player
     * @param option option string
     */
    public static void interact(Player player, String option)
    {
        Client client = Static.getClient();
        String[] actions = Static.invoke(client::getPlayerOptions);
        for(int i = 0; i < actions.length; i++)
        {
            if(actions[i].equalsIgnoreCase(option.toLowerCase()))
            {
                interact(player, i);
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

    public static Player getInCombatWith()
    {
        Client client = Static.getClient();
        return new PlayerQuery()
                .keepIf(n -> n.getInteracting() != null && n.getInteracting().equals(client.getLocalPlayer()))
                .keepIf(n -> !isIdle(n) || n.getHealthRatio() != -1)
                .nearest();
    }
}
