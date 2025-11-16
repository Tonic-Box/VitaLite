package com.tonic.services.pathfinder.requirements;

import com.tonic.api.game.SkillAPI;
import com.tonic.api.game.WorldsAPI;
import lombok.Value;
import net.runelite.api.Skill;

import java.util.Set;

@Value
public class SkillRequirement implements Requirement
{
    Skill skill;
    int level;

    @Override
    public Boolean get()
    {
        if(SkillAPI.MEMBER_SKILLS.contains(skill) && !WorldsAPI.inMembersWorld())
        {
            return false;
        }

        return SkillAPI.getLevel(skill) >= level;
    }
}

