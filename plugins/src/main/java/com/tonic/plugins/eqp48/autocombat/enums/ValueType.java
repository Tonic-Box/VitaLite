package com.tonic.plugins.eqp48.autocombat.enums;

public enum ValueType
{
	GE("Grand Exchange"),
	HA("High Alchemy");

	private final String displayName;

	ValueType(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
