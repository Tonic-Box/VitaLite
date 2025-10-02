package com.tonic.queries;

import com.tonic.queries.abstractions.AbstractActorQuery;
import com.tonic.services.GameManager;
import com.tonic.util.TextUtil;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

/**
 * A query class to filter and retrieve NPCs based on various criteria.
 */
public class NpcQuery extends AbstractActorQuery<NPC, NpcQuery>
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
            NPCComposition composition = getComposition(n);
            if (composition == null)
                return false;
            for (int id : ids)
            {
                if (composition.getId() == id)
                {
                    return true;
                }
            }
            return false;
        });
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
            NPCComposition composition = getComposition(n);
            if (composition == null || composition.getActions() == null)
                return false;
            for (String a : composition.getActions())
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
        return removeIf(o -> {
            NPCComposition composition = getComposition(o);
            if (composition == null || composition.getName() == null)
                return true;
            return !name.equalsIgnoreCase(TextUtil.sanitize(composition.getName()));
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
        return removeIf(o -> {
            NPCComposition composition = getComposition(o);
            if (composition == null || composition.getName() == null)
                return true;
            return composition.getName() == null ||  !TextUtil.sanitize(composition.getName()).toLowerCase().contains(name.toLowerCase());
        });
    }

    private NPCComposition getComposition(NPC npc)
    {
        NPCComposition composition = npc.getComposition();
        if(composition == null)
            return null;
        if(composition.getConfigs() != null)
        {
            composition = composition.transform();
        }
        return composition;
    }
}