package com.tonic.api.handlers;

import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.TradeAPI;
import static com.tonic.api.widgets.TradeAPI.*;

import com.tonic.data.ItemEx;
import com.tonic.queries.PlayerQuery;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepContext;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class TradeBuilder extends HandlerBuilder
{
    public static DialogueBuilder get()
    {
        return new DialogueBuilder();
    }

    private int currentStep = 0;

    public TradeBuilder tradePlayer(String name, TradeItem... items)
    {
        int step = currentStep;
        add(currentStep++, context -> {
            context.put("ORIGINAL", TradeItem.of(InventoryAPI.getItems(), false));
            Player player = new PlayerQuery()
                    .withName(name)
                    .first();
            if(player == null)
            {
                return step;
            }
            PlayerAPI.interact(player, "Trade");
            return step + 1;
        });
        addDelayUntil(currentStep++, TradeAPI::isOnMainScreen);
        add(currentStep++, () -> {
            if(items == null)
            {
                return;
            }
            for(TradeItem item : items)
            {
                TradeAPI.offer(item.itemId, item.amount);
            }
        });
        addDelayUntil(currentStep++, TradeAPI::isAcceptedByOther);
        addDelayUntil(currentStep++, context -> {
            if(!isAcceptedByPlayer())
            {
                context.put("RECEIVED", TradeItem.of(getReceivingItems(), false));
                context.put("OFFERED", TradeItem.of(getOfferingItems(), true));
                accept();
            }
            return !isOnConfirmationScreen();
        });
        addDelayUntil(currentStep++, context -> {
            if(!isAcceptedByPlayer())
            {
                accept();
            }
            if(!isOpen())
            {
                context.put("RESULT", validate(context) ? "SUCCESS" : "FAILURE");
            }
            return !isOpen();
        });

        return this;
    }

    private static boolean validate(StepContext context)
    {
        List<TradeItem> original = consolidate(context.get("ORIGINAL"));
        List<TradeItem> received = consolidate(context.get("RECEIVED"));
        List<TradeItem> offered = consolidate(context.get("OFFERED"));

        List<TradeItem> shift = new ArrayList<>();
        shift.addAll(received);
        shift.addAll(offered);
        shift = consolidate(shift);

        for(TradeItem item : original)
        {
            int netAmount = shift.stream()
                    .filter(i -> i.itemId == item.itemId)
                    .mapToInt(i -> i.amount)
                    .sum();
            if(netAmount != item.amount)
            {
                return false;
            }
        }

        return true;
    }

    private static List<TradeItem> consolidate(List<TradeItem> items) {
        return items.stream()
                .collect(Collectors.toMap(
                        item -> item.itemId,                    // Key: itemId
                        item -> item.amount,                    // Value: amount
                        Integer::sum,                           // Merge function for duplicates
                        LinkedHashMap::new))                    // Preserve order
                .entrySet().stream()
                .map(e -> new TradeItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @RequiredArgsConstructor
    public static class TradeItem
    {
        public static TradeItem[] of(int... itemIdQuantityPairs)
        {
            if(itemIdQuantityPairs.length % 2 != 0)
                throw new IllegalArgumentException("Item ID and quantity pairs must be even in number.");

            TradeItem[] items = new TradeItem[itemIdQuantityPairs.length / 2];
            int index = 0;
            for(int i = 0; i < itemIdQuantityPairs.length; i += 2)
            {
                items[index++] = new TradeItem(itemIdQuantityPairs[i], itemIdQuantityPairs[i + 1]);
            }
            return items;
        }

        private static List<TradeItem> of(List<ItemEx> items, boolean negate)
        {
            return items.stream()
                    .map(item -> new TradeItem(item.getId(), -item.getQuantity()))
                    .collect(Collectors.toList());
        }

        private final int itemId;
        private final int amount;
    }
}
