package com.tonic.api.game;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.Skill;

public class Skills {
    public static int getBoostedLevel(Skill skill)
    {
        Client client = Static.getClient();
        return client.getBoostedSkillLevel(skill);
    }

    public static int getLevel(Skill skill)
    {
        Client client = Static.getClient();
        return client.getRealSkillLevel(skill);
    }

    public static int getExperience(Skill skill)
    {
        Client client = Static.getClient();
        return client.getSkillExperience(skill);
    }
}
