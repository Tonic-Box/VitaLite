package com.tonic.plugins.eqp48.autocombat.utils;

import com.tonic.data.wrappers.TileItemEx;
import com.tonic.plugins.eqp48.autocombat.enums.ValueType;
import net.runelite.api.gameval.ItemID;

public final class LootValues
{
	private LootValues()
	{
	}

	public static long compute(TileItemEx item, ValueType type)
	{
		if (item == null)
		{
			return 0L;
		}

		int qty = Math.max(item.getQuantity(), 1);
		if (type == ValueType.HA)
		{
			return (long) item.getHighAlchValue() * qty;
		}

		long gePrice = item.getGePrice();
		if (item.getId() == ItemID.COINS || item.getId() == ItemID.PLATINUM)
		{
			return gePrice;
		}

		return gePrice * qty;
	}
}
