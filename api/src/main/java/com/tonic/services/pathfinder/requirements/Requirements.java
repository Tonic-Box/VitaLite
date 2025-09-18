package com.tonic.services.pathfinder.requirements;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Value
public class Requirements
{
    List<ItemRequirement> itemRequirements = new ArrayList<>();
    List<SkillRequirement> skillRequirements = new ArrayList<>();
    List<VarRequirement> varRequirements = new ArrayList<>();
    List<QuestRequirement> questRequirements = new ArrayList<>();
    List<WorldRequirement> worldRequirements = new ArrayList<>();
    List<OtherRequirement> otherRequirements = new ArrayList<>();

    public Requirements() {
    }

    public Requirements(List<ItemRequirement> itemRequirements, List<SkillRequirement> skillRequirements, List<VarRequirement> varRequirements, List<QuestRequirement> questRequirements, List<WorldRequirement> worldRequirements, List<OtherRequirement> otherRequirements) {
        this.itemRequirements.addAll(itemRequirements);
        this.skillRequirements.addAll(skillRequirements);
        this.varRequirements.addAll(varRequirements);
        this.questRequirements.addAll(questRequirements);
        this.worldRequirements.addAll(worldRequirements);
        this.otherRequirements.addAll(otherRequirements);
    }

    public boolean fulfilled()
    {
        List<Requirement> all = getAll();
        return all.stream().allMatch(Supplier::get);
    }

    public List<Requirement> getAll()
    {
        List<Requirement> all = new ArrayList<>();
        all.addAll(itemRequirements);
        all.addAll(skillRequirements);
        all.addAll(varRequirements);
        all.addAll(questRequirements);
        all.addAll(worldRequirements);
        all.addAll(otherRequirements);
        return all;
    }

    public void addRequirements(Requirement... reqs)
    {
        for(Requirement req : reqs)
        {
            addRequirement(req);
        }
    }

    public void addRequirements(List<Requirement> reqs)
    {
        for(Requirement req : reqs)
        {
            addRequirement(req);
        }
    }

    public void addRequirement(Requirement req)
    {
        if(req instanceof ItemRequirement)
        {
            itemRequirements.add((ItemRequirement) req);
        }
        else if(req instanceof SkillRequirement)
        {
            skillRequirements.add((SkillRequirement) req);
        }
        else if(req instanceof VarRequirement)
        {
            varRequirements.add((VarRequirement) req);
        }
        else if(req instanceof QuestRequirement)
        {
            questRequirements.add((QuestRequirement) req);
        }
        else if(req instanceof WorldRequirement)
        {
            worldRequirements.add((WorldRequirement) req);
        }
        else if(req instanceof OtherRequirement)
        {
            otherRequirements.add((OtherRequirement) req);
        }
    }
}