package com.tonic.queries;

import com.tonic.queries.abstractions.AbstractActorQuery;
import com.tonic.services.GameManager;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

public class NpcQuery extends AbstractActorQuery<NPC, NpcQuery>
{
    public NpcQuery()
    {
        super(GameManager.npcList());
    }

    public NpcQuery withIds(int... ids)
    {
        return keepIf(n -> {
            for (int id : ids)
            {
                if (n.getId() == id)
                {
                    return true;
                }
            }
            return false;
        });
    }

    public NpcQuery withAction(String action)
    {
        return keepIf(n -> {
            NPCComposition composition = n.getComposition();
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
}