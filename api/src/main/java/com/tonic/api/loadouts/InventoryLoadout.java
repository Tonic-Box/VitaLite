package com.tonic.api.loadouts;

import com.tonic.api.loadouts.item.ItemDepletionListener;
import com.tonic.api.loadouts.item.LoadoutItem;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.ItemEx;

import java.util.*;

/**
 * A high-level loadout API designed for use in conjunction with banking and resupplying.
 */
public class InventoryLoadout implements Iterable<LoadoutItem>
{

  private static final int CAPACITY = 28;

  private final String name;
  private final Map<String, LoadoutItem> items;

  private ItemDepletionListener itemDepletionListener;

  public InventoryLoadout(String name)
  {
    this.name = name.toLowerCase(); //TODO if we support serialization, verify that the name is a valid identifier
    this.items = new LinkedHashMap<>();
  }

  public String getName()
  {
    return name;
  }

  /**
   * @return A listener to trigger when an item is attempted to be withdrawn but is not available in the desired quantity.
   * This can be utilised to trigger states in your plugin such as restocking.
   */
  public ItemDepletionListener getItemDepletionListener()
  {
    return itemDepletionListener;
  }

  public void setItemDepletionListener(ItemDepletionListener itemDepletionListener)
  {
    this.itemDepletionListener = itemDepletionListener;
  }

  public void add(LoadoutItem item)
  {
    if (!isEligible(item))
    {
      throw new LoadoutException("Failed to add " + item.getIdentifier() + " as it would cause loadout to overflow");
    }

    if (item.getAmount() == 0)
    {
      throw new LoadoutException("Invalid quantity specified for " + item.getIdentifier());
    }

    if (item.isStackable())
    {
      items.put(item.getIdentifier(), item);
      return;
    }

    int usedSpace = item.getAmount();
    int availableSpace = CAPACITY - items.size();
    if (usedSpace <= availableSpace)
    {
      items.put(item.getIdentifier(), item);
      return;
    }

    //should never reach here?
    throw new LoadoutException("Failed to add " + item.getIdentifier() + " as it would cause loadout to overflow");
  }

  public int getSlotCount(LoadoutItem entry)
  {
    return entry.isStackable() || entry.isNoted() ? 1 : entry.getAmount();
  }

  public int getUsedSlots()
  {
    return items.values().stream().mapToInt(this::getSlotCount).sum();
  }

  private boolean isEligible(LoadoutItem entry)
  {
    int available = CAPACITY - getUsedSlots();
    if (entry.isStackable())
    {
      return available > 0;
    }

    return available >= entry.getAmount();
  }

  public LoadoutItem get(String key)
  {
    return items.get(key);
  }

  public LoadoutItem remove(String key)
  {
    return items.remove(key);
  }

  public boolean isFulfilled()
  {
    for (LoadoutItem item : getRequiredItems())
    {
      if (!item.isOptional())
      {
        return false;
      }
    }

    return getCarriedExcessItems().isEmpty();
  }

  /**
   * @return A List containing the remainder of items that we still need
   */
  public List<LoadoutItem> getRequiredItems() {
    List<LoadoutItem> missing = new ArrayList<>();
    for (LoadoutItem entry : this)
    {
      if (!entry.isCarried())
      {
        missing.add(entry);
      }
    }

    return missing;
  }

  /**
   * @return A List of foreign items that are currently in the inventory.
   * A foreign item includes anything that isn't in this loadout
   */
  public List<ItemEx> getCarriedForeignItems() {
    List<ItemEx> invalid = new LinkedList<>(InventoryAPI.getItems());
    List<ItemEx> valid = new ArrayList<>();
    for (LoadoutItem item : this)
    {
      valid.addAll(item.getCarried());
    }

    invalid.removeIf(valid::contains);
    return invalid;
  }

  /**
   * @return A List of items that are not foreign to this loadout, but are present in excess quantities
   */
  public List<LoadoutItem> getCarriedExcessItems() {
    List<LoadoutItem> excess = new ArrayList<>();
    for (LoadoutItem item : this)
    {
      List<ItemEx> present = item.getCarried();
      if (present.isEmpty())
      {
        continue;
      }

      ItemEx carried = present.get(0);
      int count = item.isStackable() ? carried.getQuantity() : present.size();
      if (count <= item.getAmount())
      {
        continue;
      }

      excess.add(item);
    }

    return excess;
  }

  @Override
  public Iterator<LoadoutItem> iterator()
  {
    return items.values().iterator();
  }
}
