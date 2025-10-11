package com.tonic.api.loadouts.item;

import com.tonic.api.loadouts.LoadoutException;
import com.tonic.data.EquipmentSlot;
import com.tonic.data.ItemEx;
import com.tonic.queries.InventoryQuery;
import net.runelite.api.gameval.InventoryID;

import java.util.List;

public class IdLoadoutItem extends LoadoutItem {

  private final int[] ids;

  private IdLoadoutItem(String identifier, int minimumAmount, int amount, boolean stackable, boolean noted, boolean optional, EquipmentSlot equipmentSlot, int[] ids)
  {
    super(identifier, minimumAmount, amount, stackable, noted, optional, equipmentSlot);
    this.ids = ids;
  }

  public int[] getIds()
  {
    return ids;
  }

  public static Builder builder()
  {
    return new Builder();
  }

  @Override
  public List<ItemEx> getCarried()
  {
    return InventoryQuery.fromInventoryId(InventoryID.INV)
        .withId(ids)
        .removeIf(item -> item.isNoted() != isNoted())
        .collect();
  }

  @Override
  public List<ItemEx> getWorn()
  {
    return InventoryQuery.fromInventoryId(InventoryID.WORN)
        .withId(ids)
        .collect();
  }

  @Override
  public List<ItemEx> getBanked()
  {
    return InventoryQuery.fromInventoryId(InventoryID.BANK)
        .withId(ids)
        .removeIf(ItemEx::isPlaceholder)
        .collect();
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
    private int[] ids;

    public Builder identifier(String identifier)
    {
      this.identifier = identifier;
      return this;
    }

    public Builder ids(int... ids)
    {
      this.ids = ids;
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

    public IdLoadoutItem build()
    {
      if (ids == null || ids.length == 0)
      {
        throw new LoadoutException("IDs not specified for IdLoadoutItem");
      }

      return new IdLoadoutItem(identifier, amount, minimumAmount, stackable, noted, optional, equipmentSlot, ids);
    }
  }
}
