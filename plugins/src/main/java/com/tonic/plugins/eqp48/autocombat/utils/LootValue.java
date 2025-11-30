package com.tonic.plugins.eqp48.autocombat.utils;

import com.tonic.data.wrappers.TileItemEx;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LootValue
{
	private final TileItemEx item;
	private final long value;
}
