package com.tonic.data;

import com.tonic.Static;
import com.tonic.api.TItemComposition;
import com.tonic.services.GameManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

@RequiredArgsConstructor
@Getter
public class TileItemEx
{
    private final TileItem item;
    private final WorldPoint worldLocation;
    private final LocalPoint localPoint;
    private String[] actions = null;

    public int getId() {
        return item.getId();
    }

    public boolean isNoted() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getNote()) == 799;
    }

    public int getCanonicalId() {
        ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
        return itemManager.canonicalize(item.getId());
    }

    public String getName() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getName());
    }

    public int getQuantity() {
        return item.getQuantity();
    }

    public String[] getActions()
    {
        if(actions != null)
            return actions;
        if(item == null)
            return new String[0];
        actions = Static.invoke(() -> {
            Client client = Static.getClient();
            TItemComposition itemComp = (TItemComposition) client.getItemDefinition(item.getId());
            return itemComp.getGroundActions();
        });

        return actions;
    }

    public int getShopPrice() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getPrice());
    }

    public long getGePrice()
    {
        return Static.invoke(() -> {
            ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
            int id = itemManager.canonicalize(item.getId());
            if (id == ItemID.COINS)
            {
                return (long) getQuantity();
            }
            else if (id == ItemID.PLATINUM)
            {
                return getQuantity() * 1000L;
            }

            ItemComposition itemDef = itemManager.getItemComposition(id);
            if (itemDef.getPrice() <= 0)
            {
                return 0L;
            }

            return (long) itemManager.getItemPrice(id);
        });
    }

    public int getHighAlchValue()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getHaPrice());
    }

    public int getLowAlchValue()
    {
        return (int) Math.floor(getHighAlchValue() * 0.6);
    }
}
