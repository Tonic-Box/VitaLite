package com.tonic.api.handlers;

import com.tonic.Logger;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.widgets.GrandExchangeAPI;
import com.tonic.data.GrandExchangeSlot;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.NpcQuery;
import com.tonic.util.handler.AbstractHandlerBuilder;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import static com.tonic.api.widgets.GrandExchangeAPI.*;

/**
 * Handler builder for interacting with the Grand Exchange.
 */
public class GrandExchangeHandler extends AbstractHandlerBuilder<GrandExchangeHandler> {

    /**
     * Creates a new instance of the GrandExchangeHandler.
     *
     * @return A new GrandExchangeHandler instance.
     */
    public static GrandExchangeHandler get()
    {
        return new GrandExchangeHandler();
    }
    private static final WorldPoint location = new WorldPoint(3164, 3487, 0);

    /**
     * Opens the Grand Exchange interface by walking to the location and interacting with the clerk.
     *
     * @return GrandExchangeHandler instance
     */
    public GrandExchangeHandler open()
    {
        walkTo(location);
        add(() -> {
            NpcEx clerk = new NpcQuery()
                    .withNameContains("Clerk")
                    .nearest();
            NpcAPI.interact(clerk, 2);
        });
        addDelayUntil(GrandExchangeAPI::isOpen);
        return this;
    }

    /**
     * Collects all items from the Grand Exchange.
     *
     * @return GrandExchangeHandler instance
     */
    public GrandExchangeHandler collectAll()
    {
        add(GrandExchangeAPI::collectAll);

        return this;
    }

    /**
     * Buys an item from the Grand Exchange.
     *
     * @param itemId      The ID of the item to buy.
     * @param quantity    The quantity of the item to buy.
     * @param pricePerItem The price per item.
     * @param noted       Whether to collect the item as noted.
     * @return GrandExchangeHandler instance
     */
    public GrandExchangeHandler buy(int itemId, int quantity, int pricePerItem, boolean noted)
    {
        buyOffer(itemId, quantity, pricePerItem);
        addDelayUntil(context -> {
            GrandExchangeSlot slot = context.get("ge_slot_buy");
            return slot.isDone();
        });
        addDelay(1);
        add(context -> {
            GrandExchangeSlot slot = context.get("ge_slot_buy");
            collectFromSlot(slot.getSlot(), noted, quantity);
        });
        return this;
    }

    /**
     * Sells an item to the Grand Exchange.
     *
     * @param itemId      The ID of the item to sell.
     * @param quantity    The quantity of the item to sell.
     * @param pricePerItem The price per item.
     * @return GrandExchangeHandler instance
     */
    public GrandExchangeHandler sell(int itemId, int quantity, int pricePerItem)
    {
        sellOffer(itemId, quantity, pricePerItem);
        addDelayUntil(context -> {
            GrandExchangeSlot slot = context.get("ge_slot_sell");
            return slot.isDone();
        });
        add(context -> {
            GrandExchangeSlot slot = context.get("ge_slot_sell");
            collectFromSlot(slot.getSlot());
        });
        return this;
    }

    /**
     * Initiates a buy offer on the Grand Exchange.
     *
     * @param itemId      The ID of the item to buy.
     * @param quantity    The quantity of the item to buy.
     * @param pricePerItem The price per item.
     * @return GrandExchangeHandler instance
     */
    public GrandExchangeHandler buyOffer(int itemId, int quantity, int pricePerItem)
    {
        int step = currentStep + 1;
        add(context -> {
            GrandExchangeSlot slot = startBuyOffer(itemId, quantity, pricePerItem);
            if(slot == null)
            {
                Logger.warn("Failed to buy '" + itemId + "' from the ge. No free slots.");
                return END_EXECUTION;
            }
            context.put("ge_slot_buy", slot);
            return step;
        });
        return this;
    }

    /**
     * Initiates a sell offer on the Grand Exchange.
     *
     * @param itemId      The ID of the item to sell.
     * @param quantity    The quantity of the item to sell.
     * @param pricePerItem The price per item.
     * @return GrandExchangeHandler instance
     */
    public GrandExchangeHandler sellOffer(int itemId, int quantity, int pricePerItem)
    {
        int step = currentStep + 1;
        add(context -> {
            GrandExchangeSlot slot = startSellOffer(itemId, quantity, pricePerItem);
            if(slot == null)
            {
                Logger.warn("Failed to buy '" + itemId + "' from the ge. No free slots.");
                return END_EXECUTION;
            }
            context.put("ge_slot_sell", slot);
            return step;
        });
        return this;
    }
}
