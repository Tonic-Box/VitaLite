package com.tonic.services.pathfinder.teleports;


import com.tonic.data.magic.Spell;
import com.tonic.data.magic.spellbooks.Standard;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public enum TeleportSpell
{
	VARROCK_TELEPORT(
		Standard.VARROCK_TELEPORT,
		new WorldPoint(3212, 3424, 0),
		false),
	FALADOR_TELEPORT(
		Standard.FALADOR_TELEPORT,
		new WorldPoint(2966, 3379, 0),
		false),

	;

	private final Spell spell;
	private final WorldPoint point;
	private final boolean members;
	TeleportSpell(Spell spell, WorldPoint point, boolean members)
	{
		this.spell = spell;
		this.point = point;
		this.members = members;
	}

	public Spell getSpell()
	{
		return spell;
	}

	public WorldPoint getPoint()
	{
		return point;
	}

	public boolean isMembers()
	{
		return members;
	}

}
