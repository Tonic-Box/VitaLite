package com.tonic.api.game;

import com.google.common.collect.ImmutableMap;
import com.tonic.Static;
import com.tonic.api.widgets.WidgetAPI;
import java.util.Map;
import java.util.Set;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Skill API
 */
public class SkillAPI {
    public final static Set<Skill> MEMBER_SKILLS = Set.of(
            Skill.AGILITY,
            Skill.HERBLORE,
            Skill.THIEVING,
            Skill.FLETCHING,
            Skill.SLAYER,
            Skill.FARMING,
            Skill.CONSTRUCTION,
            Skill.HUNTER,
            Skill.SAILING
    );
    private static final int MAX_SKILL_LEVEL = 99;
    private static final int[] XP_TABLE;

    /**
     * Mapping of skills to their reward widget packed IDs.
     */
    private static final Map<Skill, Integer> skillRewardMap;

    static {
        XP_TABLE = new int[127];
        XP_TABLE[0] = 0;

        for (int level = 1; level < XP_TABLE.length; level++) {
            double delta = 0;
            for (int i = 1; i < level; i++) {
                delta += Math.floor(i + 300 * Math.pow(2, i / 7.0));
            }

            XP_TABLE[level] = (int) Math.floor(delta / 4);
        }

        skillRewardMap = ImmutableMap.<Skill, Integer>builder()
                .put(Skill.ATTACK, InterfaceID.Xpreward.ATTACK)
                .put(Skill.STRENGTH, InterfaceID.Xpreward.STRENGTH)
                .put(Skill.RANGED, InterfaceID.Xpreward.RANGED)
                .put(Skill.MAGIC, InterfaceID.Xpreward.MAGIC)
                .put(Skill.DEFENCE, InterfaceID.Xpreward.DEFENCE)
                .put(Skill.HITPOINTS, InterfaceID.Xpreward.HITPOINTS)
                .put(Skill.PRAYER, InterfaceID.Xpreward.PRAYER)
                .put(Skill.AGILITY, InterfaceID.Xpreward.AGILITY)
                .put(Skill.HERBLORE, InterfaceID.Xpreward.HERBLORE)
                .put(Skill.THIEVING, InterfaceID.Xpreward.THIEVING)
                .put(Skill.CRAFTING, InterfaceID.Xpreward.CRAFTING)
                .put(Skill.RUNECRAFT, InterfaceID.Xpreward.RUNECRAFT)
                .put(Skill.SLAYER, InterfaceID.Xpreward.SLAYER)
                .put(Skill.FARMING, InterfaceID.Xpreward.FARMING)
                .put(Skill.MINING, InterfaceID.Xpreward.MINING)
                .put(Skill.SMITHING, InterfaceID.Xpreward.SMITHING)
                .put(Skill.FISHING, InterfaceID.Xpreward.FISHING)
                .put(Skill.COOKING, InterfaceID.Xpreward.COOKING)
                .put(Skill.FIREMAKING, InterfaceID.Xpreward.FIREMAKING)
                .put(Skill.WOODCUTTING, InterfaceID.Xpreward.WOODCUTTING)
                .put(Skill.FLETCHING, InterfaceID.Xpreward.FLETCHING)
                .put(Skill.CONSTRUCTION, InterfaceID.Xpreward.CONSTRUCTION)
                .put(Skill.HUNTER, InterfaceID.Xpreward.HUNTER)
                .build();
    }

    /**
     * Gets the minimum experience required to reach a specific base level.
     *
     * @param level The skill level to check (0 to XP_TABLE.length - 1).
     * @return The total experience at that level, or 0 if the level is out of the calculated bounds.
     */
    public static int getExperienceAt(int level) {
        if (level < 0 || level >= XP_TABLE.length) {
            return 0;
        }

        return XP_TABLE[level];
    }

    /**
     * Gets the base skill level corresponding to a total experience value.
     * The returned level will not exceed {@value #MAX_SKILL_LEVEL}.
     *
     * @param experience The total experience earned in a skill.
     * @return The calculated level based on experience, or -1 if no level is found (should only happen for experience < 0).
     */
    public static int getLevelAt(int experience) {
        for (int i = XP_TABLE.length - 1; i > 0; i--) {
            if (i <= MAX_SKILL_LEVEL) {
                int experienceAtLevel = XP_TABLE[i];
                if (experience >= experienceAtLevel) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Calculates the experience needed to reach the next base level for a given skill.
     *
     * @param skill The skill to check.
     * @return The experience required for the next level, or 0 if the skill is already at {@value #MAX_SKILL_LEVEL}.
     */
    public static int getExperienceToNextLevel(Skill skill) {
        int nextLevel = getLevel(skill) + 1;
        if (nextLevel > MAX_SKILL_LEVEL) {
            return 0;
        }

        return getExperienceAt(nextLevel) - getExperience(skill);
    }

    /**
     * Gets the current base level of a skill, without boosts.
     *
     * @param skill The skill to check.
     * @return The skill's base level.
     */
    public static int getLevel(Skill skill) {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getRealSkillLevel(skill));
    }

    /**
     * Gets the total level across all skills (base levels, not boosted).
     *
     * @return the sum of all skill levels.
     */
    public static int getTotalLevel() {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            int totalLevel = 0;
            for (Skill skill : Skill.values()) {
                totalLevel += client.getRealSkillLevel(skill);
            }
            return totalLevel;
        });
    }

    /**
     * Gets the current boosted level of a skill (current level, including temporary boosts).
     *
     * @param skill The skill to check.
     * @return The skill's boosted level.
     */
    public static int getBoostedLevel(Skill skill) {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getBoostedSkillLevel(skill));
    }

    /**
     * Gets the total boosted level across all skills.
     *
     * @return the sum of all boosted skill levels.
     */
    public static int getBoostedTotalLevel() {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            int totalLevel = 0;
            for (Skill skill : Skill.values()) {
                totalLevel += client.getBoostedSkillLevel(skill);
            }
            return totalLevel;
        });
    }

    /**
     * Gets the total experience earned in a specific skill.
     *
     * @param skill The skill to check.
     * @return The skill's current total experience.
     */
    public static int getExperience(Skill skill) {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getSkillExperience(skill));
    }

    /**
     * Gets the total experience across all skills.
     *
     * @return the sum of all skill experience.
     */
    public static int getTotalExperience() {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            int totalExperience = 0;
            for (Skill skill : Skill.values()) {
                totalExperience += client.getSkillExperience(skill);
            }
            return totalExperience;
        });
    }

    /**
     * Gets the reward widget in the experience rewards widget (lamp, book, ...) for a given skill.
     *
     * @param skill the preferred skill
     * @return the reward widget corresponding the requested skill
     */
    public static Widget getRewardWidget(Skill skill) {
        return WidgetAPI.get(skillRewardMap.get(skill));
    }

    /**
     * Check if the skill's reward widget can be selected. This can be false if, for example, the skill is not yet
     * unlocked (like herblore).
     *
     * @param skill the skill to check
     * @return true if the skill's reward widget can be selected, false otherwise
     */
    public static boolean canSelectReward(Skill skill) {
        Widget rewardWidget = getRewardWidget(skill);

        return rewardWidget != null
                && rewardWidget.getChild(9) != null
                && rewardWidget.getChild(9).getOpacity() != 150;
    }
}