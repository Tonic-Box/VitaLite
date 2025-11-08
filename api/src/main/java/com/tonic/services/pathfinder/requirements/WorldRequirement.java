package com.tonic.services.pathfinder.requirements;

import com.tonic.api.game.WorldsAPI;
import lombok.Value;

@Value
public class WorldRequirement implements Requirement
{
    boolean memberWorld;

    @Override
    public Boolean get()
    {
        return !memberWorld || WorldsAPI.inMembersWorld();
    }
}