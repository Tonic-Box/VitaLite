package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.PlayerQuery;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
import com.tonic.util.ActionMatch;

/**
 * Player API
 */
public class PlayerAPI extends ActorAPI
{
    /**
     * Creates an instance of PlayerQuery
     * @return PlayerQuery
     */
    public static PlayerQuery search()
    {
        return new PlayerQuery();
    }

    /**
     * interact with a player by option number
     * @param player player
     * @param option option number
     */
    public static void interact(PlayerEx player, int option)
    {
        interact(player.getIndex(), option);
    }

    /**
     * interacts with a player by first matching action
     * @param player player
     * @param actions actions list
     */
    public static void interact(PlayerEx player, String... actions)
    {
        String[] playerActions = player.getActions();
        for (String action : actions)
        {
            int index = ActionMatch.indexOf(playerActions, action);
            if (index != -1)
            {
                interact(player, index);
                return;
            }
        }
    }

    /**
     * interact with a player by option number
     * @param index player index
     * @param option option number
     */
    public static void interact(int index, int option)
    {
        TClient client = Static.getClient();
        Static.invoke(() ->
        {
            ClickManager.click(ClickType.ACTOR);
            client.getPacketWriter().playerActionPacket(option, index, false);
        });
    }
}
