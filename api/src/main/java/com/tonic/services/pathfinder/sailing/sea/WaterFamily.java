package com.tonic.services.pathfinder.sailing.sea;

import net.runelite.api.gameval.VarbitID;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * The kinds of water the sea map tells apart, numbered as the map stores them
 */
public enum WaterFamily
{
    NONE(0),
    WATER(1),
    CRANDOR_SMEGMA(2),
    TEMPOR_STORM(3),
    DISEASE(4),
    KELP(5),
    SUNBAKED(6),
    JAGGED_REEFS(7),
    SHARP_CRYSTAL(8),
    ICE(9),
    NORTH_EAST_PURPLE_GREY(10),
    NORTH_WEST_GREY(11),
    SOUTH_EAST_PURPLE(12),
    INLAND(13);

    private static final WaterFamily[] BY_NUMBER = values();

    private final int number;

    WaterFamily(int number)
    {
        this.number = number;
    }

    /**
     * Gets the family's number in the sea map
     * @return the family's number
     */
    public int number()
    {
        return number;
    }

    /**
     * Gets the family with a number
     * @param number Family number in the sea map
     * @return the family
     */
    public static WaterFamily of(int number)
    {
        if (number < 0 || number >= BY_NUMBER.length)
        {
            throw new IndexOutOfBoundsException("number=" + number);
        }
        return BY_NUMBER[number];
    }

    /**
     * Gets the families the boat the player sails must keep out of, by its resistances
     * @param varbit Reads a varbit's value by its id
     * @return the avoided families
     */
    public static Set<WaterFamily> avoided(IntUnaryOperator varbit)
    {
        if (varbit == null)
        {
            throw new IllegalArgumentException("varbit cannot be null");
        }
        Set<WaterFamily> avoided = EnumSet.of(NONE, INLAND, CRANDOR_SMEGMA, JAGGED_REEFS, SUNBAKED, NORTH_EAST_PURPLE_GREY, NORTH_WEST_GREY, SOUTH_EAST_PURPLE);
        if (varbit.applyAsInt(VarbitID.SAILING_SIDEPANEL_BOAT_RAPIDRESISTANCE) <= 0 || varbit.applyAsInt(VarbitID.SAILING_SIDEPANEL_BOAT_STORMRESISTANCE) <= 0)
        {
            avoided.add(TEMPOR_STORM);
        }
        if (varbit.applyAsInt(VarbitID.SAILING_SIDEPANEL_BOAT_CRYSTALFLECKED_RESISTANT) <= 0)
        {
            avoided.add(SHARP_CRYSTAL);
        }
        if (varbit.applyAsInt(VarbitID.SAILING_SIDEPANEL_BOAT_TANGLEDKELP_RESISTANT) <= 0)
        {
            avoided.add(KELP);
        }
        if (varbit.applyAsInt(VarbitID.SAILING_SIDEPANEL_BOAT_ICYSEAS_RESISTANT) <= 0)
        {
            avoided.add(ICE);
        }
        if (varbit.applyAsInt(VarbitID.SAILING_SIDEPANEL_BOAT_FETIDWATER_RESISTANT) <= 0)
        {
            avoided.add(DISEASE);
        }
        return avoided;
    }
}
