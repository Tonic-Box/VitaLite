package com.tonic.api.game;

import com.google.common.collect.ImmutableMap;
import com.tonic.Static;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.queries.WidgetQuery;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Skill API
 */
public class SkillAPI
{

    private static final int MAX_SKILL_LEVEL = 99;
    private static final int[] XP_TABLE;

    static {
        XP_TABLE = new int[127];
        XP_TABLE[0] = 0;

        for (int level = 1; level < XP_TABLE.length; level++) {
            double delta = 0;
            for (int i = 1; i < level; i++)
            {
                delta += Math.floor(i + 300 * Math.pow(2, i / 7.0));
            }

            XP_TABLE[level] = (int) Math.floor(delta / 4);
        }
    }

    public static int getExperienceAt(int level)
    {
        if (level < 0 || level >= XP_TABLE.length)
        {
            return 0;
        }

        return XP_TABLE[level];
    }

    public static int getLevelAt(int experience)
    {
        for (int i = XP_TABLE.length - 1; i > 0; i--)
        {
            if (i <= MAX_SKILL_LEVEL)
            {
                int experienceAtLevel = XP_TABLE[i];
                if (experience >= experienceAtLevel)
                {
                    return i;
                }
            }
        }

        return -1;
    }

    public static int getExperienceToNextLevel(Skill skill)
    {
        int nextLevel = getLevel(skill) + 1;
        if (nextLevel > MAX_SKILL_LEVEL)
        {
            return 0;
        }

        return getExperienceAt(nextLevel) - getExperience(skill);
    /**
     * Mapping of skills to their reward widget packed IDs.
     */
    private static final Map<Skill, Integer> skillRewardMap;
    static {
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

    public static int getLevel(Skill skill)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getRealSkillLevel(skill));
    }

    public static int getBoostedLevel(Skill skill)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getBoostedSkillLevel(skill));
    }

    public static int getExperience(Skill skill)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getSkillExperience(skill));
    }

    /**
     * Gets the reward widget in the experience rewards widget (lamp, book, ...) for a given skill.
     *
     * @param skill the preferred skill
     * @return the reward widget corresponding the requested skill
     */
    public static Widget getRewardWidget(Skill skill)
    {
        return WidgetAPI.get(skillRewardMap.get(skill));
    }

    /**
     * Check if the skill's reward widget can be selected. This can be false if for example the skill is not yet
     * unlocked (like herblore).
     *
     * <p>Skills where the 9th child widget has an opacity of 150 (grayed out) cannot be selected.
     *
     * @param skill the skill to check
     * @return true if the skill's reward widget can be selected, false otherwise
     */
    public static boolean canSelectReward(Skill skill)
    {
        Widget rewardWidget = getRewardWidget(skill);

        return rewardWidget != null
            && rewardWidget.getChild(9) != null
            && rewardWidget.getChild(9).getOpacity() != 150;
    }
}
