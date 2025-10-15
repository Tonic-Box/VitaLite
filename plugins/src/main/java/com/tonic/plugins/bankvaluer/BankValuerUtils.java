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
     * gets the top 10 most valuble items in your bank
     * @return Map<ItemId,ItemValue>
     */
    public static Map<Integer,Long> getTopTenItems()
    {
        if(Static.getClient() == null)
            return new HashMap<>();
        return Static.invoke(() -> {
            Map<Integer,Long> topTen = new HashMap<>();
            Map<Integer,Integer> cache = BankCache.getCachedBank();
            if(cache == null || cache.isEmpty())
                return topTen;


            for(Map.Entry<Integer,Integer> entry : cache.entrySet())
            {
                int id = entry.getKey();
                int quantity = entry.getValue();
                long itemPrice = getGePrice(id, quantity);
                if(itemPrice <= 0)
                    continue;
                topTen.put(id,itemPrice);
                if(topTen.size() > 10)
                {
                    int lowestId = -1;
                    long lowestValue = Long.MAX_VALUE;
                    for(Map.Entry<Integer,Long> e : topTen.entrySet())
                    {
                        if(e.getValue() < lowestValue)
                        {
                            lowestValue = e.getValue();
                            lowestId = e.getKey();
                        }
                    }
                    if(lowestId != -1)
                        topTen.remove(lowestId);
                }
            }
            return topTen;
        });
    }

    public static long getGePrice(int id, int quantity)
    {
        ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
        if (id == ItemID.COINS)
        {
            return quantity;
        }
        else if (id == ItemID.PLATINUM)
        {
            return quantity * 1000L;
        }

        ItemComposition itemDef = itemManager.getItemComposition(id);

        if (itemDef.getPrice() <= 0)
        {
            return 0L;
        }

        return (long) itemManager.getItemPrice(id) * (long) quantity;
    }
}
