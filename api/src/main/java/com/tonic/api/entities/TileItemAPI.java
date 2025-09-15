package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.data.TileItemEx;
import com.tonic.services.ClickManager;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;

/**
 * TileItem API
 */
public class TileItemAPI
{
    /**
     * interact with a tile item
     * @param item tile item
     * @param action action
     */
    public static void interact(TileItemEx item, int action)
    {
        if (item == null)
            return;

        interact(action, item.getId(), item.getWorldLocation().getX(), item.getWorldLocation().getY());
    }

    /**
     * interact with a tile item
     * @param item tile item
     * @param action action
     */
    public static void interact(TileItem item, WorldPoint location, int action)
    {
        if (item == null)
            return;

        interact(action, item.getId(), location.getX(), location.getY());
    }

    /**
     * interact with a tile item
     * @param action action
     */
    public static void interact(int action, int identifier, int worldX, int worldY)
    {
        Client client = Static.getClient();
        if(!client.getGameState().equals(GameState.LOGGED_IN))
            return;

        TClient tClient = Static.getClient();
        Static.invoke(() ->
        {
            ClickManager.click();
            tClient.getPacketWriter().groundItemActionPacket(action, identifier, worldX, worldY, false);
        });
    }
}
