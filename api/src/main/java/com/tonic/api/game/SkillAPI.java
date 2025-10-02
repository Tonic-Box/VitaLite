package com.tonic.api.game;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * Skill API
 */
public class SkillAPI
{
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
