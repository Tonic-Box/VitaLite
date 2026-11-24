package com.tonic.services.pathfinder.requirements;

import com.tonic.api.game.SkillAPI;
import com.tonic.api.game.WorldsAPI;
import lombok.Value;
import net.runelite.api.Skill;

@Value
public class SkillRequirement implements Requirement
{
    Skill skill;
    int level;
    boolean boostable;

    /**
     * A base level, boosts not counting.
     *
     * @param skill the skill
     * @param level the level needed
     */
    public SkillRequirement(Skill skill, int level)
    {
        this(skill, level, false);
    }

    /**
     * @param skill the skill
     * @param level the level needed
     * @param boostable whether a boosted level counts
     */
    public SkillRequirement(Skill skill, int level, boolean boostable)
    {
        this.skill = skill;
        this.level = level;
        this.boostable = boostable;
    }

    @Override
    public Boolean get()
    {
        if(SkillAPI.MEMBER_SKILLS.contains(skill) && !WorldsAPI.inMembersWorld())
        {
            return false;
        }

        return (boostable ? SkillAPI.getBoostedLevel(skill) : SkillAPI.getLevel(skill)) >= level;
    }
}

