package com.tonic.api.widgets;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.data.GrandExchangeSlot;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickPacket.ClickType;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;


/**
 * Grand Exchange API
 */
public class GrandExchangeAPI
{
    /**
     * Bypasses the high offer warning dialog if it is open.
     */
    public static void bypassHighOfferWarning()
    {
        Client client = Static.getClient();
        TClient tclient = Static.getClient();
        Static.invoke(() -> {
            Widget w = client.getWidget(289, 8);
            if(w == null || !WidgetAPI.isVisible(w))
                return;

            ClickManager.click(ClickType.WIDGET);
            tclient.getPacketWriter().resumeCountDialoguePacket(1);
        });
    }

    /**
     * Cancels an active Grand Exchange offer in the specified slot.
     * @param slot The GrandExchangeSlot to cancel.
     */
    public static void cancel(GrandExchangeSlot slot)
    {
        WidgetAPI.interact(2, slot.getId(), 2, -1);
        WidgetAPI.interact(1, slot.getId(), 2, -1);
        Delays.tick(2);
        WidgetAPI.interact(2, InterfaceID.GeOffers.DETAILS_COLLECT, 2, ItemID.COINS);
    }

    /**
     * Returns the first free Grand Exchange slot (1-8) or -1 if none are free.
     * @return slot
     */
    public static int freeSlot()
    {
        try
        {
            Client client = Static.getClient();
            GrandExchangeOffer[] offers = Static.invoke(client::getGrandExchangeOffers);
            for (int slot = 0; slot < 8; slot++) {
                if (offers[slot] == null || offers[slot].getState() == GrandExchangeOfferState.EMPTY)
                {
                    return slot+1;
                }
            }
        }
        catch (Exception e) {
            Logger.error(e);
        }
        return -1;
    }

    /**
     * Starts a buy offer in the first free Grand Exchange slot.
     * @param itemId The item ID to buy.
     * @param amount The amount to buy.
     * @param price The price per item.
     * @return The GrandExchangeSlot object representing the slot used, or null if no slot is free or an error occurs.
     */
    public static GrandExchangeSlot startBuyOffer(int itemId, int amount, int price)
    {
        int slotNumber = freeSlot();
        if(slotNumber == -1)
            return null;
        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);
        if(slot == null)
            return null;
        TClient client = Static.getClient();
        Static.invoke(() -> {
            WidgetAPI.interact(1, slot.getId(), slot.getBuyChild(), -1);
            DialogueAPI.resumeObjectDialogue(itemId);
            WidgetAPI.interact(1, InterfaceID.GeOffers.SETUP, 12, -1);
            DialogueAPI.resumeNumericDialogue(price);
            WidgetAPI.interact(1, InterfaceID.GeOffers.SETUP, 7, -1);
            DialogueAPI.resumeNumericDialogue(amount);
            WidgetAPI.interact(1, InterfaceID.GeOffers.SETUP_CONFIRM, -1, -1);
        });
        ClientScriptAPI.closeNumericInputDialogue();
        return slot;
    }

    /**
     * start a buy offer priced by a # of 5%s
     * @param itemId item id
     * @param amount amount
     * @param FivePercents price
     * @param slotNumber slot
     * @return slot number
     */
    public static int startBuyOfferPercentage(int itemId, int amount, int FivePercents, int slotNumber)
    {
        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);
        if(slot == null)
            return -1;
        Static.invoke(() -> {
            WidgetAPI.interact(1, slot.getId(), slot.getBuyChild(), -1);
            DialogueAPI.resumeObjectDialogue(itemId);
            WidgetAPI.interact(1, InterfaceID.GeOffers.SETUP, 7, -1);
            DialogueAPI.resumeNumericDialogue(amount);
        });
        int ticker;
        if(FivePercents < 0)
        {
            ticker = 10;
            FivePercents = FivePercents * -1;
        }
        else
        {
            ticker = 13;
        }
        int finalFivePercents = FivePercents;
        Static.invoke(() -> {
            for(int i = finalFivePercents; i > 0; i--) {
                WidgetAPI.interact(1, InterfaceID.GeOffers.SETUP, ticker, -1);
            }
            WidgetAPI.interact(1, InterfaceID.GeOffers.SETUP_CONFIRM, -1, -1);
        });
        Delays.tick((finalFivePercents/10));
        ClientScriptAPI.closeNumericInputDialogue();
        return slotNumber;
    }

    /**
     * Starts a sell offer in the first free Grand Exchange slot.
     * @param itemId The item ID to sell.
     * @param amount The amount to sell. Use -1 to sell all available.
     * @param price The price per item.
     * @return The GrandExchangeSlot object representing the slot used, or null if no slot is free or an error occurs.
     */
    public static GrandExchangeSlot startSellOffer(int itemId, int amount, int price)
    {
        int slotNumber = freeSlot();
        if(slotNumber == -1)
            return null;
        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);
        if(slot == null)
            return null;
        TClient client = Static.getClient();
        Static.invoke(() -> {
            client.getPacketWriter().widgetActionPacket(1, slot.getId(), slot.getSellChild(), -1);
            client.getPacketWriter().widgetActionPacket(1, InterfaceID.GeOffersSide.ITEMS, getItemSlot(itemId), itemId);

            client.getPacketWriter().widgetActionPacket(1, InterfaceID.GeOffers.SETUP, 12, -1);
            client.getPacketWriter().resumeCountDialoguePacket(price);
            if(amount != -1)
            {
                client.getPacketWriter().widgetActionPacket(1, InterfaceID.GeOffers.SETUP, 7, -1);
                client.getPacketWriter().resumeCountDialoguePacket(amount);
            }
            client.getPacketWriter().widgetActionPacket(1, InterfaceID.GeOffers.SETUP_CONFIRM, -1, -1);
            client.getPacketWriter().resumeCountDialoguePacket(1);
        });
        ClientScriptAPI.closeNumericInputDialogue();
        return slot;
    }

    /**
     * start a sell offer priced by a # of 5%s
     * @param itemId item id
     * @param amount amount
     * @param FivePercents price
     * @param slotNumber slot
     */
    public static void startSellOfferPercentage(int itemId, int amount, int FivePercents, int slotNumber)
    {
        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);
        if(slot == null)
            return;
        WidgetAPI.interact(1, slot.getId(), slot.getSellChild(), -1);
        WidgetAPI.interact(1, InterfaceID.GeOffersSide.ITEMS, getItemSlot(itemId), itemId);
        int ticker;
        if(FivePercents < 0) {
            ticker = 10;
            FivePercents = FivePercents * -1;
        }
        else {
            ticker = 13;
        }
        int finalFivePercents = FivePercents;
        Static.invoke(() -> {
            for(int i = finalFivePercents; i > 0; i--) {
                WidgetAPI.interact(1, InterfaceID.GeOffers.SETUP, ticker, -1);
            }
            if(amount != -1)
            {
                WidgetAPI.interact(1, InterfaceID.GeOffers.SETUP, 7, -1);
                DialogueAPI.resumeNumericDialogue(amount);
            }
            WidgetAPI.interact(1, InterfaceID.GeOffers.SETUP_CONFIRM, -1, -1);
        });
        ClientScriptAPI.closeNumericInputDialogue();
    }

    /**
     * Collects items from a specified Grand Exchange slot.
     * @param slotNumber The slot number (1-8) to collect from.
     * @param noted True to collect items as noted, false for unnoted.
     * @param amount The amount to collect. Use 1 for a single item, or any other number for all available.
     */
    public static void collectFromSlot(int slotNumber, boolean noted, int amount)
    {
        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);
        if(slot == null)
            return;
        Static.invoke(() -> {
            int n = noted ? 1 : 2;
            if(amount == 1)
            {
                n = noted ? 2 : 1;
            }
            int itemId = slot.getItemId();
            WidgetAPI.interact(1, slot.getId(), 2, -1);
            WidgetAPI.interact(n, InterfaceID.GeOffers.DETAILS_COLLECT, 2, itemId);
            WidgetAPI.interact(1, InterfaceID.GeOffers.DETAILS_COLLECT, 3, ItemID.COINS);
        });
    }

    /**
     * Collects gold from from a specified Grand Exchange slot (fromm sell offer).
     * @param slotNumber The slot number (1-8) to collect from.
     */
    public static void collectFromSlot(int slotNumber)
    {
        GrandExchangeSlot slot = GrandExchangeSlot.getBySlot(slotNumber);
        if(slot == null)
            return;
        Static.invoke(() -> {
            int itemId = slot.getItemId();
            WidgetAPI.interact(1, slot.getId(), 2, -1);
            WidgetAPI.interact(2, InterfaceID.GeOffers.DETAILS_COLLECT, 2, itemId);
        });
    }

    private static int getItemSlot(int id)
    {
        ItemEx item = InventoryAPI.getItem(id);
        if(item == null)
            return -1;
        return item.getSlot();
    }

    /**
     * Checks if the Grand Exchange interface is currently open.
     * @return true if the Grand Exchange interface is open, false otherwise.
     */
    public static boolean isOpen()
    {
        return WidgetAPI.isVisible(InterfaceID.GeOffers.UNIVERSE);
    }

    /**
     * Collects all completed offers in the Grand Exchange.
     */
    public static void collectAll()
    {
        WidgetAPI.interact(1, InterfaceID.GeOffers.COLLECTALL, 0, -1);
    }
}
