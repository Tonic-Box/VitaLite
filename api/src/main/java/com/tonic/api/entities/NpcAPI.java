package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.PacketInteractionType;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

/**
 * NPC API
 */
public class NpcAPI
{
    /**
     * interact with an npc by int option
     * @param npc npc
     * @param option option
     */
    public static void interact(NPC npc, int option)
    {
        if (npc == null)
            return;

        interact(npc.getIndex(), option);
    }

    /**
     * interactShop with an npc by option string
     * @param npc npc
     * @param option option string
     */
    public static void interact(NPC npc, String option)
    {
        if(npc == null)
            return;
        Static.invoke(() -> {
            NPCComposition composition;
            if(npc.getComposition().getConfigs() != null)
            {
                composition = npc.getComposition().transform();
            }
            else
            {
                composition = npc.getComposition();
            }

            if(composition == null || composition.getActions() == null)
                return;
            String[] actions = composition.getActions();
            for(int i = 0; i < actions.length; i++)
            {
                if(actions[i] != null && actions[i].equalsIgnoreCase(option))
                {
                    interact(npc, i);
                    return;
                }
            }
        });
    }

    /**
     * interact with an npc by its index
     * @param npcIndex npc index
     * @param option option
     */
    public static void interact(int npcIndex, int option)
    {
        TClient client = Static.getClient();
        Static.invoke(() ->
        {
            ClickManager.click(PacketInteractionType.NPC_INTERACT);
            client.getPacketWriter().npcActionPacket(option, npcIndex, false);
        });
    }
}
