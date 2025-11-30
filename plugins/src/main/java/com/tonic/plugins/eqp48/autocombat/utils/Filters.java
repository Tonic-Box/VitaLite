package com.tonic.plugins.eqp48.autocombat.utils;

import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileItemEx;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Filters
{
	private final Set<Integer> ids = new HashSet<>();
	private final Set<String> names = new HashSet<>();

	public static Filters fromCsv(String raw)
	{
		Filters filters = new Filters();
		if (raw == null || raw.isEmpty())
		{
			return filters;
		}

		for (String part : raw.split(","))
		{
			String trimmed = part.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}

			try
			{
				filters.ids.add(Integer.parseInt(trimmed));
			}
			catch (NumberFormatException ex)
			{
				filters.names.add(trimmed.toLowerCase(Locale.ENGLISH));
			}
		}

		return filters;
	}

	public boolean isEmpty()
	{
		return ids.isEmpty() && names.isEmpty();
	}

	public boolean matches(NpcEx npc)
	{
		if (npc == null)
		{
			return false;
		}

		if (ids.contains(npc.getId()))
		{
			return true;
		}

		String name = npc.getName();
		return name != null && names.contains(name.toLowerCase(Locale.ENGLISH));
	}

	public boolean matches(ItemEx item)
	{
		if (item == null)
		{
			return false;
		}

		int id = item.getId();
		int canonical = item.getCanonicalId();
		if (ids.contains(id) || ids.contains(canonical))
		{
			return true;
		}

		String name = item.getName();
		return name != null && names.contains(name.toLowerCase(Locale.ENGLISH));
	}

	public boolean matches(TileItemEx item)
	{
		if (item == null)
		{
			return false;
		}

		if (ids.contains(item.getId()) || ids.contains(item.getCanonicalId()))
		{
			return true;
		}

		String name = item.getName();
		return name != null && names.contains(name.toLowerCase(Locale.ENGLISH));
	}
}
