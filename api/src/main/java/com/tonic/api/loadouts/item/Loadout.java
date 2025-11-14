package com.tonic.api.loadouts.item;

import com.tonic.data.wrappers.ItemEx;

import java.util.*;

public abstract class Loadout implements Iterable<LoadoutItem>
{

  private final String name;
  protected final Map<String, LoadoutItem> items;

  protected transient ItemDepletionListener itemDepletionListener;

  public Loadout(String name)
  {
    this.name = name.toLowerCase();
    this.items = new LinkedHashMap<>();
  }

  public String getName()
  {
    return name;
  }

  protected abstract List<ItemEx> getLiveItems();

  /**
   * @return A List containing the remainder of items that we still need.
   * For EquipmentLoadout, an item is marked as not required if it is either in the inventory, or equipment
   */
  public abstract List<LoadoutItem> getRequiredItems();

  public abstract void add(LoadoutItem item);

  public LoadoutItem get(String key)
  {
    return items.get(key);
  }

  public LoadoutItem remove(String key)
  {
    return items.remove(key);
  }

  /**
   * @return A listener to trigger when an item is attempted to be interacted with but is not available or in the desired quantity.
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

  public boolean isFulfilled()
  {
    for (LoadoutItem item : getRequiredItems())
    {
      if (!item.isOptional())
      {
        return false;
      }
    }

    return getExcessItems().isEmpty();
  }

  /**
   * @return A List of foreign items that are currently in the inventory, or equipment if this is an EquipmentLoadout.
   * A foreign item includes anything that isn't in this loadout
   */
  public List<ItemEx> getForeignItems() {
    List<ItemEx> invalid = new LinkedList<>(getLiveItems());
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
  public List<LoadoutItem> getExcessItems() {
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
