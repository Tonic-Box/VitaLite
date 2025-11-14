package com.tonic.api.handlers;

import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.PlayerQuery;
import com.tonic.util.handler.AbstractHandlerBuilder;
import net.runelite.api.Player;

/**
 * Builder for handling player interactions.
 */
public class PlayerBuilder extends AbstractHandlerBuilder<PlayerBuilder>
{
    /**
     * Creates a new PlayerBuilder instance.
     *
     * @return A new PlayerBuilder.
     */
    public static PlayerBuilder get()
    {
        return new PlayerBuilder();
    }

    /**
     * Interacts with a player by name and action.
     *
     * @param name   The name of the player.
     * @param action The action to perform.
     * @return The current PlayerBuilder instance.
     */
    public PlayerBuilder interact(String name, String action)
    {
        add(() -> {
            PlayerEx npc = new PlayerQuery().withName(name).first();
            PlayerAPI.interact(npc, action);
        });
        return this;
    }

    /**
     * Follows a player by name.
     *
     * @param name The name of the player to follow.
     * @return The current PlayerBuilder instance.
     */
    public PlayerBuilder follow(String name)
    {
        interact(name, "Follow");
        addDelayUntil(DialogueAPI::dialoguePresent);
        return this;
    }

    /**
     * Attacks a player by name.
     *
     * @param name The name of the player to attack.
     * @return The current PlayerBuilder instance.
     */
    public PlayerBuilder attack(String name)
    {
        return interact(name, "Attack");
    }

    /**
     * Trades with a player by name.
     *
     * @param name The name of the player to trade with.
     * @return The current PlayerBuilder instance.
     */
    public PlayerBuilder trade(String name)
    {
        return interact(name, "Trade");
    }
}
