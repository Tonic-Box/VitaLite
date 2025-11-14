package com.tonic.api.loadouts;

import com.tonic.api.loadouts.item.Loadout;
import com.tonic.api.loadouts.item.LoadoutItem;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.wrappers.ItemEx;

import java.util.*;

/**
 * A high-level loadout API designed for use in conjunction with banking and resupplying.
 */
public class InventoryLoadout extends Loadout
{

  private static final int CAPACITY = 28;

  public InventoryLoadout(String name)
  {
    super(name);
  }

  @Override
  protected List<ItemEx> getLiveItems()
  {
    return InventoryAPI.getItems();
  }

  @Override
  public List<LoadoutItem> getRequiredItems()
  {
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

  @Override
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

  /**
   * Adds missing equipment items to this loadout
   * @param equipmentLoadout The loadout to add from
   */
  public void fulfill(EquipmentLoadout equipmentLoadout)
  {
    for (LoadoutItem item : equipmentLoadout.getRequiredItems())
    {
      add(item);
    }
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
}
