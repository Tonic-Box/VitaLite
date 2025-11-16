package com.tonic.api.handlers;

import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.SceneAPI;
import com.tonic.api.widgets.BankAPI;
import com.tonic.data.LayoutView;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.data.locatables.BankLocations;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.ClickManager;
import com.tonic.util.ClickManagerUtil;
import com.tonic.util.Location;
import com.tonic.util.handler.AbstractHandlerBuilder;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.InterfaceID;

import javax.annotation.Nullable;

/**
 * A handler builder for banking actions.
 */
public class BankBuilder extends AbstractHandlerBuilder<BankBuilder>
{
    /**
     * Creates a new instance of the BankBuilder.
     *
     * @return A new BankBuilder instance.
     */
    public static BankBuilder get()
    {
        return new BankBuilder();
    }

    /**
     * Walks to and opens the nearest bank.
     *
     * @return The current BankBuilder instance for chaining.
     */
    public BankBuilder open()
    {
        return open(null);
    }

    /**
     * Walks to and opens the specified bank location.
     *
     * @param bankLocation The bank location to open.
     * @return BankBuilder instance
     */
    public BankBuilder open(@Nullable BankLocations bankLocation)
    {
        int step2 = currentStep;
        add(context -> {
            NpcEx banker = new NpcQuery()
                    .withNameContains("Banker")
                    .keepIf(n -> SceneAPI.losTileNextTo(n.getWorldPoint()) != null)
                    .nearest();
            if(banker != null)
            {
                ClickManagerUtil.queueClickBox(banker);
                NpcAPI.interact(banker, 2);
                ClickManager.clearClickBox();
                return step2 + 3;
            }
            TileObjectEx bank = new TileObjectQuery()
                    .withNamesContains("Bank booth", "Bank chest")
                    .sortNearest()
                    .first();
            if(bank != null && SceneAPI.losTileNextTo(bank.getWorldPoint()) != null)
            {
                ClickManagerUtil.queueClickBox(bank);
                if(bank.getName().contains("Bank booth"))
                    TileObjectAPI.interact(bank, 1);
                else
                    TileObjectAPI.interact(bank, 0);
                ClickManager.clearClickBox();
                return step2 + 3;
            }
            return step2 + 1;
        });
        walkToWorldAreaSupplier(() -> bankLocation != null ? bankLocation.getArea() : BankLocations.getNearest().getArea());
        int step = currentStep;
        add(context -> {
            if(BankAPI.isOpen())
            {
                return step + 1;
            }
            NpcEx banker = new NpcQuery()
                    .withNameContains("Banker")
                    .keepIf(n -> SceneAPI.losTileNextTo(n.getWorldPoint()) != null)
                    .nearest();
            if(banker != null)
            {
                ClickManagerUtil.queueClickBox(banker);
                NpcAPI.interact(banker, 2);
                ClickManager.clearClickBox();
                return step + 1;
            }

            TileObjectEx bank = new TileObjectQuery()
                    .withNamesContains("Bank booth", "Bank chest")
                    .sortNearest()
                    .first();
            Client client = Static.getClient();
            if(bank != null && bank.getWorldPoint().distanceTo(client.getLocalPlayer().getWorldLocation())  < 10)
            {
                ClickManagerUtil.queueClickBox(bank);
                if(bank.getName().contains("Bank booth"))
                    TileObjectAPI.interact(bank, 1);
                else
                    TileObjectAPI.interact(bank, 0);
                ClickManager.clearClickBox();
                return step + 1;
            }
            return step;
        });
        addDelayUntil(BankAPI::isOpen);
        return this;
    }

    /**
     * Deposits the entire inventory into the bank.
     * @return BankBuilder instance
     */
    public BankBuilder depositInventory()
    {
        add(() -> {
            ClickManagerUtil.queueClickBoxInterface(InterfaceID.Bankmain.DEPOSITINV);
            BankAPI.depositAll();;
            ClickManager.clearClickBox();
        });
        return this;
    }

    /**
     * Deposits all equipped items into the bank.
     * @return BankBuilder instance
     */
    public BankBuilder depositEquipment()
    {
        add(() -> {
            ClickManagerUtil.queueClickBoxInterface(InterfaceID.Bankmain.DEPOSITWORN);
            BankAPI.depositEquipment();
            ClickManager.clearClickBox();
        });
        return this;
    }

    /**
     * Withdraws the specified items from the bank.
     *
     * @param noted Whether to withdraw the items as notes.
     * @param items The items to withdraw.
     * @return BankBuilder instance
     */
    public BankBuilder withdraw(boolean noted, BankItem... items)
    {
        add(() -> {
            ClickManagerUtil.queueClickBoxInterface(InterfaceID.Bankmain.ITEMS);
            for(BankItem item : items)
            {
                BankAPI.withdraw(item.itemId, item.amount, noted);
            }
            ClickManager.clearClickBox();
        });
        return this;
    }

    /**
     * Deposits the specified items into the bank.
     *
     * @param items The items to deposit.
     * @return BankBuilder instance
     */
    public BankBuilder deposit(BankItem... items)
    {
        add(() -> {
            ClickManagerUtil.queueClickBox(LayoutView.SIDE_MENU.getWidget());
            for(BankItem item : items)
            {
                BankAPI.deposit(item.itemId, item.amount);
            }
            ClickManager.clearClickBox();
        });
        return this;
    }

    /**
     * Uses an item from the banks inventory interface.
     *
     * @param itemId The ID of the item to use.
     * @return BankBuilder instance
     */
    public BankBuilder use(int itemId)
    {
        add(() -> {
            ClickManagerUtil.queueClickBox(LayoutView.SIDE_MENU.getWidget());
            BankAPI.use(itemId);
            ClickManager.clearClickBox();
        });
        return this;
    }

    /**
     * Uses an item from the banks inventory interface, guessing the next slot.
     *
     * @param itemId The ID of the item to use.
     * @return BankBuilder instance
     */
    public BankBuilder useGuessNextSlot(int itemId)
    {
        add(() -> {
            ClickManagerUtil.queueClickBox(LayoutView.SIDE_MENU.getWidget());
            BankAPI.useGuessNextSlot(itemId);
            ClickManager.clearClickBox();
        });
        return this;
    }

    /**
     * Represents an item in the bank/bank inv with its ID and amount.
     */
    @RequiredArgsConstructor
    public static class BankItem
    {
        /**
         * Creates an array of BankItem from pairs of item IDs and quantities.
         *
         * @param itemIdQuantityPairs Pairs of item IDs and quantities.
         * @return An array of BankItem.
         */
        public static BankItem[] of(int... itemIdQuantityPairs)
        {
            if(itemIdQuantityPairs.length % 2 != 0)
                throw new IllegalArgumentException("Item ID and quantity pairs must be even in number.");

            BankItem[] items = new BankItem[itemIdQuantityPairs.length / 2];
            int index = 0;
            for(int i = 0; i < itemIdQuantityPairs.length; i += 2)
            {
                items[index++] = new BankItem(itemIdQuantityPairs[i], itemIdQuantityPairs[i + 1]);
            }
            return items;
        }

        private final int itemId;
        private final int amount;
    }
}
