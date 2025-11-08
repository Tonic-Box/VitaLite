package com.tonic.api.handlers;

import com.tonic.api.widgets.BankAPI;
import com.tonic.util.handler.HandlerBuilder;
import lombok.RequiredArgsConstructor;

public class BankBuilder extends HandlerBuilder
{
    public static DialogueBuilder get()
    {
        return new DialogueBuilder();
    }

    private int currentStep = 0;

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
