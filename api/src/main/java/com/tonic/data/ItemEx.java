package com.tonic.data;

import com.tonic.Static;
import com.tonic.util.TextUtil;
import lombok.*;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

import java.awt.*;

@Getter
@RequiredArgsConstructor
public class ItemEx {
    private final Item item;
    private final int slot;
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
        return Static.invoke(() -> itemManager.canonicalize(item.getId()));
    }

    public int getLinkedNoteId() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getLinkedNoteId());
    }

    public String getName() {
        Client client = Static.getClient();
        return Static.invoke(() -> TextUtil.sanitize(client.getItemDefinition(item.getId()).getName()));
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
            ItemComposition itemComp = client.getItemDefinition(item.getId());
            return itemComp.getInventoryActions();
        });
        return actions;
    }

    public boolean hasAction(String action)
    {
        String[] actions = getActions();
        if(actions == null)
            return false;
        for(String a : actions)
        {
            if(a != null && a.equalsIgnoreCase(action))
                return true;
        }
        return false;
    }

    public boolean hasActionContains(String actionPart)
    {
        String[] actions = getActions();
        if(actions == null)
            return false;
        for(String a : actions)
        {
            if(a != null && a.toLowerCase().contains(actionPart.toLowerCase()))
                return true;
        }
        return false;
    }

    public Widget getWidget() {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            Widget inventory = client.getWidget(InterfaceID.Inventory.ITEMS);
            if(inventory == null)
            {
                return null;
            }
            return inventory.getChild(getSlot());
        });
    }

    public Shape getClickBox()
    {
        Widget w = getWidget();
        if(w == null)
            return null;
        return w.getBounds();
    }

    public int getShopPrice() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getPrice());
    }

    public long getGePrice()
    {
        ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
        int id = itemManager.canonicalize(item.getId());
        if (id == ItemID.COINS)
        {
            return getQuantity();
        }
        else if (id == ItemID.PLATINUM)
        {
            return getQuantity() * 1000L;
        }

        ItemComposition itemDef = itemManager.getItemComposition(id);
        // Only check prices for things with store prices
        if (itemDef.getPrice() <= 0)
        {
            return 0;
        }

        return itemManager.getItemPrice(id);
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
