package com.tonic.plugins.eqp48.autocombat.enums;

public enum LootMode
{
	BY_NAME("By name"),
	BY_VALUE("By value");

	private final String displayName;

	LootMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
