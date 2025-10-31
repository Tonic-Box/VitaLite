package com.tonic.plugins.bankvaluer;

import com.tonic.Static;
import com.tonic.services.BankCache;
import com.tonic.util.TextUtil;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class BankValuerUtils
{
    public static void getItemImage(JLabel label, int itemId, int quantity)
    {
        ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
        AsyncBufferedImage itemImage = itemManager.getImage(itemId, quantity, quantity > 1);
        itemImage.onLoaded(() ->
        {
            label.setIcon(new ImageIcon(itemImage));
        });
    }

    public static String getName(int id) {
        Client client = Static.getClient();
        return Static.invoke(() -> TextUtil.sanitize(client.getItemDefinition(id).getName()));
    }

    /**
     * Gets the top valued items in your bank limited by the provided amount.
     *
     * @param limit the maximum number of items to return
     * @return Map<ItemId, ItemValue>
     */
    public static Map<Integer, Long> getTopItems(int limit, boolean hideUntradeables)
    {
        if (Static.getClient() == null || limit <= 0)
            return new HashMap<>();
        return Static.invoke(() -> {
            Map<Integer, Long> topItems = new HashMap<>();
            Map<Integer,Integer> cache = BankCache.getCachedBank();
            if(cache == null || cache.isEmpty())
                return topItems;

            ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);


            for(Map.Entry<Integer,Integer> entry : cache.entrySet())
            {
                int id = entry.getKey();
                int quantity = entry.getValue();
                ItemComposition itemDef = itemManager.getItemComposition(id);

                if (hideUntradeables && !itemDef.isTradeable())
                {
                    continue;
                }

                long itemPrice = getGePrice(itemManager, itemDef, id, quantity);
                if(itemPrice <= 0)
                    continue;
                topItems.put(id,itemPrice);
                if(topItems.size() > limit)
                {
                    int lowestId = -1;
                    long lowestValue = Long.MAX_VALUE;
                    for(Map.Entry<Integer,Long> e : topItems.entrySet())
                    {
                        if(e.getValue() < lowestValue)
                        {
                            lowestValue = e.getValue();
                            lowestId = e.getKey();
                        }
                    }
                    if(lowestId != -1)
                        topItems.remove(lowestId);
                }
            }
            return topItems;
        });
    }

    public static long getGePrice(int id, int quantity)
    {
        ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
        ItemComposition itemDef = itemManager.getItemComposition(id);
        return getGePrice(itemManager, itemDef, id, quantity);
    }

    private static long getGePrice(ItemManager itemManager, ItemComposition itemDef, int id, int quantity)
    {
        if (id == ItemID.COINS)
        {
            return quantity;
        }
        else if (id == ItemID.PLATINUM)
        {
            return quantity * 1000L;
        }

        if (itemDef.getPrice() <= 0)
        {
            return 0L;
        }

        return (long) itemManager.getItemPrice(id) * (long) quantity;
    }
}
