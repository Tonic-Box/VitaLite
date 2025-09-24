package com.tonic.data;

import com.tonic.Static;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ItemContainerEx
{
    private final int containerId;
    private final List<ItemEx> items;

    public ItemContainerEx(InventoryID inventoryID)
    {
        this(inventoryID.getId());
    }
    public ItemContainerEx(int containerId)
    {
        this.containerId = containerId;
        Client client = Static.getClient();
        ItemContainer container = Static.invoke(() -> client.getItemContainer(containerId));
        if(container == null)
        {
            items = List.of();
            return;
        }
        items = new ArrayList<>();
        for(int i = 0; i < container.getItems().length; i++)
        {
            if(container.getItems()[i] == null || container.getItems()[i].getId() <= 0)
                continue;
            items.add(new ItemEx(container.getItems()[i], i));
        }
    }

    public List<ItemEx> getAll(int... itemIds)
    {
        List<ItemEx> found = new ArrayList<>();
        for(int itemId : itemIds)
        {
            for(ItemEx item : items)
            {
                if(item.getItem().getId() == itemId)
                    found.add(item);
            }
        }
        return found;
    }

    public List<ItemEx> getAll(String... itemNames)
    {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            ItemComposition itemComp;
            List<ItemEx> found = new ArrayList<>();
            for(String itemName : itemNames)
            {
                for(ItemEx item : items)
                {
                    itemComp = client.getItemDefinition(item.getItem().getId());
                    if(itemName.equalsIgnoreCase(itemComp.getName()))
                        found.add(item);
                }
            }
            return found;
        });
    }

    public ItemEx getFirst(int itemId)
    {
        for(ItemEx item : items)
        {
            if(item.getItem().getId() == itemId)
                return item;
        }
        return null;
    }

    public ItemEx getFirst(String itemName)
    {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            ItemComposition itemComp;
            for(ItemEx item : items)
            {
                itemComp = client.getItemDefinition(item.getItem().getId());
                if(itemName.equalsIgnoreCase(itemComp.getName()))
                    return item;
            }
            return null;
        });
    }

    public ItemEx getFirstContains(String partialName)
    {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            ItemComposition itemComp;
            for(ItemEx item : items)
            {
                itemComp = client.getItemDefinition(item.getItem().getId());
                if(itemComp.getName().toLowerCase().contains(partialName.toLowerCase()))
                    return item;
            }
            return null;
        });
    }

    public int getNextEmptySlot() {
        Client client = Static.getClient();
        ItemContainer container = Static.invoke(() -> client.getItemContainer(containerId));
        if(container == null)
        {
            return -1;
        }
        for(int i = 0; i < container.getItems().length; i++)
        {
            if(container.getItems()[i] == null || container.getItems()[i].getId() <= 0)
            {
                return i;
            }
        }
        return -1;
    }
}
