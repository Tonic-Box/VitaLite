package com.tonic.queries;

import com.tonic.queries.abstractions.AbstractActorQuery;
import com.tonic.services.GameManager;
import net.runelite.api.Player;

/**
 * A query class for filtering and retrieving Player entities in the game.
 */
public class PlayerQuery extends AbstractActorQuery<Player, PlayerQuery>
{
    /**
     * Constructs a new PlayerQuery instance.
     * Initializes the query with the list of all players from the GameManager.
     */
    public PlayerQuery() {
        super(GameManager.playerList());
    }
}
