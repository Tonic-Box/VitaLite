package com.tonic.api.handlers;

import com.tonic.api.entities.PlayerAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.TradeAPI;
import static com.tonic.api.widgets.TradeAPI.*;

import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.queries.PlayerQuery;
import com.tonic.util.handler.AbstractHandlerBuilder;
import com.tonic.util.handler.StepContext;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for handling player trades.
 */
public class TradeBuilder extends AbstractHandlerBuilder<TradeBuilder>
{
    /**
     * Creates a new instance of TradeBuilder.
     *
     * @return A new TradeBuilder instance.
     */
    public static TradeBuilder get()
    {
        return new TradeBuilder();
    }

    /**
     * Trades with a player by name, offering specified items.
     *
     * CONTEXT:
     * - "ORIGINAL": List of TradeItem representing the original inventory before trade.
     * - "RECEIVED": List of TradeItem representing the items received from the trade.
     * - "OFFERED": List of TradeItem representing the items offered in the trade.
     * - "RESULT": String indicating "SUCCESS" or "FAILURE" based on trade validation.
     *
     * @param name    The name of the player to trade with.
     * @param timeout The timeout in milliseconds to wait for the trade screen to open.
     * @param items   The items to offer in the trade.
     * @return The current TradeBuilder instance for chaining.
     */
    public TradeBuilder tradePlayer(String name, int timeout, TradeItem... items)
    {
        int step = currentStep;
        add(context -> {
            context.put("ORIGINAL", TradeItem.of(InventoryAPI.getItems(), false));
            PlayerEx player = new PlayerQuery()
                    .withName(name)
                    .first();
            if(player == null)
            {
                return step;
            }
            PlayerAPI.interact(player, "Trade");
            return step + 1;
        });
        addDelayUntil(timeout, TradeAPI::isOnMainScreen, () -> {});
        int step2 = currentStep + 1;
        add(() -> {
            if(!isOpen())
            {
                return END_EXECUTION;
            }
            if(items == null)
            {
                return step2;
            }
            for(TradeItem item : items)
            {
                TradeAPI.offer(item.itemId, item.amount);
            }
            return step2;
        });
        addDelayUntil(TradeAPI::isAcceptedByOther);
        addDelayUntil(context -> {
            if(!isOpen())
            {
                return true;
            }
            if(!isAcceptedByPlayer())
            {
                context.put("RECEIVED", TradeItem.of(getReceivingItems(), false));
                context.put("OFFERED", TradeItem.of(getOfferingItems(), true));
                accept();
            }
            return !isOnConfirmationScreen();
        });
        addDelayUntil(context -> {
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

        if(original == null || received == null || offered == null)
        {
            return false;
        }

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

    /**
     * Represents an item involved in a trade, including its ID and amount.
     */
    @RequiredArgsConstructor
    public static class TradeItem
    {
        /**
         * Creates an array of TradeItem from pairs of item IDs and quantities.
         *
         * @param itemIdQuantityPairs Pairs of item IDs and quantities.
         * @return An array of TradeItem.
         */
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
            int state = negate ? -1 : 1;
            return items.stream()
                    .map(item -> new TradeItem(item.getId(), item.getQuantity() * state))
                    .collect(Collectors.toList());
        }

        private final int itemId;
        private final int amount;
    }
}
