package com.tonic.services.pathfinder.requirements;

import com.tonic.Static;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Skill;

@Value
public class SkillRequirement implements Requirement
{
    Skill skill;
    int level;

    @Override
    public Boolean get()
    {
        Client client = Static.getClient();
        return client.getRealSkillLevel(skill) >= level;
    }
}

