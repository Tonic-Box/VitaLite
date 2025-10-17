package com.tonic.api.entities;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.TItemComposition;
import com.tonic.data.TileItemEx;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.PacketInteractionType;
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
     * interact with a tile item without holding down control
     * @param item tile item
     * @param action action
     */
    public static void interact(TileItemEx item, int action)
    {
        interact(item, action, false);
    }

    /**
     * interact with a tile item
     * @param item tile item
     * @param action action
     * @param ctrlDown is control held
     */

    public static void interact(TileItemEx item, int action, boolean ctrlDown)
    {
        if (item == null)
            return;

        interact(action, item.getId(), item.getWorldLocation().getX(), item.getWorldLocation().getY(), ctrlDown);
    }

    /**
     * interact with a tile item without holding down control
     * @param item tile item
     * @param action action
     */
    public static void interact(TileItemEx item, String action)
    {
        interact(item, action, false);
    }

    /**
     * interact with a tile item
     * @param item tile item
     * @param action action
     * @param ctrlDown is control held
     */
    public static void interact(TileItemEx item, String action, boolean ctrlDown)
    {
        if (item == null)
            return;

        int actionIndex = getActionIndex(item, action);

        interact(actionIndex, item.getId(), item.getWorldLocation().getX(), item.getWorldLocation().getY(), ctrlDown);
    }

    /**
     * interact with a tile item without holding down control
     * @param item tile item
     * @param action action
     */
    public static void interact(TileItem item, WorldPoint location, int action)
    {
        interact(item, location, action, false);
    }

    /**
     * interact with a tile item
     * @param item tile item
     * @param location world point
     * @param action action index
     * @param ctrlDown is control held
     */
    public static void interact(TileItem item, WorldPoint location, int action, boolean ctrlDown)
    {
        if (item == null)
            return;

        interact(action, item.getId(), location.getX(), location.getY(), ctrlDown);
    }

    /**
     * interact with a tile item without holding down control
     * @param item tile item
     * @param action action
     */
    public static void interact(TileItem item, WorldPoint location, String action)
    {
        interact(item, location, action, false);
    }

    /**
     * interact with a tile item
     * @param item tile item
     * @param location world point
     * @param action action
     * @param ctrl is control held
     */
    public static void interact(TileItem item, WorldPoint location, String action, boolean ctrl)
    {
        if (item == null)
            return;

        String[] actions = Static.invoke(() -> {
            Client client = Static.getClient();
            TItemComposition itemComp = (TItemComposition) client.getItemDefinition(item.getId());
            return itemComp.getGroundActions();
        });

        int actionIndex = getActionIndex(actions, action);

        interact(actionIndex, item.getId(), location.getX(), location.getY(), ctrl);
    }

    /**
     * interact with a tile item without holding down control
     * @param action action index
     * @param identifier item id
     * @param worldX world point x
     * @param worldY world point y
     */
    public static void interact(int action, int identifier, int worldX, int worldY)
    {
        interact(action, identifier, worldX, worldY, false);
    }

    /**
     * interact with a tile item
     * @param action action index
     * @param identifier item id
     * @param worldX world point x
     * @param worldY world point y
     * @param ctrlDown is control held
     */

    public static void interact(int action, int identifier, int worldX, int worldY, boolean ctrlDown)
    {
        Client client = Static.getClient();
        if(!client.getGameState().equals(GameState.LOGGED_IN))
            return;

        TClient tClient = Static.getClient();
        Static.invoke(() ->
        {
            ClickManager.click(PacketInteractionType.TILEITEM_INTERACT);
            tClient.getPacketWriter().groundItemActionPacket(action, identifier, worldX, worldY, ctrlDown);
        });
    }

    private static int getActionIndex(TileItemEx item, String action)
    {
        if(item == null || item.getActions() == null || action == null)
            return -1;

        return getActionIndex(item.getActions(), action);
    }

    private static int getActionIndex(String[] actions, String action)
    {
        for(int i = 0; i < actions.length; i++)
        {
            if(actions[i] == null)
                continue;

            if(action.equalsIgnoreCase(actions[i]))
                return i;
        }

        return -1;
    }
}
