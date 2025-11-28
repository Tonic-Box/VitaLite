package com.tonic.data.locatables.sailing;

import com.tonic.Static;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.util.TextUtil;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.util.List;

public class CargoHoldAPI
{
    public static boolean isOpen()
    {
        return WidgetAPI.isVisible(InterfaceID.SailingBoatCargohold.UNIVERSE);
    }

    public static void withdrawCrate()
    {
        if(!isOpen())
        {
            return;
        }

        List<CargoItem> items = getItems();

        for(CargoItem item : items)
        {
            if(item.getName().startsWith("Crate"))
            {
                WidgetAPI.interact(1, InterfaceID.SailingBoatCargohold.ITEMS, item.getSlot(), item.getId());
                break;
            }
        }
    }

    public static void close()
    {
        if(!isOpen())
            return;

        WidgetAPI.closeInterface();
    }

    public static List<CargoItem> getItems()
    {
        if(!isOpen())
        {
            return List.of();
        }

        return Static.invoke(() -> {
            Widget widget = WidgetAPI.get(InterfaceID.SailingBoatCargohold.ITEMS);
            if(widget == null || widget.getDynamicChildren() == null || widget.getDynamicChildren().length == 0)
            {
                return List.of();
            }

            List<CargoItem> items = new java.util.ArrayList<>();
            for(Widget child : widget.getDynamicChildren())
            {
                int itemId = child.getItemId();
                if(itemId < 0)
                {
                    continue;
                }
                String itemName = TextUtil.sanitize(child.getName());
                int quantity = child.getItemQuantity();
                int slot = child.getIndex();
                CargoItem cargoItem = new CargoItem(itemName, itemId, slot, quantity);
                items.add(cargoItem);
            }

            return items;
        });
    }

    @Getter
    public static class CargoItem
    {
        private final String name;
        private final int id;
        private final int slot;
        private final int quantity;

        public CargoItem(String name, int id, int slot, int quantity)
        {
            this.name = name;
            this.id = id;
            this.slot = slot;
            this.quantity = quantity;
        }
    }
}
