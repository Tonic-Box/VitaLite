package com.tonic.plugins.eqp48.autocombat;

import com.tonic.plugins.eqp48.autocombat.enums.LootMode;
import com.tonic.plugins.eqp48.autocombat.enums.ValueType;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("eqp48autocombat")
public interface AutoCombatConfig extends Config
{
	@ConfigSection(
		name = "General",
		description = "",
		position = 0
	)
	String GENERAL = "general";

	@ConfigItem(
		position = 0,
		keyName = "generalInfo",
		name = "",
		description = "Right-click NPCs and select \"Mark target\" to flag them as eligible targets.",
		section = GENERAL
	)
	default String generalInfo()
	{
		return "Right-click NPCs and select \"Mark target\" to flag them as eligible targets.";
	}

	@ConfigSection(
		name = "Attacking",
		description = "",
		position = 1
	)
	String FIGHTING = "attacking";

	@ConfigSection(
		name = "Looting",
		description = "",
		position = 2
	)
	String LOOTING = "looting";

	@ConfigSection(
		name = "Loot mode (By name)",
		description = "",
		position = 3,
		closedByDefault = true
	)
	String LOOTING_BY_NAME = "lootingByName";

	@ConfigSection(
		name = "Loot mode (By value)",
		description = "",
		position = 4,
		closedByDefault = true
	)
	String LOOTING_BY_VALUE = "lootingByValue";

	@ConfigSection(
		name = "Hitpoints",
		description = "",
		position = 5
	)
	String HITPOINTS = "hitpoints";

	@ConfigSection(
		name = "Prayer",
		description = "",
		position = 6
	)
	String PRAYER = "prayer";

	@ConfigSection(
		name = "Miscellaneous",
		description = "",
		position = 7
	)
	String MISC = "misc";

	@ConfigItem(
		position = 0,
		keyName = "npcNames",
		name = "NPC names/IDs",
		description = "CSV list of NPC names or IDs to attack.",
		section = FIGHTING
	)
	default String npcNames()
	{
		return "";
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		position = 1,
		keyName = "specThreshold",
		name = "Spec at %",
		description = "Enable special attack at or above this energy (0 = off).",
		section = FIGHTING
	)
	default int specThreshold()
	{
		return 0;
	}

	@ConfigItem(
		position = 2,
		keyName = "avoidTargetsInCombat",
		name = "Avoid engaged targets",
		description = "Skip NPCs that are already in combat.",
		section = FIGHTING
	)
	default boolean avoidTargetsInCombat()
	{
		return true;
	}

	@ConfigItem(
		position = 2,
		keyName = "prioritizeLooting",
		name = "Prioritize looting",
		description = "Put looting actions above fighting.",
		section = LOOTING
	)
	default boolean prioritizeLooting()
	{
		return false;
	}

	@Range(
		min = 1,
		max = 30
	)
	@ConfigItem(
		position = 1,
		keyName = "lootDistance",
		name = "Max loot distance",
		description = "Maximum tiles away to consider loot.",
		section = LOOTING
	)
	default int lootDistance()
	{
		return 12;
	}

	@ConfigItem(
		position = 0,
		keyName = "lootMode",
		name = "Loot mode",
		description = "Choose whether to loot by name list or by value.",
		section = LOOTING
	)
	default LootMode lootMode()
	{
		return LootMode.BY_NAME;
	}

	@ConfigItem(
		position = 0,
		keyName = "lootNames",
		name = "Loot names/IDs",
		description = "CSV list of item names or IDs to loot when mode is By name.",
		section = LOOTING_BY_NAME
	)
	default String lootNames()
	{
		return "";
	}

	@ConfigItem(
		position = 0,
		keyName = "valueType",
		name = "Value type",
		description = "Choose which price to use when looting by value.",
		section = LOOTING_BY_VALUE
	)
	default ValueType valueType()
	{
		return ValueType.GE;
	}

	@ConfigItem(
		position = 1,
		keyName = "minValue",
		name = "Min value",
		description = "Pick up items at or above this value when looting by value.",
		section = LOOTING_BY_VALUE
	)
	default int minValue()
	{
		return 1000;
	}

	@ConfigItem(
		position = 3,
		keyName = "lootOnlyOwned",
		name = "Loot only our items",
		description = "Only loot ground items with self ownership.",
		section = LOOTING
	)
	default boolean lootOnlyOwned()
	{
		return false;
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		position = 2,
		keyName = "eatAtHpPercent",
		name = "Restore HP at %",
		description = "Eat food at or below this HP% (0 disables eating).",
		section = HITPOINTS
	)
	default int eatAtHpPercent()
	{
		return 50;
	}

	@ConfigItem(
		position = 1,
		keyName = "foodNames",
		name = "Food names/IDs",
		description = "CSV list of food names or IDs used for eating.",
		section = HITPOINTS
	)
	default String foodNames()
	{
		return "Shark";
	}

	@ConfigItem(
		position = 2,
		keyName = "restorePrayer",
		name = "Restore prayer",
		description = "Drink a prayer potion when below the set threshold.",
		section = PRAYER
	)
	default boolean restorePrayer()
	{
		return false;
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		position = 3,
		keyName = "prayerAtPercent",
		name = "Restore prayer at %",
		description = "Drink at or below this % (0 disables).",
		section = PRAYER
	)
	default int prayerAtPercent()
	{
		return 25;
	}

	@ConfigItem(
		position = 0,
		keyName = "buryBones",
		name = "Bury bones",
		description = "Bury bones in inventory.",
		section = MISC
	)
	default boolean buryBones()
	{
		return false;
	}

	@ConfigItem(
		position = 1,
		keyName = "scatterAshes",
		name = "Scatter ashes",
		description = "Scatter ashes in inventory.",
		section = MISC
	)
	default boolean scatterAshes()
	{
		return false;
	}
}
