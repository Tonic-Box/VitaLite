package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.game.CombatAPI;
import com.tonic.data.wrappers.ActorEx;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.PlayerQuery;
import net.runelite.api.*;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Actor API
 */
public class ActorAPI
{
    /**
     * find the actor currently in combat with the local player
     * @return the actor, or null if none found
     */
    @Deprecated
    public static ActorEx<?> getInCombatWith()
    {
        Client client = Static.getClient();
        return ActorEx.fromActor(client.getLocalPlayer()).getInCombatWith();
    }
}
