package com.tonic.api.handlers;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.ItemEx;
import com.tonic.data.TileItemEx;
import com.tonic.data.TileObjectEx;
import com.tonic.util.handler.AbstractHandlerBuilder;
import com.tonic.util.handler.HandlerBuilder;
import net.runelite.api.NPC;
import net.runelite.api.Player;

public class InventoryBuilder extends AbstractHandlerBuilder
{
    public static HandlerBuilder get()
    {
        return new HandlerBuilder();
    }

    public InventoryBuilder interact(int itemId, String action)
    {
        add(() -> InventoryAPI.interact(itemId, action));
        return this;
    }

    public InventoryBuilder interact(String itemName, String action)
    {
        add(() -> InventoryAPI.interact(itemName, action));
        return this;
    }

    public InventoryBuilder interact(int itemId, String menu, String action)
    {
        add(() -> InventoryAPI.interactSubOp(itemId, menu, action));
        return this;
    }

    public InventoryBuilder interact(String itemName, String menu, String action)
    {
        add(() -> InventoryAPI.interactSubOp(itemName, menu, action));
        return this;
    }

    public InventoryBuilder drop(String... itemNames)
    {

        add(context -> {
            int delay = InventoryAPI.dropAll(itemNames);
            context.put("DELAY", delay);
        });
        addDelayUntil(context -> {
            int delay = context.get("DELAY");
            context.put("DELAY", delay - 1);
            return delay <= 0;
        });
        return this;
    }

    public InventoryBuilder useOnItem(int delay, String itemName1, String itemName2)
    {
        ItemEx item1 = InventoryAPI.getItem(itemName1);
        ItemEx item2 = InventoryAPI.getItem(itemName2);
        add(() -> InventoryAPI.useOn(item1, item2));
        addDelayUntil(() -> PlayerAPI.isIdle());
        if(delay > 0)
        {
            addDelay(delay);
        }
        return this;
    }

    public InventoryBuilder useOnTileItem(int delay, String itemName, String tileItemName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        TileItemEx tileItem = TileItemAPI.search().withName(tileItemName).first();
        add(() -> InventoryAPI.useOn(item, tileItem));
        addDelayUntil(() -> PlayerAPI.isIdle());
        if(delay > 0)
        {
            addDelay(delay);
        }
        return this;
    }

    public InventoryBuilder useOnObject(int delay, String itemName, String objectName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        TileObjectEx object = TileObjectAPI.get(objectName);
        add(() -> InventoryAPI.useOn(item, object));
        addDelayUntil(() -> PlayerAPI.isIdle());
        if(delay > 0)
        {
            addDelay(delay);
        }
        return this;
    }

    public InventoryBuilder useOnNpc(int delay, String itemName, String npcName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        NPC npc = NpcAPI.search().withName(npcName).first();
        add(() -> InventoryAPI.useOn(item, npc));
        addDelayUntil(() -> PlayerAPI.isIdle());
        if(delay > 0)
        {
            addDelay(delay);
        }
        return this;
    }

    public InventoryBuilder useOnPlayer(int delay, String itemName, String playerName)
    {
        ItemEx item = InventoryAPI.getItem(itemName);
        Player player = PlayerAPI.search().withName(playerName).first();
        add(() -> InventoryAPI.useOn(item, player));
        addDelayUntil(() -> PlayerAPI.isIdle());
        if(delay > 0)
        {
            addDelay(delay);
        }
        return this;
    }
}
