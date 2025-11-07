package com.tonic.api.handlers;

import com.tonic.Logger;
import com.tonic.api.widgets.GrandExchangeAPI;
import com.tonic.data.GrandExchangeSlot;
import com.tonic.util.handler.HandlerBuilder;

import static com.tonic.api.widgets.GrandExchangeAPI.*;

public class GrandExchangeHandler extends HandlerBuilder {
    public static GrandExchangeHandler get()
    {
        return new GrandExchangeHandler();
    }

    private int currentStep = 0;

    private int getStep()
    {
        return currentStep;
    }

    private int getThenIncrementStep()
    {
        return currentStep++;
    }

    public GrandExchangeHandler collectAll()
    {
        add(getThenIncrementStep(), GrandExchangeAPI::collectAll);

        return this;
    }

    public GrandExchangeHandler buy(int itemId, int quantity, int pricePerItem, boolean noted)
    {
        buyOffer(itemId, quantity, pricePerItem);
        addDelayUntil(getThenIncrementStep(), context -> {
            GrandExchangeSlot slot = context.get("ge_slot_buy");
            return slot.isDone();
        });
        addDelay(getThenIncrementStep(), 1);
        add(getThenIncrementStep(), context -> {
            GrandExchangeSlot slot = context.get("ge_slot_buy");
            collectFromSlot(slot.getSlot(), noted, quantity);
        });
        return this;
    }

    public GrandExchangeHandler sell(int itemId, int quantity, int pricePerItem)
    {
        sellOffer(itemId, quantity, pricePerItem);
        addDelayUntil(getThenIncrementStep(), context -> {
            GrandExchangeSlot slot = context.get("ge_slot_sell");
            return slot.isDone();
        });
        add(getThenIncrementStep(), context -> {
            GrandExchangeSlot slot = context.get("ge_slot_sell");
            collectFromSlot(slot.getSlot());
        });
        return this;
    }

    public GrandExchangeHandler buyOffer(int itemId, int quantity, int pricePerItem)
    {
        int step = getStep() + 1;
        add(getThenIncrementStep(), context -> {
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
        int step = getStep() + 1;
        add(getThenIncrementStep(), context -> {
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
