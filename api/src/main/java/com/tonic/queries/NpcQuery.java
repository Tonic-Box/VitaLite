package com.tonic.queries;

import com.tonic.queries.abstractions.AbstractActorQuery;
import com.tonic.services.GameManager;
import com.tonic.util.TextUtil;
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

    @Override
    public NpcQuery withName(String name)
    {
        return removeIf(o -> {
            NPCComposition composition = getComposition(o);
            return !name.equalsIgnoreCase(TextUtil.sanitize(composition.getName()));
        });
    }

    @Override
    public NpcQuery withNameContains(String name)
    {
        return removeIf(o -> {
            NPCComposition composition = getComposition(o);
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