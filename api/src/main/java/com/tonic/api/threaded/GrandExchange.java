package com.tonic.api.threaded;

import com.tonic.Logger;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.data.GrandExchangeSlot;

import static com.tonic.api.widgets.GrandExchangeAPI.*;

public class GrandExchange
{
    /**
     * start a fully automated buy offer
     * @param id item id
     * @param quantity amount
     */
    public static void buy(int id, int quantity, boolean noted, int tries)
    {
        int slotNumber = freeSlot();
        if(slotNumber == -1)
            return;
        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);
        if(slot == null)
        {
            Logger.warn("Failed to buy '" + id + "' from the ge. No free slots.");
            return;
        }

        int percents = 5;
        int tryCount = 0;
        while(!buyNow(id, quantity, slot, percents) && tryCount++ < tries)
        {
            percents += 5;
        }
        ClientScriptAPI.closeNumericInputDialogue();
        Delays.tick();
        collectFromSlot(slotNumber, noted, quantity);
        Delays.tick();
    }

    public static void buy(int id, int quantity, int price)
    {
        GrandExchangeSlot slot = startBuyOffer(id, quantity, price);
        if(slot == null)
        {
            Logger.warn("Failed to buy '" + id + "' from the ge. No free slots.");
            return;
        }
        while(!slot.isDone())
        {
            Delays.tick();
        }
        Delays.tick();
        collectFromSlot(slot.getSlot(), true, quantity);
        Delays.tick();
    }

    public static void buy(int id, int quantity, int price, boolean noted)
    {
        GrandExchangeSlot slot = startBuyOffer(id, quantity, price);
        if(slot == null)
        {
            Logger.warn("Failed to buy '" + id + "' fromt he ge. No free slots.");
            return;
        }
        while(!slot.isDone())
        {
            bypassHighOfferWarning();
            Delays.tick();
        }
        ClientScriptAPI.closeNumericInputDialogue();
        Delays.tick();
        collectFromSlot(slot.getSlot(), noted, quantity);
        Delays.tick();
    }

    public static void buy(int id, int quantity, boolean noted)
    {
        int slotNumber = freeSlot();
        if(slotNumber == -1)
            return;
        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);
        if(slot == null)
        {
            Logger.warn("Failed to buy '" + id + "' from the ge. No free slots.");
            return;
        }

        int percents = 5;
        while(!buyNow(id, quantity, slot, percents))
        {
            percents += 5;
        }
        ClientScriptAPI.closeNumericInputDialogue();
        Delays.tick();
        collectFromSlot(slotNumber, noted, quantity);
        Delays.tick();
    }

    private static boolean buyNow(int id, int quantity, GrandExchangeSlot slot, int percents)
    {
        startBuyOfferPercentage(id, quantity, percents, slot.getSlot());
        int timeout = 3;
        while(!slot.isDone() && timeout-- > 0)
        {
            bypassHighOfferWarning();
            Delays.tick();
        }

        if(!slot.isDone())
        {
            cancel(slot);
            return false;
        }
        return true;
    }

    /**
     * fully automated sell offer
     * @param id item id
     * @param quantity amount
     * @param immediate attempt to insta sell
     */
    public static void sell(int id, int quantity, boolean immediate)
    {
        int slotNumber = freeSlot();
        if(slotNumber == -1)
            return;

        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);
        if(slot == null)
        {
            Logger.warn("Failed to sell '" + id + "' to the ge. No free slots.");
            return;
        }

        startSellOfferPercentage(id, quantity, ((immediate)?-15:-1), slotNumber);
        if(immediate)
        {
            int timeout = 5;
            while(!slot.isDone() && timeout-- > 0)
            {
                bypassHighOfferWarning();
                Delays.tick();
            }
            if(slot.isDone())
            {
                collectFromSlot(slotNumber, true, quantity);
            }
        }
    }
}
