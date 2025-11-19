package com.tonic.data;

import com.tonic.data.wrappers.ActorEx;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.TileObject;

@Getter
public enum Orientation
{
    SOUTH(0),
    SOUTH_WEST(1),
    WEST(2),
    NORTH_WEST(3),
    NORTH(4),
    NORTH_EAST(5),
    EAST(6),
    SOUTH_EAST(7)
    ;

    private final int value;

    Orientation(int value)
    {
        this.value = value;
    }

    public static Orientation of(int value)
    {
        if(value > 7)
        {
            value /= 256;
        }
        for (Orientation orientation : values())
        {
            if (orientation.value == value)
            {
                return orientation;
            }
        }
        return null;
    }

    public static Orientation of(ActorEx<?> actor)
    {
        for (Orientation orientation : values())
        {
            if (orientation.value == (actor.getActor().getOrientation() / 256))
            {
                return orientation;
            }
        }
        return null;
    }
}