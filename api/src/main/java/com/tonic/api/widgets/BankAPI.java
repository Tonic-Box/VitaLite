package com.tonic.api.widgets;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.VarAPI;
import com.tonic.data.ItemContainerEx;
import com.tonic.queries.InventoryQuery;
import com.tonic.data.ItemEx;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.WidgetInfo;

/**
 * Bank API
 */
public class BankAPI
{
    /**
     * Sets the withdraw amount to the specified amount.
     * @param amount The amount to set the withdraw amount to.
     */
    public static void setX(int amount)
    {
        int withdrawMode = VarAPI.getVar(VarbitID.BANK_QUANTITY_TYPE);
        if(withdrawMode != 3)
        {
            WidgetAPI.interact(1, InterfaceID.Bankmain.QUANTITYX, -1, -1);
        }

        int xQuantity = VarAPI.getVar(VarbitID.BANK_REQUESTEDQUANTITY);
        if(xQuantity != amount && amount != 1 && amount != 5 && amount != 10 && amount != -1)
        {
            WidgetAPI.interact(2, InterfaceID.Bankmain.QUANTITYX, -1, -1);
            DialogueAPI.resumeNumericDialogue(amount);
        }
    }

    /**
     * Sets the withdraw mode to either noted or unnoted.
     * @param noted True for noted, false for unnoted.
     */
    public static void setWithdrawMode(boolean noted) {
        if(noted) {
            WidgetAPI.interact(1, InterfaceID.Bankmain.NOTE, -1, -1);
        }
        else {
            WidgetAPI.interact(1, InterfaceID.Bankmain.ITEM, -1, -1);
        }
    }

    /**
     * Withdraws the specified amount of the item with the given id from the bank.
     * @param id The id of the item to withdraw.
     * @param amount The amount to withdraw. Use -1 for "all" option.
     * @param noted True to withdraw as noted, false to withdraw as unnoted.
     */
    public static void withdraw(int id, int amount, boolean noted) {
        setWithdrawMode(noted);
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.BANK).withId(id).first()
        );

        if(item == null)
            return;

        withdrawAction(item.getId(), amount, item.getSlot());
    }

    /**
     * Withdraws the specified amount of the item with the given name from the bank.
     * @param name The name of the item to withdraw.
     * @param amount The amount to withdraw. Use -1 for "all" option.
     * @param noted True to withdraw as noted, false to withdraw as unnoted.
     */
    public static void withdraw(String name, int amount, boolean noted) {
        setWithdrawMode(noted);
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.BANK).withName(name).first()
        );

        if(item == null)
            return;

        withdrawAction(item.getId(), amount, item.getSlot());
    }

    /**
     * Deposits the specified amount of the item with the given id into the bank.
     * @param id The id of the item to deposit.
     * @param amount The amount to deposit. Use -1 for "all" option.
     */
    public static void deposit(int id, int amount) {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.INV).withId(id).first()
        );

        if(item == null)
            return;

        depositAction(item.getId(), amount, item.getSlot());
    }

    /**
     * Deposits the specified amount of the item with the given name into the bank.
     * @param name The name of the item to deposit.
     * @param amount The amount to deposit. Use -1 for "all" option.
     */
    public static void deposit(String name, int amount) {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.INV).withName(name).first()
        );

        if(item == null)
            return;

        depositAction(item.getId(), amount, item.getSlot());
    }

    /**
     * Invokes the withdraw action on the bank item.
     * @param id The id of the item to withdraw.
     * @param amount The amount to withdraw.
     * @param slot The slot of the item in the bank.
     */
    public static void withdrawAction(int id, int amount, int slot) {
        setX(amount);
        if(amount == 1) {
            WidgetAPI.interact(2, InterfaceID.Bankmain.ITEMS, slot, id);
        }
        else if(amount == 5) {
            WidgetAPI.interact(3, InterfaceID.Bankmain.ITEMS, slot, id);
        }
        else if(amount == 10) {
            WidgetAPI.interact(4, InterfaceID.Bankmain.ITEMS, slot, id);
        }
        else if(amount == -1) {
            WidgetAPI.interact(7, InterfaceID.Bankmain.ITEMS, slot, id);
        }
        else {
            WidgetAPI.interact(5, InterfaceID.Bankmain.ITEMS, slot, id);
        }
    }

    /**
     * Invokes the deposit action on the inventory item.
     * @param id The id of the item to deposit.
     * @param amount The amount to deposit.
     * @param slot The slot of the item in the inventory.
     */
    public static void depositAction(int id, int amount, int slot) {
        setX(amount);
        if(amount == 1) {
            WidgetAPI.interact(3, InterfaceID.Bankside.ITEMS, slot, id);
        }
        else if(amount == 5) {
            WidgetAPI.interact(4, InterfaceID.Bankside.ITEMS, slot, id);
        }
        else if(amount == 10) {
            WidgetAPI.interact(5, InterfaceID.Bankside.ITEMS, slot, id);
        }
        else if(amount == -1) {
            WidgetAPI.interact(8, InterfaceID.Bankside.ITEMS, slot, id);
        }
        else {
            WidgetAPI.interact(2, InterfaceID.Bankside.ITEMS, slot, id);
        }
    }

    /**
     * Deposits all items from the inventory into the bank.
     */
    public static void depositAll() {
        WidgetAPI.interact(1, InterfaceID.Bankmain.DEPOSITINV, -1, -1);
    }

    /**
     * Deposits all items from the equipment into the bank.
     */
    public static void depositEquipment() {
        WidgetAPI.interact(1, InterfaceID.Bankmain.DEPOSITWORN, -1, -1);
    }

    /**
     * Checks if the bank is currently open.
     * @return True if the bank is open, false otherwise.
     */
    public static boolean isOpen()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemContainer(InventoryID.BANK) != null);
    }

    /**
     * Checks if the bank contains an item with the specified id.
     * @param itemId The id of the item to check for.
     * @return True if the item is in the bank, false otherwise.
     */
    public static boolean contains(int itemId)
    {
        return InventoryQuery.fromInventoryId(InventoryID.BANK).withId(itemId).first() != null;
    }

    /**
     * Checks if the bank contains an item with the specified name.
     * @param itemName The name of the item to check for.
     * @return True if the item is in the bank, false otherwise.
     */
    public static boolean contains(String itemName)
    {
        return InventoryQuery.fromInventoryId(InventoryID.BANK).withName(itemName).first() != null;
    }

    /**
     * Counts the total quantity of the item with the specified id in the bank.
     * @param itemId The id of the item to count.
     * @return The total quantity of the item in the bank.
     */
    public static int count(int itemId)
    {
        return InventoryQuery.fromInventoryId(InventoryID.BANK).withId(itemId).count();
    }

    /**
     * Counts the total quantity of the item with the specified name in the bank.
     * @param itemName The name of the item to count.
     * @return The total quantity of the item in the bank.
     */
    public static int count(String itemName)
    {
        return InventoryQuery.fromInventoryId(InventoryID.BANK).withName(itemName).count();
    }

    /**
     * Gets the slot of the item with the specified id in the bank.
     * @param itemId The id of the item to get the slot for.
     * @return The slot of the item in the bank, or -1 if the item is not found.
     */
    public static int getSlot(int itemId)
    {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.BANK).withId(itemId).first()
        );
        if(item == null)
            return -1;
        return item.getSlot();
    }

    /**
     * Gets the slot of the item with the specified name in the bank.
     * @param itemName The name of the item to get the slot for.
     * @return The slot of the item in the bank, or -1 if the item is not found.
     */
    public static int getSlot(String itemName)
    {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.BANK).withName(itemName).first()
        );
        if(item == null)
            return -1;
        return item.getSlot();
    }

    /**
     * Uses the item with the specified id from the inventory.
     * @param itemId The id of the item to use.
     */
    public static void use(int itemId) {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.INV).withId(itemId).first()
        );
        if(item == null)
            return;
        WidgetAPI.interact(9, InterfaceID.Bankside.ITEMS, item.getId(), itemId);
    }

    /**
     * Uses the item with the specified name from the inventory.
     * @param itemName The name of the item to use.
     */
    public static void use(String itemName) {
        ItemEx item = Static.invoke(() ->
                InventoryQuery.fromInventoryId(InventoryID.INV).withName(itemName).first()
        );
        if(item == null)
            return;
        WidgetAPI.interact(9, InterfaceID.Bankside.ITEMS, item.getSlot(), item.getId());
    }

    /**
     * Attempts to guess the slot an item will be withdrawn into (for use in programatically
     * 1-ticking interactions with withdrawn items) and "uses" the item. Not guarenteed to be
     * 100% accurate.
     * @param itemId The item id
     */
    public static void useGuessNextSlot(int itemId) {
        int slot = Static.invoke(() -> {
            ItemContainerEx inventory = new ItemContainerEx(InventoryID.INV);
            return inventory.getNextEmptySlot();
        });
        if(slot == -1)
        {
            Logger.warn("[useGuessNextSlot] Inventory full already");
            return;
        }
        WidgetAPI.interact(9, InterfaceID.Bankside.ITEMS, slot, itemId);
    }
}
