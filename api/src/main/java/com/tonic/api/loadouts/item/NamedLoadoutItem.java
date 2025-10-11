package com.tonic.api.loadouts.item;

import com.tonic.api.loadouts.LoadoutException;
import com.tonic.data.EquipmentSlot;
import com.tonic.data.ItemEx;
import com.tonic.queries.InventoryQuery;
import net.runelite.api.gameval.InventoryID;

import java.util.Comparator;
import java.util.List;

public class NamedLoadoutItem extends LoadoutItem {

  private final String[] names;

  private NamedLoadoutItem(String identifier, int minimumAmount, int amount, boolean stackable, boolean noted, boolean optional, EquipmentSlot equipmentSlot, String[] names)
  {
    super(identifier, minimumAmount, amount, stackable, noted, optional, equipmentSlot);
    this.names = names;
  }

  /**
   * @return An array of accepted item names. The array should be ordered by the developer such that prioritized items
   * appear first. This makes it optimal to deal with homogeneous items (Infernal cape > Fire cape), and items with
   * charges or doses in their names such as potions and jewellery
   */
  public String[] getNames()
  {
    return names;
  }

  @Override
  public List<ItemEx> getCarried()
  {
    String[] preferredOrder = getNames();
    return InventoryQuery.fromInventoryId(InventoryID.INV)
        .removeIf(item -> item.isNoted() != isNoted())
        .sort(Comparator.comparingInt(item -> {
          String itemName = item.getName();
          for (int i = 0; i < preferredOrder.length; i++) {
            if (preferredOrder[i].equalsIgnoreCase(itemName)) {
              return i;
            }
          }
          return Integer.MAX_VALUE;
        })).collect();
  }

  @Override
  public List<ItemEx> getWorn()
  {
    String[] preferredOrder = getNames();
    return InventoryQuery.fromInventoryId(InventoryID.WORN)
        .sort(Comparator.comparingInt(item -> {
          String itemName = item.getName();
          for (int i = 0; i < preferredOrder.length; i++) {
            if (preferredOrder[i].equalsIgnoreCase(itemName)) {
              return i;
            }
          }
          return Integer.MAX_VALUE;
        })).collect();
  }

  @Override
  public List<ItemEx> getBanked()
  {
    String[] preferredOrder = getNames();
    return InventoryQuery.fromInventoryId(InventoryID.BANK)
        .removeIf(ItemEx::isPlaceholder)
        .sort(Comparator.comparingInt(item -> {
          String itemName = item.getName();
          for (int i = 0; i < preferredOrder.length; i++) {
            if (preferredOrder[i].equalsIgnoreCase(itemName)) {
              return i;
            }
          }
          return Integer.MAX_VALUE;
        })).collect();
  }

  public static Builder builder()
  {
    return new Builder();
  }

  public static class Builder
  {

    private String identifier;
    private int minimumAmount;
    private int amount;
    private boolean stackable;
    private boolean noted;
    private boolean optional;
    private EquipmentSlot equipmentSlot;
    private String[] names;

    public Builder identifier(String identifier)
    {
      this.identifier = identifier;
      return this;
    }

    public Builder names(String... names)
    {
      this.names = names;
      return this;
    }

    public Builder single(String name)
    {
      this.identifier = name;
      this.names = new String[]{name};
      return this;
    }

    public Builder amount(int minimumAmount, int withdrawAmount)
    {
      this.minimumAmount = minimumAmount;
      this.amount = withdrawAmount;
      return this;
    }

    public Builder amount(int amount)
    {
      return amount(amount, amount);
    }

    public Builder stackable(boolean stackable)
    {
      this.stackable = stackable;
      return this;
    }

    public Builder noted(boolean noted)
    {
      this.noted = noted;
      return stackable(noted);
    }

    public Builder optional(boolean optional)
    {
      this.optional = optional;
      return this;
    }

    public Builder slot(EquipmentSlot equipmentSlot)
    {
      this.equipmentSlot = equipmentSlot;
      return this;
    }

    public NamedLoadoutItem build()
    {
      if (names == null || names.length == 0)
      {
        throw new LoadoutException("Names not specified for NamedLoadoutItem");
      }

      return new NamedLoadoutItem(identifier, amount, minimumAmount, stackable, noted, optional, equipmentSlot, names);
    }
  }
}
