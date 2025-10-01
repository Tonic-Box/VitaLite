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
public class PlayerAPI
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

    public static boolean canAttack(NPC npc) {
        if(npc == null || npc.isDead())
            return false;

        return Static.invoke(() -> {
            NPCComposition composition = npc.getComposition();
            if(composition.getConfigs() != null)
                composition = composition.transform();

            if(composition.getActions() == null || !ArrayUtils.contains(composition.getActions(), "Attack"))
                return false;

            if(npc.getHealthRatio() == -1 || npc.getHealthScale() == -1)
                return true;

            Client client = Static.getClient();
            return npc.getInteracting().equals(client.getLocalPlayer());
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

    /**
     * check if a player is idle
     * @param player player
     * @return true if idle
     */
    public static boolean isIdle(Player player)
    {
        return (player.getIdlePoseAnimation() == player.getPoseAnimation() && player.getAnimation() == -1);
    }
}
