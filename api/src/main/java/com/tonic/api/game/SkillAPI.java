package com.tonic.api.game;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.Skill;

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
}
