package com.tonic.api.handlers;

import com.tonic.Logger;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.widgets.GrandExchangeAPI;
import com.tonic.data.GrandExchangeSlot;
import com.tonic.data.WorldLocation;
import com.tonic.queries.NpcQuery;
import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.util.handler.AbstractHandlerBuilder;
import com.tonic.util.handler.HandlerBuilder;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import static com.tonic.api.widgets.GrandExchangeAPI.*;

public class GrandExchangeHandler extends AbstractHandlerBuilder {
    public static GrandExchangeHandler get()
    {
        return new GrandExchangeHandler();
    }
    private static final WorldPoint location = new WorldPoint(3164, 3487, 0);

    public GrandExchangeHandler open()
    {
        walkTo(location);
        add(() -> {
            NPC clerk = new NpcQuery()
                    .withNameContains("Clerk")
                    .nearest();
            NpcAPI.interact(clerk, 2);
        });
        addDelayUntil(GrandExchangeAPI::isOpen);
        return this;
    }

    public GrandExchangeHandler collectAll()
    {
        add(GrandExchangeAPI::collectAll);

        return this;
    }

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
