package com.tonic.services.pathfinder.requirements;

public class RequirementsBuilder
{
    public static RequirementsBuilder get()
    {
        return new RequirementsBuilder();
    }
    private final Requirements requirements;

    RequirementsBuilder()
    {
        requirements = new Requirements();
    }

    public RequirementsBuilder addRequirement(Requirement requirement)
    {
        requirements.addRequirement(requirement);
        return this;
    }

    public RequirementsBuilder addRequirements(Requirements requirements)
    {
        this.requirements.addRequirements(requirements.getAll());
        return this;
    }

    public Requirements build()
    {
        return requirements;
    }
}