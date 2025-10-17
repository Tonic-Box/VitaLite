package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.PacketInteractionType;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.client.game.NPCManager;

/**
 * NPC API
 */
public class NpcAPI extends ActorAPI
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
     * interact with an npc by first matching action
     * @param npc npc
     * @param actions actions list
     */
    public static void interact(NPC npc, String... actions)
    {
        if(npc == null)
            return;
        Static.invoke(() -> {
            NPCComposition composition = npc.getComposition();
            if(composition.getConfigs() != null) {
                composition = composition.transform();
            }

            if(composition == null || composition.getActions() == null)
                return;

            String[] compositionActions = composition.getActions();

            for (String action : actions)
            {
                for(int i = 0; i < compositionActions.length; i++)
                {
                    if(compositionActions[i] != null && compositionActions[i].equalsIgnoreCase(action))
                    {
                        interact(npc, i);
                        return;
                    }
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

    public static int getHealth(NPC npc) {
        return Static.invoke(() -> {
            NPCManager npcManager = Static.getInjector().getInstance(NPCManager.class);
            Integer maxHealthValue = npcManager.getHealth(npc.getId());
            if(maxHealthValue == null)
                return 0;

            int healthRatio = npc.getHealthRatio();
            if(healthRatio <= 0)
                return 0;

            int healthScale = npc.getHealthScale();
            if(healthScale <= 0)
                return 0;

            if(healthScale == 1) {
                return maxHealthValue;
            }

            int minHealth = 1;
            if(healthRatio > 1) {
                minHealth = (maxHealthValue * (healthRatio - 1) + healthScale - 2) / (healthScale - 1);
            }

            int maxHealth = (maxHealthValue * healthRatio - 1) / (healthScale - 1);
            if(maxHealth > maxHealthValue) {
                maxHealth = maxHealthValue;
            }

            return (minHealth + maxHealth + 1) / 2;
        });
    }

    public static String getName(NPC npc)
    {
        return Static.invoke(() -> {
            NPCComposition composition = getComposition(npc);
            if (composition == null || composition.getName() == null)
                return npc.getName();
            return composition.getName();
        });
    }

    public static NPCComposition getComposition(NPC npc)
    {
        return Static.invoke(() -> {
            NPCComposition composition = npc.getComposition();
            if(composition == null)
                return null;
            if(composition.getConfigs() != null)
            {
                composition = composition.transform();
            }
            return composition;
        });
    }
}
