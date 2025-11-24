package com.tonic.data;

import com.tonic.Static;
import com.tonic.api.widgets.WidgetAPI;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

@Getter
public enum GrandExchangeSlot
{
    SLOT_1(InterfaceID.GeOffers.INDEX_0, 1), //, WidgetInfoExtended.GRAND_EXCHANGE_OFFER1),
    SLOT_2(InterfaceID.GeOffers.INDEX_1, 2), //, WidgetInfoExtended.GRAND_EXCHANGE_OFFER2),
    SLOT_3(InterfaceID.GeOffers.INDEX_2, 3), //, WidgetInfoExtended.GRAND_EXCHANGE_OFFER3),
    SLOT_4(InterfaceID.GeOffers.INDEX_3, 4), //, WidgetInfoExtended.GRAND_EXCHANGE_OFFER4),
    SLOT_5(InterfaceID.GeOffers.INDEX_4, 5), //, WidgetInfoExtended.GRAND_EXCHANGE_OFFER5),
    SLOT_6(InterfaceID.GeOffers.INDEX_5, 6), //, WidgetInfoExtended.GRAND_EXCHANGE_OFFER6),
    SLOT_7(InterfaceID.GeOffers.INDEX_6, 7), //, WidgetInfoExtended.GRAND_EXCHANGE_OFFER7),
    SLOT_8(InterfaceID.GeOffers.INDEX_7, 8), //, WidgetInfoExtended.GRAND_EXCHANGE_OFFER8);

    ;

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
        Client client = Static.getClient();
        return Static.invoke(() ->{
            Widget widget = client.getWidget(id);
            if(widget == null)
            {
                return false;
            }
            Widget child = widget.getChild(22);
            if(child == null || !WidgetAPI.isVisible(child))
            {
                return false;
            }
            return Integer.toString(child.getTextColor(), 16).equals("5f00");
        });
    }

    public int getItemId()
    {
        return Static.invoke(() -> {
            Widget widget = WidgetAPI.get(id);
            if(widget == null)
            {
                return -1;
            }
            Widget child = widget.getChild(18);
            if(child == null)
            {
                return -1;
            }
            return child.getItemId();
        });
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