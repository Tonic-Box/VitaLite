package com.tonic.data.smithing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.tonic.api.widgets.InventoryAPI;
import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Requirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.SkillRequirement;

import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Represents all types of metal bars that can be smelted in the game.
 */
public enum BarType {
    BRONZE("Bronze bar", ItemID.BRONZE_BAR, 6.2,
            new OreRequirement[] {new OreRequirement(ItemID.COPPER_ORE, 1), new OreRequirement(ItemID.TIN_ORE, 1)},
            new SkillRequirement(Skill.SMITHING, 1)),
    LUNAR("Lunar bar", ItemID.QUEST_LUNAR_MAGIC_BAR, 6.2,
            new OreRequirement[] {new OreRequirement(ItemID.QUEST_LUNAR_MAGIC_ORE, 1)},
            new SkillRequirement(Skill.SMITHING, 1)),
    BLURITE("Blurite bar", ItemID.BLURITE_BAR, 8.0,
            new OreRequirement[] {new OreRequirement(ItemID.BLURITE_ORE, 1)},
            new SkillRequirement(Skill.SMITHING, 8),
            new QuestRequirement(Quest.THE_KNIGHTS_SWORD)),
    IRON("Iron bar", ItemID.IRON_BAR, 12.5,
            new OreRequirement[] {new OreRequirement(ItemID.IRON_ORE, 1)},
            new SkillRequirement(Skill.SMITHING, 15),
            new SuccessRateModifier(0.5)), // 50% base success rate
    ELEMENTAL("Elemental metal", ItemID.ELEMENTAL_WORKSHOP_BAR, 7.5,
            new OreRequirement[] {new OreRequirement(ItemID.ELEMENTAL_WORKSHOP_ORE, 1), new OreRequirement(ItemID.COAL, 4)},
            new SkillRequirement(Skill.SMITHING, 20),
            new QuestRequirement(Quest.ELEMENTAL_WORKSHOP_I)),
    SILVER("Silver bar", ItemID.SILVER_BAR, 13.7,
            new OreRequirement[] {new OreRequirement(ItemID.SILVER_ORE, 1)},
            new SkillRequirement(Skill.SMITHING, 20)),
    LEAD("Lead bar", ItemID.LEAD_BAR, 15.5,
            new OreRequirement[] {new OreRequirement(ItemID.LEAD_ORE, 2)},
            new SkillRequirement(Skill.SMITHING, 25)),
    STEEL("Steel bar", ItemID.STEEL_BAR, 17.5,
            new OreRequirement[] {new OreRequirement(ItemID.IRON_ORE, 1), new OreRequirement(ItemID.COAL, 2)},
            new SkillRequirement(Skill.SMITHING, 30)),
    GOLD("Gold bar", ItemID.GOLD_BAR, 22.5,
            new OreRequirement[] {new OreRequirement(ItemID.GOLD_ORE, 1)},
            new SkillRequirement(Skill.SMITHING, 40),
            new GoldsmithGauntletsModifier(56.2)),
    LOVAKITE("Lovakite bar", ItemID.LOVAKITE_BAR, 20.0,
            new OreRequirement[] {new OreRequirement(ItemID.LOVAKITE_ORE, 1), new OreRequirement(ItemID.COAL, 2)},
            new SkillRequirement(Skill.SMITHING, 45)),
    MITHRIL("Mithril bar", ItemID.MITHRIL_BAR, 30.0,
            new OreRequirement[] {new OreRequirement(ItemID.MITHRIL_ORE, 1), new OreRequirement(ItemID.COAL, 4)},
            new SkillRequirement(Skill.SMITHING, 50)),
    ADAMANT("Adamant bar", ItemID.ADAMANTITE_BAR, 37.5,
            new OreRequirement[] {new OreRequirement(ItemID.ADAMANTITE_ORE, 1), new OreRequirement(ItemID.COAL, 6)},
            new SkillRequirement(Skill.SMITHING, 70)),
    CUPRONICKEL("Cupronickel bar", ItemID.CUPRONICKEL_BAR, 42.0,
            new OreRequirement[] {new OreRequirement(ItemID.COPPER_ORE, 2), new OreRequirement(ItemID.NICKEL_ORE, 1)},
            new SkillRequirement(Skill.SMITHING, 74)),
    RUNE("Rune bar", ItemID.RUNITE_BAR, 50.0,
            new OreRequirement[] {new OreRequirement(ItemID.RUNITE_ORE, 1), new OreRequirement(ItemID.COAL, 8)},
            new SkillRequirement(Skill.SMITHING, 85));

    private final String displayName;
    private final int barId;
    private final double baseSmithingExp;
    private final Requirements requirements;
    private final SmeltingModifier[] modifiers;

    /**
     * Constructs a BarType with skill requirement only.
     */
    BarType(String displayName, int barId, double baseSmithingExp,
            OreRequirement[] oreRequirements, SkillRequirement skillRequirement) {
        this(displayName, barId, baseSmithingExp, oreRequirements, skillRequirement, (Requirement) null);
    }

    /**
     * Constructs a BarType with skill requirement and one additional requirement.
     */
    BarType(String displayName, int barId, double baseSmithingExp,
            OreRequirement[] oreRequirements, SkillRequirement skillRequirement,
            Requirement additionalReq) {
        this(displayName, barId, baseSmithingExp, oreRequirements, skillRequirement, additionalReq,
                new SmeltingModifier[0]);
    }

    /**
     * Constructs a BarType with skill requirement and modifiers.
     */
    BarType(String displayName, int barId, double baseSmithingExp,
            OreRequirement[] oreRequirements, SkillRequirement skillRequirement,
            SmeltingModifier... modifiers) {
        this(displayName, barId, baseSmithingExp, oreRequirements, skillRequirement, null, modifiers);
    }

    /**
     * Main constructor for BarType with consolidated requirements.
     */
    BarType(String displayName, int barId, double baseSmithingExp,
            OreRequirement[] oreRequirements, SkillRequirement skillRequirement,
            Requirement additionalReq, SmeltingModifier... modifiers) {

        this.displayName = displayName;
        this.barId = barId;
        this.baseSmithingExp = baseSmithingExp;

        Requirements reqs = new Requirements();
        reqs.addRequirement(skillRequirement);
        if (additionalReq != null) {
            reqs.addRequirement(additionalReq);
        }
        for (OreRequirement oreReq : oreRequirements) {
            reqs.addRequirement(oreReq);
        }

        this.requirements = reqs;
        this.modifiers = modifiers != null ? modifiers.clone() : new SmeltingModifier[0];
    }

    /**
     * Returns the display name of the bar.
     *
     * @return the display name (e.g., "Bronze bar")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the item ID of the bar.
     *
     * @return the bar's item ID
     */
    public int getBarId() {
        return barId;
    }

    /**
     * Returns the base smithing experience for this bar.
     * Use {@link #getSmithingExp(boolean)} for context-aware experience calculation.
     *
     * @return the base smithing experience
     */
    public double getBaseSmithingExp() {
        return baseSmithingExp;
    }

    /**
     * Returns the actual smithing experience based on current conditions.
     *
     * @param hasGoldsmithGauntlets whether the player is wearing goldsmith gauntlets or smithing cape
     * @return the modified smithing experience
     */
    public double getSmithingExp(boolean hasGoldsmithGauntlets) {
        if (hasGoldsmithGauntlets) {
            for (SmeltingModifier modifier : modifiers) {
                if (modifier instanceof GoldsmithGauntletsModifier) {
                    return ((GoldsmithGauntletsModifier) modifier).getModifiedExperience();
                }
            }
        }
        return baseSmithingExp;
    }

    /**
     * Returns the list of ore requirements for smelting this bar.
     * Extracted from the consolidated requirements.
     */
    public List<OreRequirement> getOreRequirements() {
        return requirements.getAll().stream()
                .filter(req -> req instanceof OreRequirement)
                .map(req -> (OreRequirement) req)
                .collect(Collectors.toList());
    }

    /**
     * Returns the ore requirements adjusted for Blast Furnace (halves coal requirement).
     */
    public List<OreRequirement> getOreRequirements(boolean isBlastFurnace) {
        if (!isBlastFurnace) {
            return getOreRequirements();
        }

        return getOreRequirements().stream()
                .map(oreReq -> {
                    if (oreReq.getOreId() == ItemID.COAL) {
                        int halvedCount = Math.max(1, oreReq.getCount() / 2);
                        return new OreRequirement(ItemID.COAL, halvedCount);
                    }
                    return oreReq;
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns the quantity of a specific ore required.
     *
     * @param oreId the ore item ID to check
     * @return the quantity required, or 0 if not required
     */
    public int getOreCount(int oreId) {
        for (OreRequirement req : getOreRequirements()) {
            if (req.getOreId() == oreId) {
                return req.getCount();
            }
        }
        return 0;
    }

    /**
     * Returns the quantity of coal required.
     *
     * @return the coal quantity, or 0 if coal is not required
     */
    public int getCoalCount() {
        return getOreCount(ItemID.COAL);
    }

    /**
     * Returns the quantity of coal required at Blast Furnace (halved).
     *
     * @return the halved coal quantity, or 0 if coal is not required
     */
    public int getCoalCountBlastFurnace() {
        int count = getCoalCount();
        return count > 0 ? Math.max(1, count / 2) : 0;
    }

    /**
     * Returns the success rate for smelting this bar.
     *
     * @param hasRingOfForging whether the player has a Ring of Forging equipped
     * @param isBlastFurnace whether smelting at the Blast Furnace
     * @return the success rate (0.0 to 1.0)
     */
    public double getSuccessRate(boolean hasRingOfForging, boolean isBlastFurnace) {
        // Ring of Forging or Blast Furnace guarantees 100% success for iron
        if (hasRingOfForging || isBlastFurnace) {
            return 1.0;
        }

        for (SmeltingModifier modifier : modifiers) {
            if (modifier instanceof SuccessRateModifier) {
                return ((SuccessRateModifier) modifier).getSuccessRate();
            }
        }

        return 1.0;
    }

    /**
     * Checks if all requirements are met to smelt this bar.
     * This includes skills, quests, AND having the required ores in inventory.
     *
     * @return true if all requirements are fulfilled
     */
    public boolean canSmelt() {
        return requirements.fulfilled();
    }

    /**
     * Checks if player has the required ores for smelting.
     * Separate from canSmelt() to allow for special cases like Blast Furnace.
     *
     * @param isBlastFurnace whether smelting at the Blast Furnace
     * @return true if player has all required ores
     */
    public boolean hasOres(boolean isBlastFurnace) {
        for (OreRequirement ore : getOreRequirements(isBlastFurnace)) {
            if (InventoryAPI.getCount(ore.getOreId()) < ore.getCount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the requirements for smelting this bar.
     *
     * @return the requirements (never null)
     */
    public Requirements getRequirements() {
        return requirements;
    }

    /**
     * Returns all smelting modifiers for this bar.
     * The returned list is unmodifiable.
     *
     * @return unmodifiable list of modifiers
     */
    public List<SmeltingModifier> getModifiers() {
        return Collections.unmodifiableList(Arrays.asList(modifiers));
    }

    /**
     * Calculates the total number of ore items needed to smelt one bar.
     *
     * @param isBlastFurnace whether smelting at the Blast Furnace
     * @return the total ore count
     */
    public int getTotalOreCount(boolean isBlastFurnace) {
        int total = 0;
        for (OreRequirement req : getOreRequirements(isBlastFurnace)) {
            total += req.getCount();
        }
        return total;
    }

    /**
     * Checks if this bar requires coal.
     *
     * @return true if coal is required
     */
    public boolean requiresCoal() {
        return getOreCount(ItemID.COAL) > 0;
    }

    /**
     * Finds a BarType by its bar item ID.
     *
     * @param barId the bar's item ID
     * @return the matching BarType, or null if not found
     */
    public static BarType fromBarId(int barId) {
        for (BarType type : values()) {
            if (type.getBarId() == barId) {
                return type;
            }
        }
        return null;
    }

    /**
     * Finds a BarType by its display name (case-insensitive).
     *
     * @param name the display name to search for
     * @return the matching BarType, or null if not found
     */
    public static BarType fromName(String name) {
        if (name == null) {
            return null;
        }
        for (BarType type : values()) {
            if (type.getDisplayName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
