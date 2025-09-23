package com.tonic.queries;

import com.tonic.Static;
import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.data.ItemContainerEx;
import com.tonic.data.ItemEx;
import com.tonic.data.ShopID;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import org.apache.commons.lang3.ArrayUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InventoryQuery extends AbstractQuery<ItemEx, InventoryQuery>
{
    public static InventoryQuery fromContainer(ItemContainerEx itemContainer)
    {
        List<ItemEx> cache;
        if(itemContainer != null)
            cache = Static.invoke(() ->
                    itemContainer.getItems().stream()
                            .filter(i -> i.getId() != -1)
                            .collect(Collectors.toList())
            );
        else
            cache = new ArrayList<>();

        return new InventoryQuery(cache);
    }

    public static InventoryQuery fromContainer(ItemContainer itemContainer)
    {
        return fromInventoryId(itemContainer.getId());
    }

    @SuppressWarnings("deprecation")
    public static InventoryQuery fromInventoryId(InventoryID inventoryId)
    {
        return fromInventoryId(inventoryId.getId());
    }

    public static InventoryQuery fromInventoryId(int inventoryId)
    {
        List<ItemEx> cache;
        ItemContainerEx itemContainer = new ItemContainerEx(inventoryId);
        if(!itemContainer.getItems().isEmpty())
            cache = Static.invoke(() ->
                    itemContainer.getItems().stream()
                            .filter(i -> i.getId() != -1)
                            .collect(Collectors.toList())
            );
        else
            cache = new ArrayList<>();

        return new InventoryQuery(cache);
    }

    public static InventoryQuery fromShopId(ShopID inventoryId)
    {
        if(inventoryId == null)
        {
            return new InventoryQuery(new ArrayList<>());
        }

        List<ItemEx> cache;
        ItemContainerEx itemContainer = new ItemContainerEx(inventoryId.getItemContainerId());
        if(!itemContainer.getItems().isEmpty())
            cache = Static.invoke(() ->
                    itemContainer.getItems().stream()
                            .filter(i -> i.getId() != -1)
                            .collect(Collectors.toList())
            );
        else
            cache = new ArrayList<>();

        return new InventoryQuery(cache);
    }

    public InventoryQuery(List<ItemEx> cache) {
        super(cache);
    }

    public InventoryQuery withId(int... id)
    {
        return removeIf(o -> !ArrayUtils.contains(id, o.getId()));
    }

    public InventoryQuery withCanonicalId(int... id)
    {
        return removeIf(o -> !ArrayUtils.contains(id, o.getCanonicalId()));
    }

    public InventoryQuery withName(String... name)
    {
        return removeIf(o -> !ArrayUtils.contains(name, o.getName()));
    }

    public InventoryQuery greaterThanShopPrice(int price)
    {
        return removeIf(o -> o.getShopPrice() <= price);
    }

    public InventoryQuery lessThanShopPrice(int price)
    {
        return removeIf(o -> o.getShopPrice() >= price);
    }

    public InventoryQuery greaterThanGePrice(int price)
    {
        return removeIf(o -> o.getGePrice() <= price);
    }

    public InventoryQuery lessThanGePrice(int price)
    {
        return removeIf(o -> o.getGePrice() >= price);
    }

    public InventoryQuery greaterThanHighAlchValue(int value)
    {
        return removeIf(o -> o.getHighAlchValue() <= value);
    }

    public InventoryQuery lessThanHighAlchValue(int value)
    {
        return removeIf(o -> o.getHighAlchValue() >= value);
    }

    public InventoryQuery greaterThanLowAlchValue(int value)
    {
        return removeIf(o -> o.getLowAlchValue() <= value);
    }

    public InventoryQuery lessThanLowAlchValue(int value)
    {
        return removeIf(o -> o.getLowAlchValue() >= value);
    }

    public InventoryQuery withNameContains(String namePart)
    {
        return removeIf(o -> !o.getName().toLowerCase().contains(namePart.toLowerCase()));
    }

    public InventoryQuery withNameMatches(String namePart)
    {
        return removeIf(o -> !WildcardMatcher.matches(namePart.toLowerCase(), Text.removeTags(o.getName().toLowerCase())));
    }

    public InventoryQuery withAction(String action)
    {
        return removeIf(o -> !o.hasAction(action));
    }

    public InventoryQuery withActionContains(String actionPart)
    {
        return removeIf(o -> !o.hasActionContains(actionPart));
    }

    public InventoryQuery fromSlot(int... slots)
    {
        return removeIf(i -> !ArrayUtils.contains(slots, i.getSlot()));
    }

    /**
     * Get total quantity - terminal operation
     */
    public int getQuantity() {
        return aggregate(stream ->
                stream.mapToInt(ItemEx::getQuantity).sum()
        );
    }

    /**
     * Get total GE value - terminal operation
     */
    public long getTotalGeValue() {
        return aggregate(stream ->
                stream.mapToLong(item -> item.getQuantity() * item.getGePrice()).sum()
        );
    }

    /**
     * Get total shop value - terminal operation
     */
    public int getTotalShopValue() {
        return aggregate(stream ->
                stream.mapToInt(item -> item.getQuantity() * item.getShopPrice()).sum()
        );
    }

    /**
     * Get total high alch value - terminal operation
     */
    public int getTotalHighAlchValue() {
        return aggregate(stream ->
                stream.mapToInt(item -> item.getQuantity() * item.getHighAlchValue()).sum()
        );
    }
}
