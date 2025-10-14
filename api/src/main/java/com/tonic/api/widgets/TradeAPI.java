package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.data.ItemEx;
import com.tonic.queries.InventoryQuery;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

import java.util.List;

public class TradeAPI
{

  private static final int TRADE_OTHER_INV_ID = 32858;

  /**
   * @return true if the trade window is open
   */
  public static boolean isOpen()
  {
    return isOnMainScreen() || isOnConfirmationScreen();
  }

  /**
   * @return true if the first screen of the trade window is open
   */
  public static boolean isOnMainScreen()
  {
    return WidgetAPI.isVisible(InterfaceID.Trademain.UNIVERSE);
  }

  /**
   * @return true is the second screen of the trade window is open
   */
  public static boolean isOnConfirmationScreen()
  {
    return WidgetAPI.isVisible(InterfaceID.Tradeconfirm.UNIVERSE);
  }

  /**
   * @return Our items in trade
   */
  public static List<ItemEx> getOfferingItems()
  {
    return Static.invoke(() -> InventoryQuery.fromInventoryId(InventoryID.TRADEOFFER).collect());
  }

  /**
   * @return Other players items in trade
   */
  public static List<ItemEx> getReceivingItems()
  {
    return Static.invoke(() -> InventoryQuery.fromInventoryId(TRADE_OTHER_INV_ID).collect());
  }

  /**
   * Offers an item with the given id and amount
   * @param id The item id
   * @param amount The item amount
   */
  public static void offer(int id, int amount)
  {
    ItemEx item = InventoryAPI.getItem(id);
    if (item == null)
    {
      return;
    }

    offer(item, amount);
  }

  /**
   * Offers an item with the given name and amount
   * @param name The name to offer
   * @param amount The amount to offer
   */
  public static void offer(String name, int amount)
  {
    ItemEx item = InventoryAPI.getItem(name);
    if (item == null)
    {
      return;
    }

    offer(item, amount);
  }

  /**
   * Removes an item from the trade with the given id and amount
   * @param id The id to remove
   * @param amount The amount to remove
   */
  public static void remove(int id, int amount)
  {
    ItemEx item = Static.invoke(() -> InventoryQuery.fromInventoryId(InventoryID.TRADEOFFER).withId(id).first());
    if (item == null)
    {
      return;
    }

    remove(item, amount);
  }

  /**
   * Removes an item from the trade with the given name and amount
   * @param name The name to remove
   * @param amount The amount to remove
   */
  public static void remove(String name, int amount)
  {
    ItemEx item = Static.invoke(() -> InventoryQuery.fromInventoryId(InventoryID.TRADEOFFER).withName(name).first());
    if (item == null)
    {
      return;
    }

    remove(item, amount);
  }

  /**
   * Offers an item from the inventory
   * @param item The item to offer
   * @param amount The amount to offer
   */
  public static void offer(ItemEx item, int amount)
  {
    interact(InterfaceID.Tradeside.SIDE_LAYER, item, amount);
  }

  /**
   * Removes an offered item
   * @param item The item to remove
   * @param amount The amount to remove
   */
  public static void remove(ItemEx item, int amount)
  {
    interact(InterfaceID.Trademain.YOUR_OFFER, item, amount);
  }

  private static void interact(int widget, ItemEx item, int amount)
  {
    if (amount <= 0)
    {
      return;
    }

    if (amount == 1)
    {
      WidgetAPI.interact(1, widget, item.getSlot(), item.getId());
    }
    else if (amount == 5)
    {
      WidgetAPI.interact(2, widget, item.getSlot(), item.getId());
    }
    else if (amount == 10)
    {
      WidgetAPI.interact(3, widget, item.getSlot(), item.getId());
    }
    else if (amount >= item.getQuantity())
    {
      WidgetAPI.interact(4, widget, item.getSlot(), item.getId());
    }
    else
    {
      WidgetAPI.interact(5, widget, item.getSlot(), item.getId());
      DialogueAPI.resumeNumericDialogue(amount);
    }
  }

  /**
   * @return true if we have accepted the trade
   */
  public static boolean isAcceptedByPlayer()
  {
    Widget label = getStatusLabel();
    return label != null && label.getText().contains("Waiting for other player");
  }

  /**
   * @return true if the other player has accepted the trade
   */
  public static boolean isAcceptedByOther()
  {
    Widget label = getStatusLabel();
    return label != null && label.getText().contains("Other player has accepted");
  }

  private static Widget getStatusLabel()
  {
    return getWidget(InterfaceID.Trademain.STATUS, InterfaceID.Tradeconfirm.TITLE);
  }

  /**
   * Accepts the trade
   */
  public static void accept() {
    Widget widget = getAcceptWidget();
    if (widget == null)
    {
      return;
    }

    WidgetAPI.interact(widget, 1);
  }

  /**
   * Declines the trade
   */
  public static void decline() {
    Widget widget = getDeclineWidget();
    if (widget == null)
    {
      return;
    }

    WidgetAPI.interact(widget, 1);
  }

  private static Widget getAcceptWidget()
  {
    return getWidget(InterfaceID.Trademain.ACCEPT, InterfaceID.Tradeconfirm.TRADE2ACCEPT);
  }

  private static Widget getDeclineWidget()
  {
    return getWidget(InterfaceID.Trademain.DECLINE, InterfaceID.Tradeconfirm.TRADE2DECLINE);
  }

  private static Widget getWidget(int main, int confirm)
  {
    if (isOnMainScreen())
    {
      return WidgetAPI.get(main);
    }

    if (isOnConfirmationScreen())
    {
      return WidgetAPI.get(confirm);
    }

    return null;
  }
}
