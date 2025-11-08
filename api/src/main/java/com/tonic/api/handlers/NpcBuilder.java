package com.tonic.api.handlers;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.data.locatables.NpcLocations;
import com.tonic.queries.NpcQuery;
import com.tonic.util.DialogueNode;
import com.tonic.util.handler.AbstractHandlerBuilder;
import com.tonic.util.handler.HandlerBuilder;
import net.runelite.api.NPC;

public class NpcBuilder extends AbstractHandlerBuilder
{
    public static HandlerBuilder get()
    {
        return new HandlerBuilder();
    }

    public NpcBuilder interact(String name, String action)
    {
        add(() -> {
            NPC npc = new NpcQuery().withName(name).first();
            NpcAPI.interact(npc, action);
        });
        return this;
    }

    public NpcBuilder visit(NpcLocations npcLocations, String action)
    {
        walkTo(npcLocations.getLocation());
        interact(npcLocations.getName(), action);
        return this;
    }

    public NpcBuilder talkTo(NpcLocations npcLocations)
    {
        visit(npcLocations, "Talk-to");
        addDelayUntil(DialogueAPI::dialoguePresent);
        return this;
    }

    public NpcBuilder talkTo(NpcLocations npcLocations, DialogueNode dialogueNode)
    {
        talkTo(npcLocations);
        addDelayUntil(() -> !dialogueNode.processStep());
        return this;
    }

    public NpcBuilder talkTo(NpcLocations npcLocations, String... chatOptions)
    {
        DialogueNode dialogueNode = DialogueNode.get(chatOptions);
        return talkTo(npcLocations, dialogueNode);
    }

    public NpcBuilder attack(NpcLocations npcLocations)
    {
        visit(npcLocations, "Attack");
        return this;
    }

    public NpcBuilder trade(NpcLocations npcLocations)
    {
        visit(npcLocations, "Trade");
        return this;
    }
}
