package com.tonic.queries;

import com.tonic.api.game.MovementAPI;
import com.tonic.data.wrappers.ActorEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.abstractions.AbstractActorQuery;
import com.tonic.services.GameManager;
import com.tonic.util.TextUtil;
import com.tonic.util.WorldPointUtil;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * A query class to filter and retrieve NPCs based on various criteria.
 */
public class NpcQuery extends AbstractActorQuery<NpcEx, NpcQuery>
{
    /**
     * Initializes the NpcQuery with the list of all NPCs from the GameManager.
     */
    public NpcQuery()
    {
        super(GameManager.npcList());
    }

    /**
     * Filters NPCs by their IDs.
     *
     * @param ids The IDs to filter by.
     * @return The updated NpcQuery instance.
     */
    public NpcQuery withIds(int... ids)
    {
        return keepIf(n -> {
            int npcId = n.getId();
            for (int id : ids)
            {
                if (npcId == id)
                {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Filters NPCs by their index.
     *
     * @param index The index to filter by.
     * @return NpcQuery
     */
    public NpcQuery withIndex(int index)
    {
        return keepIf(n -> n.getIndex() == index);
    }

    /**
     * Filters NPCs by a specific actions.
     *
     * @param action The action to filter by.
     * @return NpcQuery
     */
    public NpcQuery withAction(String action)
    {
        return keepIf(n -> {
            for (String a : n.getActions())
            {
                if (a != null && a.equalsIgnoreCase(action))
                {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Filters NPCs by their exact name.
     *
     * @param name The name to filter by.
     * @return The updated NpcQuery instance.
     */
    @Override
    public NpcQuery withName(String name)
    {
        return removeIf(o -> !name.equalsIgnoreCase(o.getName()));
    }

    public NpcQuery withNames(String... names)
    {
        return removeIf(o -> {
            for(String name : names)
            {
                if(name.equalsIgnoreCase(o.getName()))
                    return false;
            }
            return true;
        });
    }

    /**
     * Filters NPCs whose names contain the specified substring.
     *
     * @param name The substring to filter by.
     * @return The updated NpcQuery instance.
     */
    @Override
    public NpcQuery withNameContains(String name)
    {
        return removeIf(o -> o.getName() == null || !TextUtil.sanitize(o.getName()).toLowerCase().contains(name.toLowerCase()));
    }

    public NpcQuery alive() {
        return removeIf(ActorEx::isDead);
    }

    public NpcQuery walkable() {
        return removeIf(npc -> {
            WorldArea npcArea = npc.getWorldArea();

            if (npcArea == null) {
                return true;
            }

            int minX = npcArea.getX() - 1;
            int minY = npcArea.getY() - 1;
            int maxX = npcArea.getX() + npcArea.getWidth();
            int maxY = npcArea.getY() + npcArea.getHeight();
            int plane = npcArea.getPlane();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (x >= npcArea.getX() && x < npcArea.getX() + npcArea.getWidth() &&
                    y >= npcArea.getY() && y < npcArea.getY() + npcArea.getHeight()) {
                        continue;
                    }

                    boolean isCorner = (x == minX || x == maxX) && (y == minY || y == maxY);

                    if (isCorner) {
                        continue;
                    }

                    WorldPoint tile = new WorldPoint(x, y, plane);
                    if (MovementAPI.canPathTo(tile)) {
                        return false;
                    }
                }
            }

            return true;
        });
    }

    public NpcQuery withinWorldArea(WorldArea area) {
        return removeIf(npc -> {
            WorldPoint npcWorldPoint = npc.getWorldPoint();

            if (npcWorldPoint == null) {
                return true;
            }

            return !area.contains(npcWorldPoint);
        });
    }
}