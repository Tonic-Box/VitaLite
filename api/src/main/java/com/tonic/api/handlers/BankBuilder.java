package com.tonic.api.handlers;

import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.BankAPI;
import com.tonic.data.TileObjectEx;
import com.tonic.data.locatables.BankLocations;
import com.tonic.queries.NpcQuery;
import com.tonic.queries.TileObjectQuery;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.util.Location;
import com.tonic.util.handler.HandlerBuilder;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

public class BankBuilder extends HandlerBuilder
{
    public static DialogueBuilder get()
    {
        return new DialogueBuilder();
    }

    private int currentStep = 0;

    public BankBuilder open()
    {
        BankLocations nearestBank = BankLocations.getNearest();
        return open(nearestBank);
    }
    public BankBuilder open(BankLocations bankLocation)
    {
        WalkerPath pathToBank = bankLocation.pathTo();
        addDelayUntil(currentStep++, () -> !pathToBank.step());
        int step = currentStep;
        add(currentStep++, context -> {
            if(BankAPI.isOpen())
            {
                return END_EXECUTION;
            }
            NPC banker = new NpcQuery()
                    .withNameContains("Banker")
                    .keepIf(n -> Location.losTileNextTo(n.getWorldLocation()) != null)
                    .nearest();
            if(banker != null)
            {
                WorldPoint dest = Location.losTileNextTo(banker.getWorldLocation());
                Walker.walkTo(dest);
                NpcAPI.interact(banker, 2);
                return step;
            }

            TileObjectEx bank = new TileObjectQuery<>()
                    .withNamesContains("Bank booth", "Bank chest")
                    .sortNearest()
                    .first();
            Client client = Static.getClient();
            if(bank != null && bank.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation())  < 10)
            {
                context.put("bankObject", bank);
                MovementAPI.walkToWorldPoint(bank.getWorldLocation());
                return step + 1;
            }
            return step;
        });
        addDelayUntil(currentStep++, () -> !MovementAPI.isMoving());
        add(currentStep++, context -> {
            TileObjectEx bank = context.get("bankObject");
            if(bank.getName().contains("Bank booth"))
                TileObjectAPI.interact(bank, 1);
            else
                TileObjectAPI.interact(bank, 0);
        });
        addDelayUntil(currentStep++, BankAPI::isOpen);
        return this;
    }

    public BankBuilder depositInventory()
    {
        add(currentStep++, BankAPI::depositAll);
        return this;
    }

    public BankBuilder depositEquipment()
    {
        add(currentStep++, BankAPI::depositEquipment);
        return this;
    }

    public BankBuilder withdraw(boolean noted, BankItem... items)
    {
        add(currentStep++, () -> {
            for(BankItem item : items)
            {
                BankAPI.withdraw(item.itemId, item.amount, noted);
            }
        });
        return this;
    }

    public BankBuilder deposit(BankItem... items)
    {
        add(currentStep++, () -> {
            for(BankItem item : items)
            {
                BankAPI.deposit(item.itemId, item.amount);
            }
        });
        return this;
    }

    public BankBuilder use(int itemId)
    {
        add(currentStep++, () -> BankAPI.use(itemId));
        return this;
    }

    public BankBuilder useGuessNextSlot(int itemId)
    {
        add(currentStep++, () -> BankAPI.useGuessNextSlot(itemId));
        return this;
    }

    @RequiredArgsConstructor
    public static class BankItem
    {
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
