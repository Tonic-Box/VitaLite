package com.tonic.data;

import com.tonic.Static;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.gameval.InterfaceID;

@Getter
public enum GrandExchangeSlot
{
    SLOT_1(InterfaceID.GeOffers.INDEX_0, 1),
    SLOT_2(InterfaceID.GeOffers.INDEX_1, 2),
    SLOT_3(InterfaceID.GeOffers.INDEX_2, 3),
    SLOT_4(InterfaceID.GeOffers.INDEX_3, 4),
    SLOT_5(InterfaceID.GeOffers.INDEX_4, 5),
    SLOT_6(InterfaceID.GeOffers.INDEX_5, 6),
    SLOT_7(InterfaceID.GeOffers.INDEX_6, 7),
    SLOT_8(InterfaceID.GeOffers.INDEX_7, 8);

    private final int id;

    private final int slot;

    private final int buyChild = 3;

    private final int sellChild = 4;

    GrandExchangeSlot(int id, int slot)
    {
        this.id = id;
        this.slot = slot;
    }

    public boolean isDone()
    {
        GrandExchangeOfferState state = offer().getState();
        return state == GrandExchangeOfferState.BOUGHT || state == GrandExchangeOfferState.SOLD;
    }

    public int getItemId()
    {
        int itemId = offer().getItemId();
        return itemId > 0 ? itemId : -1;
    }

    public int getQuantity()
    {
        return offer().getQuantitySold();
    }

    private GrandExchangeOffer offer()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getGrandExchangeOffers()[slot - 1]);
    }

    public static GrandExchangeSlot getBySlot(int slot)
    {
        for(GrandExchangeSlot s : GrandExchangeSlot.values()) {
            if(s.getSlot() == slot) {
                return s;
            }
        }
        return null;
    }
}