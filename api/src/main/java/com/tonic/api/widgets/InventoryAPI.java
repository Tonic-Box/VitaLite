package com.tonic.api.widgets;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.data.wrappers.*;
import com.tonic.queries.InventoryQuery;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;

import java.util.List;
import java.util.function.Predicate;

/**
 * Inventory automation api
 */
public class InventoryAPI
{
    /**
     * Creates an instance of InventoryQuery from the Inventory
     * @return InventoryQuery
     */
    public static InventoryQuery search() {
        return InventoryQuery.fromInventoryId(InventoryID.INV);
    }
    /**
     * get all items in your inventory
     * @return List<ItemEx>
     */
    public static List<ItemEx> getItems()
    {
        return Static.invoke(() -> InventoryQuery.fromInventoryId(InventoryID.INV).collect());
    }

    /**
     * get an item in your inventory by id
     * @param itemId item id
     * @return ItemEx
     */
    public static ItemEx getItem(int itemId)
    {
        return InventoryQuery.fromInventoryId(InventoryID.INV).withId(itemId).first();
    }

    /**
     * get an item in your inventory by name
     * @param itemName item name
     * @return ItemEx
     */
    public static ItemEx getItem(String itemName)
    {
        return InventoryQuery.fromInventoryId(InventoryID.INV).withNameContains(itemName).first();
    }

    /**
     * get an item in your inventory by predicate
     * @param predicate predicate
     * @return ItemEx
     */
    public static ItemEx getItem(Predicate<ItemEx> predicate)
    {
        return InventoryQuery.fromInventoryId(InventoryID.INV).keepIf(predicate).first();
    }

    /**
     * interact with an item in your inventory by action name
     * @param item item
     * @param action action name
     */
    public static void interact(ItemEx item, String action)
    {
        itemAction(item.getSlot(), item.getId(), getAction(item, action));
    }

    public static void interact(ItemEx item, String... actions)
    {
        itemAction(item.getSlot(), item.getId(), getAction(item, actions));
    }

    /**
     * interact with an item in your inventory by id and action name
     * @param itemId item id
     * @param action action name
     */
    public static void interact(int itemId, String action)
    {
        ItemEx item = getItem(itemId);
        if(item == null)
        {
            Logger.warn("Item not found in inventory: " + itemId);
            return;
        }
        itemAction(item.getSlot(), item.getId(), getAction(item, action));
    }

    /**
     * interact with an item in your inventory by action index
     * @param item item
     * @param action action index
     */
    public static void interact(ItemEx item, int action)
    {
        if(item == null)
            return;
        itemAction(item.getSlot(), item.getId(), action);
    }

    /**
     * interact with an item in your inventory by id and action index
     * @param itemId item id
     * @param action action index
     */
    public static void interact(int itemId, int action) {
        ItemEx item = getItem(itemId);
        if(item != null) {
            itemAction(item.getSlot(), item.getId(), action);
        }
    }

    /**
     * interact with the first item found in your inventory by ids and action index
     * @param itemIds item ids
     * @param action action index
     */
    public static void interact(int[] itemIds, int action) {
        for(int itemId : itemIds)
        {
            ItemEx item = getItem(itemId);
            if(item != null) {
                itemAction(item.getSlot(), item.getId(), action);
                return;
            }
        }
    }

    /**
     * interact with an item in your inventory by name and action index
     * @param itemName item name
     * @param action action index
     */
    public static void interact(String itemName, int action) {
        ItemEx item = getItem(itemName);
        if(item != null) {
            itemAction(item.getSlot(), item.getId(), action);
        }
    }

    /**
     * interact with an item in your inventory by name and action index
     * @param itemName item name
     * @param action action
     */
    public static void interact(String itemName, String action) {
        ItemEx item = getItem(itemName);
        if(item != null) {
            itemAction(item.getSlot(), item.getId(), getAction(item, action));
        }
    }

    /**
     * interact with an item in your inventory by subOp and action
     * @param item item
     * @param menu menu (Eg. "Rub")
     * @param action action (Eg. "Grand Exchange")
     */
    public static void interactSubOp(ItemEx item, String menu, String action) {
        if (item == null)
            return;
        WidgetAPI.interact(item.getWidget(), menu, action);
    }

    /**
     * interact with an item in your inventory by id, subOp and action
     * @param itemId itemId
     * @param menu menu (Eg. "Rub")
     * @param action action (Eg. "Grand Exchange")
     */
    public static void interactSubOp(int itemId, String menu, String action) {
        ItemEx item = getItem(itemId);
        interactSubOp(item, menu, action);
    }

    /**
     * interact with an item in your inventory by name, subOp and action
     * @param itemName itemName
     * @param menu menu (Eg. "Rub")
     * @param action action (Eg. "Grand Exchange")
     */
    public static void interactSubOp(String itemName, String menu, String action) {
        ItemEx item = getItem(itemName);
        interactSubOp(item, menu, action);
    }

    /**
     * interact with an item in your inventory by slot, id and action index
     * @param slot slot
     * @param id id
     * @param action action index
     */
    public static void itemAction(int slot, int id, int action) {
        if(id == 6512 || id == -1)
            return;

        WidgetAPI.interact(action, InterfaceID.Inventory.ITEMS, slot, id);
    }

    /**
     * drag an item in your inventory to another slot
     * @param item item
     * @param toSlot to slot
     */
    public static void dragItem(ItemEx item, int toSlot)
    {
        if(item == null)
            return;

        dragItem(item.getId(), item.getSlot(), toSlot);
    }

    /**
     * drag an item in your inventory to another slot
     * @param id item id
     * @param toSlot to slot
     */
    public static void dragItem(int id, int toSlot)
    {
        ItemEx item = getItem(id);
        if(item == null)
            return;

        dragItem(id, item.getSlot(), toSlot);
    }

    /**
     * drag an item in your inventory to another slot
     * @param itemId item itemId
     * @param fromSlot from slot
     * @param toSlot to slot
     */
    public static void dragItem(int itemId, int fromSlot, int toSlot)
    {
        ItemEx item = search().fromSlot(fromSlot).first();
        if(item == null || item.getId() != itemId)
            return;

        ItemEx item2 = search().fromSlot(toSlot).first();
        int itemId2 = ItemID.BLANKOBJECT;
        if (item2 != null)
            itemId2 = item2.getId();

        WidgetAPI.dragWidget(InterfaceID.Inventory.ITEMS, item.getId(), item.getSlot(), InterfaceID.Inventory.ITEMS, itemId2, toSlot);
    }

    private static int getAction(ItemEx item, String... options)
    {
        for(String option : options)
        {
            int action = getAction(item, option);
            if(action != -1)
            {
                return action;
            }
        }
        return -1;
    }

    /**
     * get the action index for an item action name
     * @param item item
     * @param option action name
     * @return action index
     */
    private static int getAction(ItemEx item, String option)
    {
        option = option.toLowerCase();
        switch (option)
        {
            case "drop":
                return 7;
            case "examine":
                return 10;
            case "wear":
            case "wield":
            case "equip":
                return 3;
            case "rub":
                return 6;
        }
        String[] actions = item.getActions();
        int index = -1;
        for(int i = 0; i < actions.length; i++)
        {
            if(actions[i] != null && actions[i].toLowerCase().contains(option))
            {
                index = i;
                break;
            }
        }
        if(index == -1)
        {
            return -1;
        }
        return (index < 4) ? index + 2 : index + 3;
    }

    /**
     * drop all items from your inventory by list of ids
     * @param ids item ids to drop
     * @return number of ticks it will take
     */
    public static int dropAll(List<Integer> ids)
    {
        int count = 0;
        for(int id : ids)
        {
            count += dropAll(id);
        }
        return (int) Math.ceil((double) count / 10);
    }

    /**
     * drop all items from your inventory by list of ids
     * @param ids item ids to drop
     * @return number of ticks it will take
     */
    public static int dropAll(int... ids)
    {
        int count = 0;
        for(int id : ids)
        {
            count = dropAll(id);
        }
        return (int) Math.ceil((double) count / 10);
    }

    /**
     * drop all items from your inventory by list of ids
     * @param names items to drop
     * @return number of ticks it will take
     */
    public static int dropAll(String... names)
    {
        int count = 0;
        for(String name : names)
        {
            count = dropAll(name);
        }
        return (int) Math.ceil((double) count / 10);
    }

    /**
     * drop all items from your inventory by list of ids
     * @param id item id to drop
     * @return number of ticks it will take
     */
    public static int dropAll(int id)
    {
        ItemContainerEx inventory = new ItemContainerEx(InventoryID.INV);
        int count = 0;
        for(ItemEx item : inventory.getItems())
        {
            if(item.getId() == id)
            {
                count++;
                InventoryAPI.interact(item, 7);
            }
        }
        return (int) Math.ceil((double) count / 10);
    }

    /**
     * drop all items from your inventory by list of ids
     * @param name item to drop
     * @return number of ticks it will take
     */
    public static int dropAll(String name)
    {
        ItemContainerEx inventory = new ItemContainerEx(InventoryID.INV);
        int count = 0;
        for(ItemEx item : inventory.getItems())
        {
            if(item.getName().toLowerCase().contains(name.toLowerCase()))
            {
                count++;
                InventoryAPI.interact(item, 7);
            }
        }
        return (int) Math.ceil((double) count / 10);
    }

    /**
     * check if your inventory is full
     * @return bool
     */
    public static boolean isFull()
    {
        return getEmptySlots() <= 0;
    }

    /**
     * check if your inventory is empty
     * @return bool
     */
    public static boolean isEmpty()
    {
        return getEmptySlots() == 28;
    }

    /**
     * get the number of empty slots in your inventory
     * @return int
     */
    public static int getEmptySlots() {
        ItemContainerEx inventory = new ItemContainerEx(InventoryID.INV);
        return 28 - inventory.getItems().size();
    }

    /**
     * check if your inventory contains all the specified item ids
     * @param itemIds item ids
     * @return bool
     */
    public static boolean contains(int... itemIds)
    {
        ItemContainerEx inventory = new ItemContainerEx(InventoryID.INV);
        for(int itemId : itemIds)
        {
            if(inventory.getFirst(itemId) == null)
                return false;
        }
        return true;
    }

    /**
     * check if your inventory contains any of the specified item ids
     * @param itemIds item ids
     * @return bool
     */
    public static boolean containsAny(int... itemIds)
    {
        ItemContainerEx inventory = new ItemContainerEx(InventoryID.INV);
        for(int itemId : itemIds)
        {
            if(inventory.getFirst(itemId) != null)
                return true;
        }
        return false;
    }

    /**
     * check if your inventory contains all the specified item names
     * @param itemNames item names
     * @return bool
     */
    public static boolean contains(String... itemNames)
    {
        ItemContainerEx inventory = new ItemContainerEx(InventoryID.INV);
        for(String name : itemNames)
        {
            if(inventory.getFirst(name) == null)
                return false;
        }
        return true;
    }

    /**
     * check if your inventory contains any of the specified item names
     * @param itemNames item names
     * @return bool
     */
    public static boolean containsAny(String... itemNames)
    {
        ItemContainerEx inventory = new ItemContainerEx(InventoryID.INV);
        for(String name : itemNames)
        {
            if(inventory.getFirst(name) != null)
                return true;
        }
        return false;
    }

    /**
     * count the total number of items in your inventory by ids
     * @param itemIds item ids
     * @return int
     */
    public static int count(int... itemIds)
    {
        return InventoryQuery.fromInventoryId(InventoryID.INV).withId(itemIds).count();
    }

    /**
     * count the total number of items in your inventory by ids
     * @param itemIds item ids
     * @return int
     */
    public static int canonicalCount(int... itemIds)
    {
        return InventoryQuery.fromInventoryId(InventoryID.INV).withCanonicalId(itemIds).count();
    }

    /**
     * count the total number of items in your inventory by names
     * @param itemNames item names
     * @return int
     */
    public static int count(String... itemNames)
    {
        return InventoryQuery.fromInventoryId(InventoryID.INV).withName(itemNames).count();
    }

    /**
     * use an item on a tile object
     * @param item item
     * @param tileObject tile object
     */
    public static void useOn(ItemEx item, TileObjectEx tileObject)
    {
        if(item == null || tileObject == null)
            return;

        WorldPoint wp = tileObject.getWorldPoint();
        WidgetAPI.onTileObject(InterfaceID.Inventory.ITEMS, item.getId(), item.getSlot(), tileObject.getId(), wp.getX(), wp.getY(), false);
    }

    /**
     * use an item on a ground item
     * @param item item
     * @param tileItem tile item
     */
    public static void useOn(ItemEx item, TileItemEx tileItem)
    {
        if(item == null || tileItem == null)
            return;

        WorldPoint wp = tileItem.getWorldPoint();
        WidgetAPI.onGroundItem(InterfaceID.Inventory.ITEMS, item.getId(), item.getSlot(), tileItem.getId(), wp.getX(), wp.getY(), false);
    }

    /**
     * use an item on a player
     * @param item item
     * @param player player
     */
    public static void useOn(ItemEx item, PlayerEx player)
    {
        if(item == null || player == null)
            return;

        WidgetAPI.onPlayer(InterfaceID.Inventory.ITEMS, item.getId(), item.getSlot(), player.getIndex(), false);
    }

    /**
     * use an item on an npc
     * @param item item
     * @param npc npc
     */
    public static void useOn(ItemEx item, NpcEx npc)
    {
        if(item == null || npc == null)
            return;

        WidgetAPI.onNpc(InterfaceID.Inventory.ITEMS, item.getId(), item.getSlot(), npc.getIndex(), false);
    }

    /**
     * use an item on another item in your inventory
     * @param item item
     * @param target target item
     */
    public static void useOn(ItemEx item, ItemEx target)
    {
        if(item == null || target == null)
            return;

        WidgetAPI.onWidget(InterfaceID.Inventory.ITEMS, item.getId(), item.getSlot(), InterfaceID.Inventory.ITEMS, target.getId(), target.getSlot());
    }

    /**
     * get the total count of an item in your inventory by id
     * @param id item id
     * @return int
     */
    public static int getCount(int id) {
        return getCount(id, true);
    }

    /**
     * get the total count of an item in your inventory by id
     * @param id item id
     * @param canonicalize whether to canonicalize the id (normalize all ids to un-noted version for the count)
     * @return int
     */
    public static int getCount(int id, boolean canonicalize) {
        if(canonicalize)
            return InventoryQuery.fromInventoryId(InventoryID.INV).withCanonicalId(id).count();
        return InventoryQuery.fromInventoryId(InventoryID.INV).withId(id).count();
    }
}
