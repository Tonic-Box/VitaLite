package com.tonic.api.loadouts;

import com.tonic.data.EquipmentSlot;

public interface LoadoutItem {

  String getIdentifier();

  int getAmount();

  int getMinimumAmount();

  boolean isStackable();

  boolean isNoted();

  default boolean isOptional()
  {
    return false;
  }

  default EquipmentSlot getSlot()
  {
    return null;
  }
}
