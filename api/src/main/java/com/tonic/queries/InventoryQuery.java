package com.tonic.queries;

import com.tonic.Static;
import com.tonic.queries.abstractions.AbstractQuery;
import com.tonic.data.wrappers.ItemContainerEx;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.trading.Shop;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;
import org.apache.commons.lang3.ArrayUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Query class for searching and filtering items in an inventory.
 */
public class InventoryQuery extends AbstractQuery<ItemEx, InventoryQuery>
{
    /**
     * Create a query from an ItemContainerEx instance
     * @param itemContainer The item container to query from
     * @return A new InventoryQuery instance
     */
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

    /**
     * Create a query from a standard ItemContainer instance
     * @param itemContainer The item container to query from
     * @return A new InventoryQuery instance
     */
    public static InventoryQuery fromContainer(ItemContainer itemContainer)
    {
        return fromInventoryId(itemContainer.getId());
    }

    /**
     * Create a query from an InventoryID
     * @param inventoryId The inventory ID to query from
     * @return A new InventoryQuery instance
     */
    @SuppressWarnings("deprecation")
    public static InventoryQuery fromInventoryId(InventoryID inventoryId)
    {
        return fromInventoryId(inventoryId.getId());
    }

    /**
     * Create a query from an inventory ID
     * @param inventoryId The inventory ID to query from
     * @return A new InventoryQuery instance
     */
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

    /**
     * Create a query from a Shop enum
     * @param shop The shop to query from
     * @return A new InventoryQuery instance
     */
    public static InventoryQuery fromShop(Shop shop)
    {
        if (shop == null) {
            return new InventoryQuery(new ArrayList<>());
        }

        List<ItemEx> cache;
        ItemContainerEx itemContainer = new ItemContainerEx(shop.getInventoryId());

        if (!itemContainer.getItems().isEmpty())
            cache = Static.invoke(() ->
                    itemContainer.getItems().stream()
                            .filter(i -> i.getId() != -1)
                            .collect(Collectors.toList())
            );
        else
            cache = new ArrayList<>();

        return new InventoryQuery(cache);
    }

    /**
     * Create a query from the currently open shop
     * @return A new InventoryQuery instance, or empty if no shop is open
     */
    public static InventoryQuery fromCurrentShop()
    {
        Shop currentShop = Shop.getCurrent();
        return fromShop(currentShop);
    }

    /**
     * Constructor to initialize the query with a list of items.
     * @param cache The initial list of items to query from.
     */
    public InventoryQuery(List<ItemEx> cache) {
        super(cache);
    }

    /**
     * Filter items by their IDs.
     * @param id id
     * @return InventoryQuery
     */
    public InventoryQuery withId(int... id)
    {
        return removeIf(o -> !ArrayUtils.contains(id, o.getId()));
    }

    /**
     * Filter items by their canonical IDs.
     * @param id id
     * @return InventoryQuery
     */
    public InventoryQuery withCanonicalId(int... id)
    {
        return removeIf(o -> !ArrayUtils.contains(id, o.getCanonicalId()));
    }

    /**
     * Filter items by their names.
     * @param name name
     * @return InventoryQuery
     */
    public InventoryQuery withName(String... name)
    {
        return removeIf(o -> !ArrayUtils.contains(name, o.getName()));
    }

    /**
     * Filter items with a shop price greater than the specified value.
     * @param price price
     * @return InventoryQuery
     */
    public InventoryQuery greaterThanShopPrice(int price)
    {
        return removeIf(o -> o.getShopPrice() <= price);
    }

    /**
     * Filter items with a shop price less than the specified value.
     * @param price price
     * @return InventoryQuery
     */
    public InventoryQuery lessThanShopPrice(int price)
    {
        return removeIf(o -> o.getShopPrice() >= price);
    }

    /**
     * Filter items with a GE price greater than the specified value.
     * @param price price
     * @return InventoryQuery
     */
    public InventoryQuery greaterThanGePrice(int price)
    {
        return removeIf(o -> o.getGePrice() <= price);
    }

    /**
     * Filter items with a GE price less than the specified value.
     * @param price price
     * @return InventoryQuery
     */
    public InventoryQuery lessThanGePrice(int price)
    {
        return removeIf(o -> o.getGePrice() >= price);
    }

    /**
     * Filter items with a high alch value greater than the specified value.
     * @param value value
     * @return InventoryQuery
     */
    public InventoryQuery greaterThanHighAlchValue(int value)
    {
        return removeIf(o -> o.getHighAlchValue() <= value);
    }

    /**
     * Filter items with a high alch value less than the specified value.
     * @param value value
     * @return InventoryQuery
     */
    public InventoryQuery lessThanHighAlchValue(int value)
    {
        return removeIf(o -> o.getHighAlchValue() >= value);
    }

    /**
     * Filter items with a low alch value greater than the specified value.
     * @param value value
     * @return InventoryQuery
     */
    public InventoryQuery greaterThanLowAlchValue(int value)
    {
        return removeIf(o -> o.getLowAlchValue() <= value);
    }

    /**
     * Filter items with a low alch value less than the specified value.
     * @param value value
     * @return InventoryQuery
     */
    public InventoryQuery lessThanLowAlchValue(int value)
    {
        return removeIf(o -> o.getLowAlchValue() >= value);
    }

    /**
     * Filter items whose names contain the specified substring (case-insensitive).
     * @param namePart Substring to search for in item names
     * @return InventoryQuery
     */
    public InventoryQuery withNameContains(String namePart)
    {
        return removeIf(o -> !o.getName().toLowerCase().contains(namePart.toLowerCase()));
    }

    /**
     * Filter items whose names match the specified wildcard pattern (case-insensitive).
     * @param namePart Wildcard pattern to match against item names
     * @return InventoryQuery
     */
    public InventoryQuery withNameMatches(String namePart)
    {
        return removeIf(o -> !WildcardMatcher.matches(namePart.toLowerCase(), Text.removeTags(o.getName().toLowerCase())));
    }

    /**
     * Filter items by action.
     * @param action action
     * @return InventoryQuery
     */
    public InventoryQuery withAction(String action)
    {
        return removeIf(o -> !o.hasAction(action));
    }

    /**
     * Filter items whose actions contain the specified substring (case-insensitive).
     * @param actionPart Substring to search for in item actions
     * @return InventoryQuery
     */
    public InventoryQuery withActionContains(String actionPart)
    {
        return removeIf(o -> !o.hasActionContains(actionPart));
    }

    /**
     * Filter items by slots.
     * @param slots slots
     * @return InventoryQuery
     */
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

    /**
     * Count the total quantity of items left in the list - terminal operation
     * @return The total quantity of items
     */
    public int count()
    {
        List<ItemEx> list = collect();
        int count = 0;
        for(ItemEx item : list)
        {
            count += item.getQuantity();
        }
        return count;
    }

    /**
     * @return A list of items, only keeping a single instance of each item. Filtering out occurences of the same ID
     */
    public List<ItemEx> unique()
    {
        List<ItemEx> results = collect();
        Set<Integer> unique = new HashSet<>();
        results.removeIf(item -> !unique.add(item.getId()));
        return results;
    }
}
