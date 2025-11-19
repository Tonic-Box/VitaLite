package com.tonic.api.widgets;

import com.tonic.api.game.GameAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.queries.InventoryQuery;
import com.tonic.data.wrappers.ItemEx;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;

import java.util.function.Supplier;

/**
 * ShopAPI - methods for interacting with shops
 */
public class ShopAPI
{
    /**
     * @return true if the shop window is open
     */
    public static boolean isOpen()
    {
        return WidgetAPI.isVisible(InterfaceID.Shopmain.UNIVERSE);
    }

    /**
     * Closes the shop interface.
     */
    public static void close()
    {
        GameAPI.invokeMenuAction(1, 57, 11, InterfaceID.Shopmain.FRAME, -1);
    }

    /**
     * Purchases the amount of the desired item.
     * Stops when the amount purchased is reached, the item is out of stock, or you've hit the 10 actions per tick cap
     *
     * @param itemId The ID of the item to purchase. See {@link net.runelite.api.gameval.ItemID}
     * @param purchaseAmount The amount of the item to purchase
     */
    public static void buyX(int itemId, int purchaseAmount)
    {
        buyX(() -> getShopItem(itemId), purchaseAmount);
    }

    /**
     * Purchases the amount of the desired item.
     * Stops when the amount purchased is reached, the item is out of stock, or you've hit the 10 actions per tick cap
     *
     * @param itemName The name of the item to purchase.
     * @param purchaseAmount The amount of the item to purchase
     */
    public static void buyX(String itemName, int purchaseAmount)
    {
        buyX(() -> getShopItem(itemName), purchaseAmount);
    }

    private static void buyX(Supplier<ItemEx> supplier, int purchaseAmount)
    {
        ItemEx item = supplier.get();
        if (item == null)
        {
            return;
        }

        if (purchaseAmount == 0)
        {
            return;
        }

        int availableAmount = item.getQuantity();
        int actions = 0;
        while (purchaseAmount > 0 && actions <= 10)
        {
            if (availableAmount < purchaseAmount)
            {
                break;
            }

            actions++;
            if (purchaseAmount >= 50)
            {
                buyAction(item.getId(), item.getSlot(), 5);
                purchaseAmount -= 50;
                availableAmount -= 50;
            }
            else if (purchaseAmount >= 10)
            {
                buyAction(item.getId(), item.getSlot(), 4);
                purchaseAmount -= 10;
                availableAmount -= 10;
            }
            else if (purchaseAmount >= 5)
            {
                buyAction(item.getId(), item.getSlot(), 3);
                purchaseAmount -= 5;
                availableAmount -= 5;
            }
            else
            {
                buyAction(item.getId(), item.getSlot(), 2);
                purchaseAmount -= 1;
                availableAmount -= 1;
            }
        }
    }

    /**
     * buy 1 of an item from the shop by its id
     * @param itemId item id
     */
    public static void buy1(int itemId)
    {
        ItemEx item = getShopItem(itemId);
        if(item == null)
            return;
        buyAction(item.getId(), item.getSlot(), 2);
    }

    /**
     * buy 1 of an item from the shop by its name
     * @param itemName item name
     */
    public static void buy1(String itemName)
    {
        ItemEx item = getShopItem(itemName);
        if(item == null)
            return;
        buyAction(item.getId(), item.getSlot(), 2);
    }

    /**
     * buy 5 of an item from the shop by its id
     * @param itemId item id
     */
    public static void buy5(int itemId)
    {
        ItemEx item = getShopItem(itemId);
        if(item == null)
            return;
        buyAction(item.getId(), item.getSlot(), 3);
    }

    /**
     * buy 5 of an item from the shop by its name
     * @param itemName item name
     */
    public static void buy5(String itemName)
    {
        ItemEx item = getShopItem(itemName);
        if(item == null)
            return;
        buyAction(item.getId(), item.getSlot(), 3);
    }

    /**
     * buy 10 of an item from the shop by its id
     * @param itemId item id
     */
    public static void buy10(int itemId)
    {
        ItemEx item = getShopItem(itemId);
        if(item == null)
            return;
        buyAction(item.getId(), item.getSlot(), 4);
    }

    /**
     * buy 10 of an item from the shop by its name
     * @param itemName item name
     */
    public static void buy10(String itemName)
    {
        ItemEx item = getShopItem(itemName);
        if(item == null)
            return;
        buyAction(item.getId(), item.getSlot(), 4);
    }

    /**
     * buy 50 of an item from the shop by its id
     * @param itemId item id
     */
    public static void buy50(int itemId)
    {
        ItemEx item = getShopItem(itemId);
        if(item == null)
            return;
        buyAction(item.getId(), item.getSlot(), 5);
    }

    /**
     * buy 50 of an item from the shop by its name
     * @param itemName item name
     */
    public static void buy50(String itemName)
    {
        ItemEx item = getShopItem(itemName);
        if(item == null)
            return;
        buyAction(item.getId(), item.getSlot(), 5);
    }

    /**
     * Sells the amount of the desired item.
     * Stops when the amount sold is reached, you run out of items, or you've hit the 10 actions per tick cap
     *
     * @param itemId The ID of the item to sell
     * @param sellAmount The amount of the item to sell
     */
    public static void sellX(int itemId, int sellAmount)
    {
        sellX(() -> InventoryAPI.getItem(itemId), sellAmount);
    }

    /**
     * Sells the amount of the desired item.
     * Stops when the amount sold is reached, you run out of items, or you've hit the 10 actions per tick cap
     *
     * @param itemName The name of the item to sell
     * @param sellAmount The amount of the item to sell
     */
    public static void sellX(String itemName, int sellAmount)
    {
        sellX(() -> InventoryAPI.getItem(itemName), sellAmount);
    }

    /**
     * Attempts to sell a specified amount of a single item retrieved via a supplier.
     * Logic differs based on whether the item is stackable or not.
     *
     * @param supplier Supplies the current {@code ItemEx} instance to be sold.
     * @param sellAmount The total quantity of the item to sell.
     */
    private static void sellX(Supplier<ItemEx> supplier, int sellAmount)
    {
        if (sellAmount <= 0)
        {
            return;
        }

        ItemEx item = supplier.get();
        if (item == null)
        {
            return;
        }

        boolean isStackable = item.getQuantity() > 1;

        if (isStackable)
        {
            // For stackable items: Send multiple sell actions simultaneously
            int actions = 0;
            while (sellAmount > 0 && actions < 10)
            {
                actions++;

                if (sellAmount >= 50)
                {
                    sellAction(item.getId(), item.getSlot(), 5);
                    sellAmount -= 50;
                }
                else if (sellAmount >= 10)
                {
                    sellAction(item.getId(), item.getSlot(), 4);
                    sellAmount -= 10;
                }
                else if (sellAmount >= 5)
                {
                    sellAction(item.getId(), item.getSlot(), 3);
                    sellAmount -= 5;
                }
                else
                {
                    sellAction(item.getId(), item.getSlot(), 2);
                    sellAmount -= 1;
                }
            }
        }
        else // Non-stackable
        {
            // Optimization for large quantities
            if (sellAmount > 28)
            {
                sellAction(item.getId(), item.getSlot(), 5);
                return;
            }

            // For non-stackable items: Wait for inventory update after each sell
            int actions = 0;
            while (sellAmount > 0 && actions < 10)
            {
                // Get count before selling to determine when removal occurs
                int itemId = item.getId();
                int countBefore = InventoryQuery.fromInventoryId(InventoryID.INV)
                        .withId(itemId)
                        .count();

                actions++;

                if (sellAmount >= 50)
                {
                    sellAction(item.getId(), item.getSlot(), 5);
                    sellAmount -= 50;
                }
                else if (sellAmount >= 10)
                {
                    sellAction(item.getId(), item.getSlot(), 4);
                    sellAmount -= 10;
                }
                else if (sellAmount >= 5)
                {
                    sellAction(item.getId(), item.getSlot(), 3);
                    sellAmount -= 5;
                }
                else
                {
                    sellAction(item.getId(), item.getSlot(), 2);
                    sellAmount -= 1;
                }

                // Wait until the inventory count decreases
                Delays.waitUntil(() -> {
                    int countAfter = InventoryQuery.fromInventoryId(InventoryID.INV)
                            .withId(itemId)
                            .count();
                    return countAfter < countBefore;
                }, 2);

                // Re-query item for the next iteration slot will change
                item = supplier.get();
                if (item == null)
                {
                    break;
                }
            }
        }
    }

    /**
     * Sell 1 of an item from inventory by its id
     * @param itemId item id
     */
    public static void sell1(int itemId)
    {
        ItemEx item = InventoryAPI.getItem(itemId);
        if(item == null)
            return;
        sellAction(item.getId(), item.getSlot(), 2);
    }

    /**
     * Sell 1 of an item from inventory by its name
     * @param itemName item name
     */
    public static void sell1(String itemName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        if(item == null)
            return;
        sellAction(item.getId(), item.getSlot(), 2);
    }

    /**
     * Sell 5 of an item from inventory by its id
     * @param itemId item id
     */
    public static void sell5(int itemId)
    {
        ItemEx item = InventoryAPI.getItem(itemId);
        if(item == null)
            return;
        sellAction(item.getId(), item.getSlot(), 3);
    }

    /**
     * Sell 5 of an item from inventory by its name
     * @param itemName item name
     */
    public static void sell5(String itemName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        if(item == null)
            return;
        sellAction(item.getId(), item.getSlot(), 3);
    }

    /**
     * Sell 10 of an item from inventory by its id
     * @param itemId item id
     */
    public static void sell10(int itemId)
    {
        ItemEx item = InventoryAPI.getItem(itemId);
        if(item == null)
            return;
        sellAction(item.getId(), item.getSlot(), 4);
    }

    /**
     * Sell 10 of an item from inventory by its name
     * @param itemName item name
     */
    public static void sell10(String itemName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        if(item == null)
            return;
        sellAction(item.getId(), item.getSlot(), 4);
    }

    /**
     * Sell 50 of an item from inventory by its id
     * @param itemId item id
     */
    public static void sell50(int itemId)
    {
        ItemEx item = InventoryAPI.getItem(itemId);
        if(item == null)
            return;
        sellAction(item.getId(), item.getSlot(), 5);
    }

    /**
     * Sell 50 of an item from inventory by its name
     * @param itemName item name
     */
    public static void sell50(String itemName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        if(item == null)
            return;
        sellAction(item.getId(), item.getSlot(), 5);
    }

    /**
     * get an item by its name from the shop container
     * @param itemName item name
     * @return item
     */
    public static ItemEx getShopItem(String itemName)
    {
        return InventoryQuery.fromCurrentShop().withName(itemName).first();
    }

    /**
     * get an item by its id from the shop container
     * @param itemId item id
     * @return item
     */
    public static ItemEx getShopItem(int itemId)
    {
        return InventoryQuery.fromCurrentShop().withId(itemId).first();
    }

    /**
     * get the shops current quantity of an item
     * @param itemId item id
     * @return quantity
     */
    public static int getStockQuantity(int itemId)
    {
        return InventoryQuery.fromCurrentShop().withId(itemId).getQuantity();
    }

    /**
     * get the shops current quantity of an item
     * @param itemName item name
     * @return quantity
     */
    public static int getStockQuantity(String itemName)
    {
        return InventoryQuery.fromCurrentShop().withName(itemName).getQuantity();
    }

    /**
     * check if a shop currently has an item in stock
     * @param itemId item id
     * @return boolean
     */
    public static boolean shopContains(int itemId)
    {
        return getStockQuantity(itemId) != 0;
    }

    /**
     * check if a shop currently has an item in stock
     * @param itemName item name
     * @return boolean
     */
    public static boolean shopContains(String itemName)
    {
        return getStockQuantity(itemName) != 0;
    }

    /**
     * Send a buy action for a shop item
     * @param itemId item id
     * @param slot slot in shop inventory
     * @param action action type (2=buy1, 3=buy5, 4=buy10, 5=buy50)
     */
    public static void buyAction(int itemId, int slot, int action)
    {
        WidgetAPI.interact(action, InterfaceID.Shopmain.ITEMS, slot + 1, itemId);
    }

    /**
     * Send a sell action for an inventory item
     * @param itemId item id
     * @param slot slot in player inventory
     * @param action action type (0=sell1, 1=sell5, 2=sell10, 3=sell50)
     */
    public static void sellAction(int itemId, int slot, int action)
    {
        WidgetAPI.interact(action, InterfaceID.Shopside.ITEMS, slot, itemId);
    }
}