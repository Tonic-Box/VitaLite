package com.tonic.api.widgets;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.loadouts.InventoryLoadout;
import com.tonic.api.loadouts.item.LoadoutItem;
import com.tonic.data.wrappers.ItemContainerEx;
import com.tonic.queries.InventoryQuery;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.services.GameManager;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * Bank API
 */
public class BankAPI
{

    private static class XSnapshot
    {
        private static int amount = -1;
        private static int tick = -1;
    }

    private static class WithdrawModeSnapshot
    {
        private static boolean noted = false;
        private static int tick = -1;
    }

    /**
     * Creates an instance of InventoryQuery from the Bank
     * @return InventoryQuery
     */
    public static InventoryQuery search() {
        return InventoryQuery.fromInventoryId(InventoryID.BANK);
    }

    public static int getX()
    {
        if (GameManager.getTickCount() == XSnapshot.tick)
        {
            return XSnapshot.amount;
        }

        return VarAPI.getVar(VarbitID.BANK_REQUESTEDQUANTITY);
    }

    /**
     * Sets the withdraw quantity mode.
     * Supports preset quantities (1, 5, 10, all) for compatibility and custom X amounts.
     * @param amount The quantity mode amount.
     */
    public static void setX(int amount)
    {
        if (BankQuantityResolver.isPresetAmount(amount))
        {
            setPresetQuantityMode(amount);
            return;
        }

        int withdrawMode = VarAPI.getVar(VarbitID.BANK_QUANTITY_TYPE);
        if(withdrawMode != 3)
        {
            WidgetAPI.interact(1, InterfaceID.Bankmain.QUANTITYX, -1, -1);
            waitForCondition(() -> VarAPI.getVar(VarbitID.BANK_QUANTITY_TYPE) == 3, 350);
        }

        int xQuantity = getX();
        if(xQuantity != amount)
        {
            WidgetAPI.interact(2, InterfaceID.Bankmain.QUANTITYX, -1, -1);
            waitBriefly(25);
            DialogueAPI.resumeNumericDialogue(amount);
            XSnapshot.amount = amount;
            XSnapshot.tick = GameManager.getTickCount();
            waitForCondition(() -> VarAPI.getVar(VarbitID.BANK_REQUESTEDQUANTITY) == amount, 350);
        }
    }

    private static void setPresetQuantityMode(int amount)
    {
        switch (amount)
        {
            case 1:
                clickQuantityButton(InterfaceID.Bankmain.QUANTITY1, InterfaceID.Bankmain.QUANTITY1_TEXT);
                break;
            case 5:
                clickQuantityButton(InterfaceID.Bankmain.QUANTITY5, InterfaceID.Bankmain.QUANTITY5_TEXT);
                break;
            case 10:
                clickQuantityButton(InterfaceID.Bankmain.QUANTITY10, InterfaceID.Bankmain.QUANTITY10_TEXT);
                break;
            case -1:
                clickQuantityButton(InterfaceID.Bankmain.QUANTITYALL, InterfaceID.Bankmain.QUANTITYALL_TEXT);
                break;
            default:
                break;
        }
    }

    private static void clickQuantityButton(int primaryWidgetId, int fallbackWidgetId)
    {
        if (WidgetAPI.isVisible(primaryWidgetId))
        {
            WidgetAPI.interact(1, primaryWidgetId, -1, -1);
            return;
        }
        WidgetAPI.interact(1, fallbackWidgetId, -1, -1);
    }

    private static int resolveItemSlot(int containerWidgetId, int preferredSlot, int itemId)
    {
        Widget container = WidgetAPI.get(containerWidgetId);
        if (container == null)
        {
            return -1;
        }

        if (preferredSlot >= 0)
        {
            Widget preferred = Static.invoke(() -> container.getChild(preferredSlot));
            if (preferred != null && preferred.getItemId() == itemId && preferred.getItemQuantity() > 0)
            {
                return preferredSlot;
            }
        }

        Widget[] children = Static.invoke(container::getChildren);
        if (children == null)
        {
            return -1;
        }

        for (int i = 0; i < children.length; i++)
        {
            Widget child = children[i];
            if (child == null)
            {
                continue;
            }
            if (child.getItemId() == itemId && child.getItemQuantity() > 0)
            {
                return i;
            }
        }

        return -1;
    }

    private static boolean interactItemByAction(int containerWidgetId, int slot, int itemId, String action)
    {
        Widget container = WidgetAPI.get(containerWidgetId);
        if (container == null)
        {
            return false;
        }

        Widget widget = Static.invoke(() -> container.getChild(slot));
        if (widget == null || widget.getItemId() != itemId || widget.getItemQuantity() <= 0)
        {
            return false;
        }

        WidgetAPI.interact(widget, action);
        return true;
    }

    private static void waitForCondition(BooleanSupplier condition, int timeoutMs)
    {
        Client client = Static.getClient();
        if (client == null || client.isClientThread() || timeoutMs <= 0)
        {
            return;
        }

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline)
        {
            if (condition.getAsBoolean())
            {
                return;
            }

            try
            {
                Thread.sleep(20L);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void waitBriefly(int milliseconds)
    {
        Client client = Static.getClient();
        if (client == null || client.isClientThread() || milliseconds <= 0)
        {
            return;
        }

        try
        {
            Thread.sleep(milliseconds);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean withdraw(InventoryLoadout loadout)
    {
        return withdraw(loadout, 10);
    }

    public static boolean withdraw(InventoryLoadout loadout, int maxActionsPerTick)
    {
        if (!isOpen())
        {
            //TODO open the bank?
            return false;
        }

        int actions = 0;

        //deposit items not in the loadout
        int foreignDepositActions = depositForeignLoadoutItems(loadout, maxActionsPerTick);
        if (foreignDepositActions == Integer.MAX_VALUE)
        {
            //we used the deposit inventory button
            actions++;
        }
        else
        {
            actions += foreignDepositActions;
        }

        //deposit items that we have too many of
        actions += depositExcessLoadoutItems(loadout);

        //handle the withdraw magic
        for (LoadoutItem item : loadout)
        {
            if (actions >= maxActionsPerTick)
            {
                return true;
            }

            List<ItemEx> banked = item.getBanked();
            if (banked.isEmpty())
            {
                if (!item.isOptional() && loadout.getItemDepletionListener() != null)
                {
                    loadout.getItemDepletionListener().onDeplete(item);
                }
                continue;
            }

            List<ItemEx> carried = item.getCarried();

            int count;
            if (carried.isEmpty())
            {
                count = 0;
            }
            else if (item.isStackable())
            {
                count = carried.get(0).getQuantity();
            }
            else
            {
                count = carried.size();
            }

            int remaining = item.getAmount() - count;
            if (remaining < 0)
            {
                //We have too many, and depositExcessAmounts needs to deal with it
                return false;
            }

            if (remaining == 0)
            {
                continue;
            }

            if (remaining > BankAPI.count(banked.get(0).getId()) && !item.isOptional())
            {
                if (loadout.getItemDepletionListener() != null)
                {
                    loadout.getItemDepletionListener().onDeplete(item);
                }
                continue;
            }

            //BankAPI#withdraw handles withdraw mode but we need to keep track of actions, so do it ourselves
            if (item.isNoted() && !isWithdrawNote())
            {
                setWithdrawMode(true);
                actions++;
            }
            else if (!item.isNoted() && isWithdrawNote())
            {
                setWithdrawMode(false);
                actions++;
            }

            withdraw(banked.get(0).getId(), remaining, item.isNoted());
            actions++;
        }

        return true;
    }

    private static int depositForeignLoadoutItems(InventoryLoadout loadout, int maxActionsPerTick) {
        List<ItemEx> foreign = loadout.getForeignItems();
        if (foreign.isEmpty())
        {
            return 0;
        }

        List<ItemEx> items = InventoryAPI.getItems();
        if (items.size() == foreign.size())
        {
            depositAll();
            return Integer.MAX_VALUE;
        }

        Set<Integer> unique = foreign.stream()
            .map(ItemEx::getId)
            .collect(Collectors.toSet());
        if (unique.size() >= maxActionsPerTick)
        {
            //easier to use depositAll
            depositAll();
            return Integer.MAX_VALUE;
        }

        int actions = 0;
        for (int id : unique)
        {
            System.out.println(id + " is foreign, depositing!");
            //is there not a depositAll(id)?
            deposit(id, -1);
            actions++;
        }

        return actions;
    }

    private static int depositExcessLoadoutItems(InventoryLoadout loadout) {
        List<LoadoutItem> excess = loadout.getExcessItems();
        int actions = 0;
        for (LoadoutItem item : excess)
        {
            List<ItemEx> carried = item.getCarried();
            if (carried.isEmpty())
            {
                continue;
            }

            int count = item.isStackable() ? carried.get(0).getQuantity() : carried.size();
            int extra = count - item.getAmount();
            if (extra <= 0)
            {
                continue;
            }

            deposit(carried.get(0).getId(), extra);
            actions++;
        }

        return actions;
    }

    public static boolean isWithdrawNote()
    {
        if (GameManager.getTickCount() == WithdrawModeSnapshot.tick)
        {
            return WithdrawModeSnapshot.noted;
        }

        return VarAPI.getVar(VarbitID.BANK_WITHDRAWNOTES) == 1;
    }

    /**
     * Sets the withdraw mode to either noted or unnoted.
     * @param noted True for noted, false for unnoted.
     */
    public static void setWithdrawMode(boolean noted) {
        boolean currentlyNoted = VarAPI.getVar(VarbitID.BANK_WITHDRAWNOTES) == 1;

        if (currentlyNoted == noted) {
            return;
        }

        WithdrawModeSnapshot.noted = noted;
        WithdrawModeSnapshot.tick = GameManager.getTickCount();

        WidgetAPI.interact(1, InterfaceID.Bankmain.NOTE, -1, -1);
    }

    /**
     * Withdraws the specified amount of the item with the given id from the bank.
     * @param id The id of the item to withdraw.
     * @param amount The amount to withdraw. Use -1 for "all" option.
     * @param noted True to withdraw as noted, false to withdraw as unnoted.
     */
    public static void withdraw(int id, int amount, boolean noted) {
        setWithdrawMode(noted);
        ItemEx item = InventoryQuery.fromInventoryId(InventoryID.BANK).withId(id).first();

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
     * Withdraws the specified amount of the item with the given name from the bank.
     * @param item The ItemEx to withdraw.
     * @param amount The amount to withdraw. Use -1 for "all" option.
     * @param noted True to withdraw as noted, false to withdraw as unnoted.
     */
    public static void withdraw(ItemEx item, int amount, boolean noted) {
        setWithdrawMode(noted);

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
        if(!BankQuantityResolver.isPresetAmount(amount))
        {
            setX(amount);
        }

        int resolvedSlot = resolveItemSlot(InterfaceID.Bankmain.ITEMS, slot, id);
        if (resolvedSlot == -1)
        {
            return;
        }

        String action = BankQuantityResolver.withdrawMenuAction(amount);
        if (interactItemByAction(InterfaceID.Bankmain.ITEMS, resolvedSlot, id, action))
        {
            return;
        }

        WidgetAPI.interact(BankQuantityResolver.withdrawFallbackAction(amount), InterfaceID.Bankmain.ITEMS, resolvedSlot, id);
    }

    /**
     * Invokes the deposit action on the inventory item.
     * @param id The id of the item to deposit.
     * @param amount The amount to deposit.
     * @param slot The slot of the item in the inventory.
     */
    public static void depositAction(int id, int amount, int slot) {
        if(!BankQuantityResolver.isPresetAmount(amount))
        {
            setX(amount);
        }

        int resolvedSlot = resolveItemSlot(InterfaceID.Bankside.ITEMS, slot, id);
        if (resolvedSlot == -1)
        {
            return;
        }

        String action = BankQuantityResolver.depositMenuAction(amount);
        if (interactItemByAction(InterfaceID.Bankside.ITEMS, resolvedSlot, id, action))
        {
            return;
        }

        WidgetAPI.interact(BankQuantityResolver.depositFallbackAction(amount), InterfaceID.Bankside.ITEMS, resolvedSlot, id);
    }

    /** Widget ID for Empty containers button (opcode 57, param1 786471) */
    private static final int WIDGET_EMPTY_CONTAINERS = 786471;
    /** Widget ID for Deposit inventory button (opcode 57, param1 786473) */
    private static final int WIDGET_DEPOSIT_INVENTORY = 786473;
    /** Widget ID for Deposit worn items button (opcode 57, param1 786475) */
    private static final int WIDGET_DEPOSIT_WORN_ITEMS = 786475;

    /**
     * Deposits all items from the containers in inventory into the bank (Empty containers).
     */
    public static void depositAllContainers() {
        WidgetAPI.interact(1, WIDGET_EMPTY_CONTAINERS, -1, -1);
    }

    /**
     * Deposits all items from the inventory into the bank (Deposit inventory).
     */
    public static void depositAll() {
        WidgetAPI.interact(1, WIDGET_DEPOSIT_INVENTORY, -1, -1);
    }

    /**
     * Deposits all items from the equipment into the bank (Deposit worn items).
     */
    public static void depositEquipment() {
        WidgetAPI.interact(1, WIDGET_DEPOSIT_WORN_ITEMS, -1, -1);
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
     * Closes the bank if it is currently open.
     */
    public static void close()
    {
        if (BankAPI.isOpen())
        {
            ClientScriptAPI.runScript(29);
        }
    }

    /**
     * Checks if the bank contains an item with the specified id.
     * @param itemIds The item ids to check for.
     * @return True if the item is in the bank, false otherwise.
     */
    public static boolean contains(int... itemIds)
    {
        return InventoryQuery.fromInventoryId(InventoryID.BANK).withId(itemIds).first() != null;
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
        ItemEx item = InventoryQuery.fromInventoryId(InventoryID.BANK).withName(itemName).first();
        if(item == null)
            return -1;
        return item.getSlot();
    }

    /**
     * Uses the item with the specified id from the inventory.
     * @param itemId The id of the item to use.
     */
    public static void use(int itemId) {
        ItemEx item = InventoryQuery.fromInventoryId(InventoryID.INV).withId(itemId).first();
        if(item == null)
            return;
        WidgetAPI.interact(9, InterfaceID.Bankside.ITEMS, item.getSlot(), itemId);
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
     * Uses the item with the specified id from the bank container.
     * @param itemId The id of the item to use.
     */
    public static void bankUse(int itemId) {
        ItemEx item = InventoryQuery.fromInventoryId(InventoryID.INV).withId(itemId).first();
        if(item == null)
            return;
        WidgetAPI.interact(9, InterfaceID.Bankmain.ITEMS, item.getSlot(), itemId);
    }

    /**
     * Uses the item with the specified name from the bank container.
     * @param itemName The name of the item to use.
     */
    public static void bankUse(String itemName) {
        ItemEx item = InventoryQuery.fromInventoryId(InventoryID.BANK).withName(itemName).first();
        if(item == null)
            return;
        WidgetAPI.interact(9, InterfaceID.Bankmain.ITEMS, item.getSlot(), item.getId());
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

    /**
     * drag an item in your bank to another slot
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
     * drag an item in your bank to another slot
     * @param id item id
     * @param toSlot to slot
     */
    public static void dragItem(int id, int toSlot)
    {
        ItemEx item = search().withId(id).first();
        if(item == null)
            return;

        dragItem(id, item.getSlot(), toSlot);
    }

    /**
     * drag an item in your bank to another slot
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

        WidgetAPI.dragWidget(InterfaceID.Bankmain.ITEMS, item.getId(), item.getSlot(), InterfaceID.Bankmain.ITEMS, itemId2, toSlot);
    }

    /**
     * Drag an item to a new bank tab
     * @param item item
     * @param toTab bank tab (0,1,2,3...)
     */
    public static void dragItemToTab(ItemEx item, int toTab)
    {
        if(item == null)
            return;

        dragItemToTab(item.getId(), item.getSlot(), toTab);
    }

    /**
     * Drag an item to a new bank tab
     * @param id item id
     * @param toTab bank tab (0,1,2,3...)
     */
    public static void dragItemToTab(int id, int toTab)
    {
        ItemEx item = search().withId(id).first();
        if(item == null)
            return;

        dragItemToTab(id, item.getSlot(), toTab);
    }

    /**
     * Drag an item to a new bank tab
     * @param itemId item itemId
     * @param fromSlot from slot
     * @param toTab bank tab (0,1,2,3...)
     */
    public static void dragItemToTab(int itemId, int fromSlot, int toTab)
    {
        ItemEx item = search().fromSlot(fromSlot).first();
        if(item == null || item.getId() != itemId)
            return;

        int tabIndex = toTab + 10;

        WidgetAPI.dragWidget(InterfaceID.Bankmain.ITEMS, item.getId(), item.getSlot(), InterfaceID.Bankmain.TABS, -1, tabIndex);
    }
}
