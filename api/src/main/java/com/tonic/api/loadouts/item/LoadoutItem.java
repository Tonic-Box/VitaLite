package com.tonic.api.loadouts.item;

import com.tonic.data.EquipmentSlot;
import com.tonic.data.ItemEx;

import java.util.List;

public abstract class LoadoutItem {

  private final String identifier;
  private final int minimumAmount;
  private final int amount;
  private final boolean stackable;
  private final boolean noted;
  private final boolean optional;
  private final EquipmentSlot equipmentSlot;

  public LoadoutItem(String identifier, int minimumAmount, int amount, boolean stackable, boolean noted, boolean optional, EquipmentSlot equipmentSlot)
  {
    this.identifier = identifier;
    this.minimumAmount = minimumAmount;
    this.amount = amount;
    this.stackable = stackable;
    this.noted = noted;
    this.optional = optional;
    this.equipmentSlot = equipmentSlot;
  }

  public String getIdentifier()
  {
    return identifier;
  }

  /**
   * @return The amount used in validations
   */
  public int getMinimumAmount()
  {
    return minimumAmount;
  }

  /**
   * @return The amount used in withdrawing
   */
  public int getAmount()
  {
    return amount;
  }

  public boolean isStackable()
  {
    return stackable;
  }

  public boolean isNoted()
  {
    return noted;
  }

  /**
   * @return If the item is optional or not. Items marked as optional will not be passed to the {@link ItemDepletionListener}
   */
  public boolean isOptional()
  {
    return optional;
  }

  public EquipmentSlot getEquipmentSlot()
  {
    return equipmentSlot;
  }

  /**
   * @return A List of items carried in the inventory pertaining to this LoadoutItem
   */
  public abstract List<ItemEx> getCarried();

  /**
   * @return A List of items worn in the equipment pertaining to this LoadoutItem
   */
  public abstract List<ItemEx> getWorn();

  /**
   * @return A List of items in the bank pertaining to this LoadoutItem
   */
  public abstract List<ItemEx> getBanked();

  /**
   * @return {@code true} if the LoadoutItem is carried.
   * Different to getCarried in the sense that it checks for all parameters such as valid quantities
   */
  public boolean isCarried() {
    return isValid(getCarried());
  }

  /**
   * @return {@code true} if the LoadoutItem is worn.
   * Different to getWorn in the sense that it checks for all parameters such as valid quantities
   */
  public boolean isWorn() {
    return isValid(getWorn());
  }

  private boolean isValid(List<ItemEx> present)
  {
    if (present.isEmpty())
    {
      return false;
    }

    int desirable = getMinimumAmount();
    if (desirable == -1)
    {
      desirable = getAmount();
    }

    int count = 0;
    if (!isStackable())
    {
      count = present.size();
    }
    else
    {
      ItemEx item = present.get(0);
      if (item != null)
      {
        count = item.getQuantity();
      }

      if (count >= desirable)
      {
        return true;
      }
    }

    return count >= desirable;
  }
}
