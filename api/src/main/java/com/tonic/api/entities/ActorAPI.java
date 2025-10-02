package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.PlayerQuery;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.ArrayUtils;

public class ActorAPI
{
    public static boolean canAttack(Actor actor) {
        if(actor == null || actor.isDead())
            return false;

        return Static.invoke(() -> {
            if(actor instanceof NPC)
            {
                NPC npc = (NPC) actor;
                NPCComposition composition = npc.getComposition();
                if (composition == null)
                    return false;
                if(composition.getConfigs() != null)
                    composition = composition.transform();

                if(composition.getActions() == null || !ArrayUtils.contains(composition.getActions(), "Attack"))
                    return false;

                if(composition.getName() == null)
                    return false;
            }

            if(CombatAPI.inMultiWay())
                return true;

            if(actor.getHealthRatio() == -1 || actor.getHealthScale() == -1)
                return true;

            Client client = Static.getClient();
            return actor.getInteracting() == null || actor.getInteracting().equals(client.getLocalPlayer());
        });
    }

    /**
     * check if a player is idle
     * @param actor player
     * @return true if idle
     */
    public static boolean isIdle(Actor actor)
    {
        return (actor.getIdlePoseAnimation() == actor.getPoseAnimation() && actor.getAnimation() == -1);
    }

    public static Actor getInCombatWith()
    {
        Client client = Static.getClient();
        Actor actor = new NpcQuery()
                .keepIf(n -> n.getInteracting() != null && n.getInteracting().equals(client.getLocalPlayer()))
                .keepIf(n -> !isIdle(n) || n.getHealthRatio() != -1)
                .nearest();

        if(actor == null)
        {
            actor = new PlayerQuery()
                    .keepIf(n -> n.getInteracting() != null && n.getInteracting().equals(client.getLocalPlayer()))
                    .keepIf(n -> !isIdle(n) || n.getHealthRatio() != -1)
                    .nearest();
        }
        return actor;
    }
}
