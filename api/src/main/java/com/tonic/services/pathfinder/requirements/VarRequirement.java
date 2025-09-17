package com.tonic.services.pathfinder.requirements;

import lombok.Value;

@Value
public class VarRequirement implements Requirement
{
    Comparison comparison;
    VarType type;
    int var;
    int value;

    @Override
    public Boolean get()
    {
        return comparison.apply(type.apply(var), value);
    }
}
