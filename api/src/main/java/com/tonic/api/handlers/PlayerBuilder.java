package com.tonic.api.handlers;

import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.queries.PlayerQuery;
import com.tonic.util.handler.AbstractHandlerBuilder;
import net.runelite.api.Player;

public class PlayerBuilder extends AbstractHandlerBuilder
{
    public static PlayerBuilder get()
    {
        return new PlayerBuilder();
    }

    public PlayerBuilder interact(String name, String action)
    {
        add(() -> {
            Player npc = new PlayerQuery().withName(name).first();
            PlayerAPI.interact(npc, action);
        });
        return this;
    }

    public PlayerBuilder follow(String name)
    {
        interact(name, "Follow");
        addDelayUntil(DialogueAPI::dialoguePresent);
        return this;
    }

    public PlayerBuilder attack(String name)
    {
        return interact(name, "Attack");
    }

    public PlayerBuilder trade(String name)
    {
        return interact(name, "Trade");
    }
}
