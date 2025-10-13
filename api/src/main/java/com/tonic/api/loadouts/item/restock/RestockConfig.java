package com.tonic.api.loadouts.item.restock;

public class RestockConfig
{

  private final int itemId;
  private final int itemAmount;
  private final PriceSetupType priceSetupType;
  private final int price;

  /**
   * @param itemId The ID of the item to restock. See {@link net.runelite.api.gameval.ItemID}
   * @param itemAmount The amount of the item to restock.
   * @param priceSetupType The price type, this can be a static price or how many times to use +5%. See {@link PriceSetupType}
   * @param price The price to pay per item as per the defined {@link PriceSetupType} parameter.
   */
  public RestockConfig(int itemId, int itemAmount, PriceSetupType priceSetupType, int price)
  {
    this.itemId = itemId;
    this.itemAmount = itemAmount;
    this.priceSetupType = priceSetupType;
    this.price = price;
  }

  public int getItemId()
  {
    return itemId;
  }

  public int getItemAmount()
  {
    return itemAmount;
  }

  public PriceSetupType getPriceSetupType()
  {
    return priceSetupType;
  }

  public int getPrice()
  {
    return price;
  }
}
