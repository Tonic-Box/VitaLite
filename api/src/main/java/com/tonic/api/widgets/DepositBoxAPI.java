package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.game.GameAPI;
import com.tonic.data.EquipmentSlot;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.queries.InventoryQuery;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deposit Box API.
 */
public class DepositBoxAPI {

    /**
     * Checks if the deposit box is currently open.
     *
     * @return true if the deposit box interface is open, false otherwise.
     */
    public static boolean isOpen() {
       return WidgetAPI.isVisible(InterfaceID.BankDepositbox.INVENTORY);
    }

    /**
     * Closes the deposit box interface.
     */
    public static void close() {
        GameAPI.invokeMenuAction(1, 57, 11, InterfaceID.BankDepositbox.FRAME, -1);
    }

    /**
     * Deposits all items from the inventory.
     */
    public static void depositAll() {
        WidgetAPI.interact(1, InterfaceID.BankDepositbox.DEPOSIT_INV, -1, -1);
    }

    /**
     * Deposits all worn items.
     */
    public static void depositWornItems() {
        WidgetAPI.interact(1, InterfaceID.BankDepositbox.DEPOSIT_WORN, -1, -1);
    }

    /**
     * Deposits all items from the looting bag.
     */
    public static void depositLootingBag() {
        WidgetAPI.interact(1, InterfaceID.BankDepositbox.DEPOSIT_LOOTINGBAG, -1, -1);
    }

    /**
     * Deposits the specified amount of an item from the inventory by its ID.
     *
     * @param id     The item ID to deposit.
     * @param amount The amount to deposit (1, 5, 10, -1 for all, or a specific value).
     */
    public static void deposit(int id, int amount) {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.INV).withId(id).first()
        );

        if (item == null) {
            return;
        }

        depositAction(item.getId(), amount, item.getSlot());
    }

    /**
     * Deposits the specified amount of an item from the inventory by its name.
     *
     * @param name   The item name to deposit (exact match).
     * @param amount The amount to deposit (1, 5, 10, -1 for all, or a specific value).
     */
    public static void deposit(String name, int amount) {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.INV).withName(name).first()
        );

        if (item == null) {
            return;
        }

        depositAction(item.getId(), amount, item.getSlot());
    }

    /**
     * Deposits all items from inventory except the specified item IDs.
     *
     * @param excludedIds Item IDs to keep in inventory.
     */
    public static void depositAllExcept(int... excludedIds) {
        Set<Integer> excludedSet = Arrays.stream(excludedIds)
                .boxed()
                .collect(Collectors.toUnmodifiableSet());

        Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.INV)
                        .removeIf(item -> excludedSet.contains(item.getId()))
                        .forEach(item -> depositAction(item.getId(), -1, item.getSlot()))
        );
    }

    /**
     * Deposits all items from inventory except the specified item names.
     *
     * @param excludedNames Item names to keep in inventory.
     */
    public static void depositAllExcept(String... excludedNames) {
        Set<String> excludedSet = Set.of(excludedNames);

        Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.INV)
                        .removeIf(item -> excludedSet.contains(item.getName()))
                        .forEach(item -> depositAction(item.getId(), -1, item.getSlot()))
        );
    }

    /**
     * Internal helper to perform the deposit action.
     *
     * @param id     The item ID.
     * @param amount The amount.
     * @param slot   The inventory slot.
     */
    private static void depositAction(int id, int amount, int slot) {
        setQuantity(amount);
        WidgetAPI.interact(1, InterfaceID.BankDepositbox.INVENTORY, slot, id);
    }

    /**
     * Internal helper to set the deposit quantity.
     *
     * @param amount The amount to set (-1 for All, 1, 5, 10, or X).
     */
    private static void setQuantity(int amount) {
        if (amount == 1) {
            WidgetAPI.interact(1, InterfaceID.BankDepositbox._1, -1, -1);
        } else if (amount == 5) {
            WidgetAPI.interact(1, InterfaceID.BankDepositbox._5, -1, -1);
        } else if (amount == 10) {
            WidgetAPI.interact(1, InterfaceID.BankDepositbox._10, -1, -1);
        } else if (amount == -1) {
            WidgetAPI.interact(1, InterfaceID.BankDepositbox.ALL, -1, -1);
        } else {
            WidgetAPI.interact(1, InterfaceID.BankDepositbox.X, -1, -1);
            DialogueAPI.resumeNumericDialogue(amount);
        }
    }

    /**
     * Deposits a worn item from the specified equipment slot.
     *
     * @param slot The equipment slot to deposit from.
     */
    public static void depositWorn(EquipmentSlot slot) {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.WORN).fromSlot(slot.getSlotIdx()).first()
        );

        if (item == null) {
            return;
        }

        depositWornAction(slot);
    }

    /**
     * Deposits a specific worn item by item ID.
     *
     * @param id The item ID to deposit.
     */
    public static void depositWorn(int id) {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.WORN).withId(id).first()
        );

        if (item == null) {
            return;
        }

        EquipmentSlot slot = EquipmentSlot.findBySlot(item.getSlot());
        if (slot == null) {
            return;
        }

        depositWornAction(slot);
    }

    /**
     * Deposits a specific worn item by item name.
     *
     * @param name The item name to deposit.
     */
    public static void depositWorn(String name) {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.WORN).withName(name).first()
        );

        if (item == null) {
            return;
        }

        EquipmentSlot slot = EquipmentSlot.findBySlot(item.getSlot());
        if (slot == null) {
            return;
        }

        depositWornAction(slot);
    }

    /**
     * Invokes the deposit action on a worn item in the specified equipment slot.
     *
     * @param slot The equipment slot.
     */
    private static void depositWornAction(EquipmentSlot slot) {
        int slotWidgetId = getDepositBoxSlotWidget(slot);
        WidgetAPI.interact(2, slotWidgetId, -1, -1);
    }

    /**
     * Maps an EquipmentSlot to its corresponding BankDepositBox.SLOT widget ID.
     *
     * @param slot The equipment slot.
     * @return The widget ID for that slot in the deposit box interface.
     * @throws IllegalArgumentException if the slot is not a valid mappable slot.
     */
    private static int getDepositBoxSlotWidget(EquipmentSlot slot) {
        switch (slot) {
            case HEAD: return InterfaceID.BankDepositbox.SLOT0;
            case CAPE: return InterfaceID.BankDepositbox.SLOT1;
            case AMULET: return InterfaceID.BankDepositbox.SLOT2;
            case WEAPON: return InterfaceID.BankDepositbox.SLOT3;
            case BODY: return InterfaceID.BankDepositbox.SLOT4;
            case SHIELD: return InterfaceID.BankDepositbox.SLOT5;
            case LEGS: return InterfaceID.BankDepositbox.SLOT7;
            case GLOVES: return InterfaceID.BankDepositbox.SLOT9;
            case BOOTS: return InterfaceID.BankDepositbox.SLOT10;
            case RING: return InterfaceID.BankDepositbox.SLOT12;
            case AMMO: return InterfaceID.BankDepositbox.SLOT13;
            default: throw new IllegalArgumentException("Unknown equipment slot: " + slot);
        }
    }
}