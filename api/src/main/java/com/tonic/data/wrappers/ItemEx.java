package com.tonic.data.wrappers;

import com.tonic.Static;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.abstractions.Identifiable;
import com.tonic.data.wrappers.abstractions.Interactable;
import com.tonic.util.TextUtil;
import lombok.*;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

import java.awt.*;

@Getter
@RequiredArgsConstructor
public class ItemEx implements Interactable, Identifiable
{
    private final Item item;
    private final int slot;
    private String[] actions = null;

    @Override
    public int getId() {
        return item.getId();
    }

    public boolean isNoted() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getNote()) == 799;
    }

    public boolean isPlaceholder() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getPlaceholderTemplateId() >= 0);
    }

    public int getCanonicalId() {
        ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
        return Static.invoke(() -> itemManager.canonicalize(item.getId()));
    }

    public int getLinkedNoteId() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getLinkedNoteId());
    }

    public String getName() {
        Client client = Static.getClient();
        return Static.invoke(() -> TextUtil.sanitize(client.getItemDefinition(item.getId()).getName()));
    }

    public int getQuantity() {
        return item.getQuantity();
    }

    @Override
    public void interact(String... action) {
        InventoryAPI.interact(this, action);
    }

    @Override
    public void interact(int action) {
        InventoryAPI.interact(this, action);
    }

    public String[] getActions()
    {
        if(actions != null)
            return actions;
        if(item == null)
            return new String[0];
        actions = Static.invoke(() -> {
            Client client = Static.getClient();
            ItemComposition itemComp = client.getItemDefinition(item.getId());
            return itemComp.getInventoryActions();
        });
        return actions;
    }

    private static final int[] EQUIP_OP_PARAMS = {
            451, // OP1
            452, // OP2
            453, // OP3
            454, // OP4
            455, // OP5
            456, // OP6
            457, // OP7
            458  // OP8
    };
    /**
     * Resolve an **equipped item action index** by reading ItemComposition param opcodes.
     * TODO: Fix for equipmentitem subops (Con cape, Max cape, Diary cape, ..)
     * see <a href="https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/ParamID.java#L41">here</a> for params
     * @param item item
     * @param option option
     * @return int index
     */
    public static int getEquippedActionIndex(ItemEx item, String option)
    {
        String op = option.toLowerCase();
        switch (op) {
            case "remove":
            case "unequip":
                return 1;
            case "examine":
                return 10;
        }
        return Static.invoke(() -> {
            ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
            ItemComposition comp = itemManager.getItemComposition(item.getId());
            for (int i = 0; i < EQUIP_OP_PARAMS.length; i++)
            {
                String action = comp.getStringValue(EQUIP_OP_PARAMS[i]);
                if (action == null)
                    continue;
                if (action.toLowerCase().contains(op))
                {
                    return i + 2;
                }
            }
            return -1;
        });
    }

    public boolean hasAction(String action)
    {
        String[] actions = getActions();
        if(actions == null)
            return false;
        for(String a : actions)
        {
            if(a != null && a.equalsIgnoreCase(action))
                return true;
        }
        return false;
    }

    public boolean hasActionContains(String actionPart)
    {
        String[] actions = getActions();
        if(actions == null)
            return false;
        for(String a : actions)
        {
            if(a != null && a.toLowerCase().contains(actionPart.toLowerCase()))
                return true;
        }
        return false;
    }

    public Widget getWidget() {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            Widget inventory = client.getWidget(InterfaceID.Inventory.ITEMS);
            if(inventory == null)
            {
                return null;
            }
            return inventory.getChild(getSlot());
        });
    }

    public Shape getClickBox()
    {
        Widget w = getWidget();
        if(w == null)
            return null;
        return w.getBounds();
    }

    public int getShopPrice() {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getPrice());
    }

    public long getGePrice()
    {
        ItemManager itemManager = Static.getInjector().getInstance(ItemManager.class);
        int id = itemManager.canonicalize(item.getId());
        if (id == ItemID.COINS)
        {
            return getQuantity();
        }
        else if (id == ItemID.PLATINUM)
        {
            return getQuantity() * 1000L;
        }

        ItemComposition itemDef = itemManager.getItemComposition(id);
        // Only check prices for things with store prices
        if (itemDef.getPrice() <= 0)
        {
            return 0;
        }

        return itemManager.getItemPrice(id);
    }

    public int getHighAlchValue()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getItemDefinition(item.getId()).getHaPrice());
    }

    public int getLowAlchValue()
    {
        return (int) Math.floor(getHighAlchValue() * 0.6);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other == null || getClass() != other.getClass())
        {
            return false;
        }

        ItemEx that = (ItemEx) other;
        return this.getSlot() == that.getSlot()
            && this.getId() == that.getId()
            && this.getQuantity() == that.getQuantity();
    }
}
