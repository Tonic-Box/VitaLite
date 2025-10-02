package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.queries.InventoryQuery;
import com.tonic.data.EquipmentSlot;
import com.tonic.data.ItemEx;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.PacketInteractionType;
import net.runelite.api.gameval.InventoryID;
import java.util.List;
import java.util.function.Predicate;

/**
 * Equipment automation api
 */
public class EquipmentAPI
{
    /**
     * check if an item is equipped
     * @param itemId item id
     * @return bool
     */
    public static boolean isEquipped(int itemId)
    {
        return Static.invoke(() -> !InventoryQuery.fromInventoryId(InventoryID.WORN).withId(itemId).collect().isEmpty());
    }

    /**
     * check if an item is equipped
     * @param itemName item name
     * @return bool
     */
    public static boolean isEquipped(String itemName)
    {
        return Static.invoke(() -> !InventoryQuery.fromInventoryId(InventoryID.WORN).withName(itemName).collect().isEmpty());
    }

    /**
     * check if an item is equipped
     * @param predicate predicate
     * @return bool
     */
    public static boolean isEquipped(Predicate<ItemEx> predicate)
    {
        return Static.invoke(() -> !InventoryQuery.fromInventoryId(InventoryID.WORN).keepIf(predicate).collect().isEmpty());
    }

    /**
     * get an equipped item by id
     * @param itemId item id
     * @return ItemEx
     */
    public static ItemEx getItem(int itemId)
    {
        return Static.invoke(() -> InventoryQuery.fromInventoryId(InventoryID.WORN).withId(itemId).first());
    }

    /**
     * get an equipped item by name
     * @param itemName name
     * @return ItemEx
     */
    public static ItemEx getItem(String itemName)
    {
        return Static.invoke(() -> InventoryQuery.fromInventoryId(InventoryID.WORN).withName(itemName).first());
    }

    /**
     * unequip an item
     * @param item item
     */
    public static void unEquip(ItemEx item)
    {
        interact(item, 1);
    }

    /**
     * unequip an item in a specific slot
     * @param slot slot
     */
    public static void unEquip(EquipmentSlot slot)
    {
        ItemEx item = fromSlot(slot);
        if(item != null) {
            interact(item, 1);
        }
    }

    /**
     * equip an item by id
     * @param itemId item id
     */
    public static void equip(int itemId)
    {
        InventoryAPI.interact(itemId, 3);
    }

    /**
     * equip an item by name
     * @param itemName item name
     */
    public static void equip(String itemName)
    {
        InventoryAPI.interact(itemName, 3);
    }

    /**
     * interact with an equipped item
     * @param item item
     * @param action action
     */
    public static void interact(ItemEx item, int action)
    {
        itemAction(item.getSlot(), action);
    }

    /**
     * invoke an action on an equipped item
     * @param slot slot
     * @param action action
     */
    public static void itemAction(int slot, int action)
    {
        TClient client = Static.getClient();
        Static.invoke(() -> {
            ClickManager.click(PacketInteractionType.EQUIPMENTITEM_INTERACT);
            client.getPacketWriter().widgetActionPacket(action, EquipmentSlot.findBySlot(slot).getWidgetInfo().getId(), -1, -1);
        });
    }

    /**
     * get an equipped item in a specific slot
     * @param slot slot
     * @return ItemEx
     */
    public static ItemEx fromSlot(EquipmentSlot slot)
    {
        return Static.invoke(() -> InventoryQuery.fromInventoryId(InventoryID.WORN).fromSlot(slot.getSlotIdx()).first());
    }

    /**
     * get all equipped items
     * @return List<ItemEx>
     */
    public static List<ItemEx> getAll(){
        return Static.invoke(() -> InventoryQuery.fromInventoryId(InventoryID.WORN).collect());
    }

    /**
     * unequip all items
     */
    public static void unequipAll(){
        unEquip(EquipmentSlot.AMULET);
        unEquip(EquipmentSlot.BODY);
        unEquip(EquipmentSlot.WEAPON);
        unEquip(EquipmentSlot.LEGS);
        unEquip(EquipmentSlot.SHIELD);
        unEquip(EquipmentSlot.AMMO);
        unEquip(EquipmentSlot.BOOTS);
        unEquip(EquipmentSlot.HEAD);
        unEquip(EquipmentSlot.CAPE);
        unEquip(EquipmentSlot.GLOVES);
        unEquip(EquipmentSlot.RING);
    }

    /**
     * get the count of a specific equipped item by id
     * @param id item id
     * @return int
     */
    public static int getCount(int id) {
        return Static.invoke(() -> InventoryQuery.fromInventoryId(InventoryID.WORN).withId(id).count());
    }
}
