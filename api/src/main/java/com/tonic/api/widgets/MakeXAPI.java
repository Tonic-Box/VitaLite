package com.tonic.api.widgets;

import com.tonic.api.game.VarAPI;
import com.tonic.data.wrappers.ItemEx;
import net.runelite.api.Item;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * An API designed to interact with the creation/make-X interface
 */
public class MakeXAPI
{

  /**
   * @return true if the creation interface is open
   */
  public static boolean isOpen()
  {
    return WidgetAPI.isVisible(InterfaceID.Skillmulti.UNIVERSE);
  }

  /**
   * @return The current selected amount
   */
  public static int getAmount()
  {
    return VarAPI.getVarcInteger(VarClientID.SKILLMULTI_QUANTITY);
  }

  /**
   * @param desired The amount to set the make X to use
   */
  public static void setAmount(int desired)
  {
    int selected = getAmount();
    if (selected == desired)
    {
      return;
    }

    VarAPI.setVarcInteger(VarClientID.SKILLMULTI_QUANTITY, desired);
  }

  /**
   * Confirms the make X selection by finding an item option with any of the given ids
   * @param itemIds The product item ids to match
   * @return true if any of the item IDs were found and an action was performed
   */
  public static boolean confirm(int... itemIds)
  {
    for (ItemEx item : getResultingItems())
    {
      if (ArrayUtils.contains(itemIds, item.getId()))
      {
        DialogueAPI.resumePause(item.getSlot(), getAmount());
        return true;
      }
    }

    return false;
  }

  /**
   * Confirms the make X selection by finding an item option with any of the given names
   * @param itemNames The product item names to match
   * @return true if any of the item names were found and an action was performed
   */
  public static boolean confirm(String... itemNames)
  {
    for (ItemEx item : getResultingItems())
    {
      for (String itemName : itemNames)
      {
        if (itemName.equalsIgnoreCase(item.getName()))
        {
          DialogueAPI.resumePause(item.getSlot(), getAmount());
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Confirms the make X selection using the given index
   * @param index The index to select
   */
  public static void confirmIndex(int index)
  {
    DialogueAPI.resumePause(InterfaceID.Skillmulti.A + index, getAmount());
  }

  private static List<ItemEx> getResultingItems()
  {
    List<ItemEx> items = new ArrayList<>();
    for (int i = InterfaceID.Skillmulti.A; i < InterfaceID.Skillmulti.R; i++) //ok jagex...
    {
      Widget button = WidgetAPI.get(i);
      if (button == null || button.isSelfHidden())
      {
        continue;
      }

      Widget[] parts = button.getChildren();
      if (parts == null)
      {
        continue;
      }

      for (Widget part : parts)
      {
        int itemId = part.getItemId();
        if (itemId == -1 || itemId == 6512)
        {
          continue;
        }

        Item item = new Item(part.getItemId(), part.getItemQuantity());
        //We store the interface ID as the slot, since that's what contains the action and we need it in confirm
        items.add(new ItemEx(item, i));
      }
    }

    return items;
  }
}
