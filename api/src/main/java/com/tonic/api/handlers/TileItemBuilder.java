package com.tonic.api.handlers;

import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.util.handler.AbstractHandlerBuilder;
import net.runelite.api.coords.WorldPoint;

/**
 * Builder for handling interactions with tile items.
 */
public class TileItemBuilder extends AbstractHandlerBuilder<TileItemBuilder>
{
    /**
     * Creates a new instance of TileItemBuilder.
     *
     * @return A new TileItemBuilder instance.
     */
    public static TileItemBuilder get()
    {
        return new TileItemBuilder();
    }

    /**
     * Interacts with a tile item by its name and action.
     *
     * @param itemName The name of the item to interact with.
     * @param action   The action to perform on the item.
     * @return TileItemBuilder instance
     */
    public TileItemBuilder interact(String itemName, String action)
    {
        add(() -> {
            TileItemEx item = TileItemAPI.search().withNameContains(itemName).first();
            if(item != null)
            {
                TileItemAPI.interact(item, action);
            }
        });
        addDelayUntil(() -> PlayerAPI.isIdle());
        return this;
    }

    /**
     * Interacts with a tile item by its ID and action.
     *
     * @param itemId The ID of the item to interact with.
     * @param action The action to perform on the item.
     * @return TileItemBuilder instance
     */
    public TileItemBuilder interact(int itemId, String action)
    {
        add(() -> {
            TileItemEx item = TileItemAPI.search().withId(itemId).first();
            if(item != null)
            {
                TileItemAPI.interact(item, action);
            }
        });
        addDelayUntil(() -> PlayerAPI.isIdle());
        return this;
    }

    /**
     * Interacts with a tile item by its name and action index.
     *
     * @param itemName The name of the item to interact with.
     * @param action   The action index to perform on the item.
     * @return TileItemBuilder instance
     */
    public TileItemBuilder interact(String itemName, int action)
    {
        add(() -> {
            TileItemEx item = TileItemAPI.search().withNameContains(itemName).first();
            if(item != null)
            {
                TileItemAPI.interact(item, action);
            }
        });
        addDelayUntil(() -> PlayerAPI.isIdle());
        return this;
    }

    /**
     * Interacts with a tile item by its ID and action index.
     *
     * @param itemId The ID of the item to interact with.
     * @param action The action index to perform on the item.
     * @return TileItemBuilder instance
     */
    public TileItemBuilder interact(int itemId, int action)
    {
        add(() -> {
            TileItemEx item = TileItemAPI.search().withId(itemId).first();
            if(item != null)
            {
                TileItemAPI.interact(item, action);
            }
        });
        addDelayUntil(() -> PlayerAPI.isIdle());
        return this;
    }

    /**
     * Walks to a specified world point and picks up an item by its name.
     *
     * @param point The world point to walk to.
     * @param itemName The name of the item to pick up.
     * @return TileItemBuilder instance
     */
    public TileItemBuilder pickUp(WorldPoint point, String itemName)
    {
        walkTo(point);
        add(context -> {
            context.put("COUNT_" + itemName, InventoryAPI.count(itemName));
        });
        int step = currentStep + 1;
        interact(itemName, 2);
        int nextStep = currentStep + 1;
        add(context -> {
            int count = context.get("COUNT_" + itemName);
            return InventoryAPI.count(itemName) > count ? step : nextStep;
        });
        return this;
    }

    /**
     * Walks to a specified world point and picks up an item by its ID.
     *
     * @param point The world point to walk to.
     * @param itemId The ID of the item to pick up.
     * @return TileItemBuilder instance
     */
    public TileItemBuilder pickUp(WorldPoint point, int itemId)
    {
        walkTo(point);
        add(context -> {
            context.put("COUNT_" + itemId, InventoryAPI.count(itemId));
        });
        int step = currentStep + 1;
        interact(itemId, 2);
        int nextStep = currentStep + 1;
        add(context -> {
            int count = context.get("COUNT_" + itemId);
            return InventoryAPI.count(itemId) > count ? step : nextStep;
        });
        return this;
    }
}
