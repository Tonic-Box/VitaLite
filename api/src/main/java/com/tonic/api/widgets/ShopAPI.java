package com.tonic.api.widgets;

import com.tonic.queries.InventoryQuery;
import com.tonic.data.ItemEx;
import com.tonic.data.ShopID;
import net.runelite.api.gameval.InterfaceID;

import java.util.function.Supplier;

/**
 * ShopAPI - methods for interacting with shops
 */
public class ShopAPI
{

    /**
     * Purchases the amount of the desired item
     *
     * @param itemId The ID of the item to purchase. See {@link net.runelite.api.gameval.ItemID}
     * @param purchaseAmount The amount of the item to purchase
     */
    public static void buyX(int itemId, int purchaseAmount)
    {
        buyX(() -> getShopItem(itemId), purchaseAmount);
    }

    /**
     * Purchases the amount of the desired item
     *
     * @param itemName The name of the item to purchase. See
     * @param purchaseAmount The amount of the item to purchase
     */
    static void buyX(String itemName, int purchaseAmount)
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
                buy_50(item);
                purchaseAmount -= 50;
                availableAmount -= 50;
            }
            else if (purchaseAmount >= 10)
            {
                buy_10(item);
                purchaseAmount -= 10;
                availableAmount -= 10;
            }
            else if (purchaseAmount >= 5)
            {
                buy_5(item);
                purchaseAmount -= 5;
                availableAmount -= 5;
            }
            else
            {
                buy_1(item);
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
        buy_1(item);
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
        buy_1(item);
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
        buy_5(item);
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
        buy_5(item);
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
        buy_10(item);
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
        buy_10(item);
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
        buy_50(item);
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
        buy_50(item);
    }

    /**
     * get an item by its name from the shop container
     * @param itemName item name
     * @return item
     */
    public static ItemEx getShopItem(String itemName)
    {
        return InventoryQuery.fromShopId(ShopID.getCurrent()).withName(itemName).first();
    }

    /**
     * get an item by its id from the shop container
     * @param itemId item id
     * @return item
     */
    public static ItemEx getShopItem(int itemId)
    {
        return InventoryQuery.fromShopId(ShopID.getCurrent()).withId(itemId).first();
    }

    /**
     * get the shops current quantity of an item
     * @param itemId item id
     * @return quantity
     */
    public static int getStockQuantity(int itemId)
    {
        return InventoryQuery.fromShopId(ShopID.getCurrent()).withId(itemId).getQuantity();
    }

    /**
     * get the shops current quantity of an item
     * @param itemName item name
     * @return quantity
     */
    public static int getStockQuantity(String itemName)
    {
        return InventoryQuery.fromShopId(ShopID.getCurrent()).withName(itemName).getQuantity();
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
     * buy 1 of an item
     * @param item item
     */
    public static void buy_1(ItemEx item)
    {
        interactShop(item, 2);
    }

    /**
     * buy 5 of an item
     * @param item item
     */
    public static void buy_5(ItemEx item)
    {
        interactShop(item, 3);
    }

    /**
     * buy 10 of an item
     * @param item item
     */
    public static void buy_10(ItemEx item)
    {
        interactShop(item, 4);
    }

    /**
     * buy 50 of an item
     * @param item item
     */
    public static void buy_50(ItemEx item)
    {
        interactShop(item, 5);
    }

    /**
     * interactShop with a shop
     * @param item item
     * @param action action
     */
    public static void interactShop(ItemEx item, int action)
    {
        shopAction(item.getId(), item.getSlot(), action);
    }

    /**
     * send a raw shop item menu action
     * @param itemId item id
     * @param slot slot
     * @param action action
     */
    public static void shopAction(int itemId, int slot, int action)
    {
        WidgetAPI.interact(action, InterfaceID.Shopmain.ITEMS, slot + 1, itemId);
    }
}
