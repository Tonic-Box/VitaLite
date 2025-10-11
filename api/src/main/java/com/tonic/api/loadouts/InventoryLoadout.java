package com.tonic.api.loadouts;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A high-level loadout API designed for use in conjunction with banking
 */
public class InventoryLoadout implements Iterable<LoadoutItem>
{

  private static final int CAPACITY = 28;

  private final String name;
  private final Map<String, LoadoutItem> items;

  public InventoryLoadout(String name)
  {
    this.name = name.toLowerCase(); //TODO if we support serialization, verify that the name is a valid identifier
    this.items = new LinkedHashMap<>();
  }

  public String getName()
  {
    return name;
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

  @Override
  public Iterator<LoadoutItem> iterator()
  {
    return items.values().iterator();
  }
}
