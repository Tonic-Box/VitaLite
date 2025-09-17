package com.tonic.services.pathfinder.requirements;

import com.tonic.api.game.VarAPI;

import java.util.function.Function;

public enum VarType implements Function<Integer, Integer>
{
    VARBIT,
    VARP;

    @Override
    public Integer apply(Integer index)
    {
        switch (this)
        {
            case VARBIT:
                return VarAPI.getVar(index);
            case VARP:
                return VarAPI.getVarp(index);
        }
        return 0;
    }
}
