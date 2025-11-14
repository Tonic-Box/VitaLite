package com.tonic.api.loadouts;

import com.tonic.api.loadouts.item.Loadout;
import com.tonic.api.loadouts.item.LoadoutItem;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.data.EquipmentSlot;
import com.tonic.data.wrappers.ItemEx;

import java.util.*;

/**
 * A high-level loadout API designed for use in conjunction with equipment
 */
public class EquipmentLoadout extends Loadout
{

  public EquipmentLoadout(String name)
  {
    super(name);
  }

  @Override
  protected List<ItemEx> getLiveItems()
  {
    return EquipmentAPI.getAll();
  }

  @Override
  public List<LoadoutItem> getRequiredItems()
  {
    List<LoadoutItem> missing = new ArrayList<>();
    for (LoadoutItem entry : this)
    {
      if (!entry.isCarried() && !entry.isWorn())
      {
        missing.add(entry);
      }
    }

    return missing;
  }

  @Override
  public void add(LoadoutItem item)
  {
    if (item.getEquipmentSlot() == null)
    {
      throw new LoadoutException("Failed to add " + item.getIdentifier() + " as it is not registered to an EquipmentSlot");
    }

    if (item.getAmount() == 0)
    {
      throw new LoadoutException("Invalid quantity specified for " + item.getIdentifier());
    }

    if (items.values().stream().anyMatch(other -> other.getEquipmentSlot() == item.getEquipmentSlot()))
    {
      throw new LoadoutException("Loadout already contains mapping for slot " + item.getEquipmentSlot());
    }

    items.put(item.getIdentifier(), item);
  }

  public LoadoutItem get(EquipmentSlot slot)
  {
    return items.values().stream().filter(item -> item.getEquipmentSlot() == slot).findFirst().orElse(null);
  }

  public LoadoutItem remove(EquipmentSlot slot)
  {
    LoadoutItem existing = get(slot);
    if (existing == null)
    {
      return null;
    }

    return remove(existing.getIdentifier());
  }
}
