package com.tonic.api.handlers;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.*;
import com.tonic.util.handler.AbstractHandlerBuilder;
import net.runelite.api.NPC;
import net.runelite.api.Player;

/**
 * Builder for creating inventory-related handlers.
 */
public class InventoryBuilder extends AbstractHandlerBuilder<InventoryBuilder>
{
    /**
     * Creates a new InventoryBuilder instance.
     *
     * @return A new InventoryBuilder.
     */
    public static InventoryBuilder get()
    {
        return new InventoryBuilder();
    }

    /**
     * Interacts with an item in the inventory.
     * @param itemId item ID
     * @param action action to perform
     * @return InventoryBuilder instance
     */
    public InventoryBuilder interact(int itemId, String action)
    {
        add(() -> InventoryAPI.interact(itemId, action));
        return this;
    }

    /**
     * Interacts with an item in the inventory.
     * @param itemName item name
     * @param action action to perform
     * @return InventoryBuilder instance
     */
    public InventoryBuilder interact(String itemName, String action)
    {
        add(() -> InventoryAPI.interact(itemName, action));
        return this;
    }

    /**
     * Interacts with a sub-option of an item in the inventory.
     * @param itemId item ID
     * @param menu menu option
     * @param action action to perform
     * @return InventoryBuilder instance
     */
    public InventoryBuilder interact(int itemId, String menu, String action)
    {
        add(() -> InventoryAPI.interactSubOp(itemId, menu, action));
        return this;
    }

    /**
     * Interacts with a sub-option of an item in the inventory.
     * @param itemName item name
     * @param menu menu option
     * @param action action to perform
     * @return InventoryBuilder instance
     */
    public InventoryBuilder interact(String itemName, String menu, String action)
    {
        add(() -> InventoryAPI.interactSubOp(itemName, menu, action));
        return this;
    }

    /**
     * Drops specified items from the inventory.
     * @param itemNames Names of the items to drop.
     * @return InventoryBuilder instance.
     */
    public InventoryBuilder drop(String... itemNames)
    {

        add(context -> {
            int delay = InventoryAPI.dropAll(itemNames);
            context.put("DELAY", delay);
        });
        addDelayUntil(context -> {
            int delay = context.get("DELAY");
            context.put("DELAY", delay - 1);
            return delay <= 0;
        });
        return this;
    }

    /**
     * Uses one item on another in the inventory.
     * @param delay Delay after the action.
     * @param itemName1 Name of the first item.
     * @param itemName2 Name of the second item.
     * @return InventoryBuilder instance.
     */
    public InventoryBuilder useOnItem(int delay, String itemName1, String itemName2)
    {
        ItemEx item1 = InventoryAPI.getItem(itemName1);
        ItemEx item2 = InventoryAPI.getItem(itemName2);
        add(() -> InventoryAPI.useOn(item1, item2));
        addDelayUntil(() -> PlayerEx.getLocal().isIdle());
        if(delay > 0)
        {
            addDelay(delay);
        }
        return this;
    }

    /**
     * Uses an item on a tile item.
     * @param delay Delay after the action.
     * @param itemName Name of the item in inventory.
     * @param tileItemName Name of the tile item.
     * @return InventoryBuilder instance.
     */
    public InventoryBuilder useOnTileItem(int delay, String itemName, String tileItemName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        TileItemEx tileItem = TileItemAPI.search().withName(tileItemName).first();
        add(() -> InventoryAPI.useOn(item, tileItem));
        addDelayUntil(() -> PlayerEx.getLocal().isIdle());
        if(delay > 0)
        {
            addDelay(delay);
        }
        return this;
    }

    /**
     * Uses an item on a tile object.
     * @param delay Delay after the action.
     * @param itemName Name of the item in inventory.
     * @param objectName Name of the tile object.
     * @return InventoryBuilder instance.
     */
    public InventoryBuilder useOnObject(int delay, String itemName, String objectName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        TileObjectEx object = TileObjectAPI.get(objectName);
        add(() -> InventoryAPI.useOn(item, object));
        addDelayUntil(() -> PlayerEx.getLocal().isIdle());
        if(delay > 0)
        {
            addDelay(delay);
        }
        return this;
    }

    /**
     * Uses an item on an NPC.
     * @param delay Delay after the action.
     * @param itemName Name of the item in inventory.
     * @param npcName Name of the NPC.
     * @return InventoryBuilder instance.
     */
    public InventoryBuilder useOnNpc(int delay, String itemName, String npcName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        NpcEx npc = NpcAPI.search().withName(npcName).first();
        add(() -> InventoryAPI.useOn(item, npc));
        addDelayUntil(() -> PlayerEx.getLocal().isIdle());
        if(delay > 0)
        {
            addDelay(delay);
        }
        return this;
    }

    /**
     * Uses an item on a player.
     * @param delay Delay after the action.
     * @param itemName Name of the item in inventory.
     * @param playerName Name of the player.
     * @return InventoryBuilder instance.
     */
    public InventoryBuilder useOnPlayer(int delay, String itemName, String playerName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        PlayerEx player = PlayerAPI.search().withName(playerName).first();
        add(() -> InventoryAPI.useOn(item, player));
        addDelayUntil(() -> PlayerEx.getLocal().isIdle());
        if(delay > 0)
        {
            addDelay(delay);
        }
        return this;
    }
}
