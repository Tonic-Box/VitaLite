package com.tonic.queries;

import com.tonic.queries.abstractions.AbstractActorQuery;
import com.tonic.services.GameManager;
import net.runelite.api.Player;

public class PlayerQuery extends AbstractActorQuery<Player, PlayerQuery>
{
    public PlayerQuery() {
        super(GameManager.playerList());
    }
}
