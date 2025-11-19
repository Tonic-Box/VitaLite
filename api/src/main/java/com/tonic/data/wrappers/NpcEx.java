package com.tonic.data.wrappers;

import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.util.Location;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.NPCManager;

public class NpcEx extends ActorEx<NPC>
{
    public NpcEx(NPC actor) {
        super(actor);
    }

    public NPC getNpc() {
        return actor;
    }

    public NPCComposition getComposition()
    {
        NPCComposition composition = actor.getComposition();
        if(composition == null)
            return null;
        if(composition.getConfigs() != null)
        {
            composition = composition.transform();
        }
        return composition;
    }

    public int getId() {
        return Static.invoke(() -> getComposition().getId());
    }

    public int getHealth() {
        return Static.invoke(() -> {
            NPCManager npcManager = Static.getInjector().getInstance(NPCManager.class);
            Integer maxHealthValue = npcManager.getHealth(actor.getId());
            if(maxHealthValue == null)
                return 0;

            int healthRatio = actor.getHealthRatio();
            if(healthRatio <= 0)
                return 0;

            int healthScale = actor.getHealthScale();
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

    @Override
    public WorldPoint getWorldPoint() {
        return Static.invoke(actor::getWorldLocation);
    }

    @Override
    public WorldArea getWorldArea() {
        return Static.invoke(actor::getWorldArea);
    }

    @Override
    public LocalPoint getLocalPoint() {
        return Static.invoke(actor::getLocalLocation);
    }

    @Override
    public Tile getTile() {
        return SceneAPI.getTile(getWorldPoint());
    }

    @Override
    public void interact(String... actions) {
        NpcAPI.interact(this, actions);
    }

    @Override
    public void interact(int action) {
        NpcAPI.interact(this, action);
    }

    @Override
    public String[] getActions() {
        return Static.invoke(() -> getComposition().getActions());
    }
}
