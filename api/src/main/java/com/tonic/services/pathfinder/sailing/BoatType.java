package com.tonic.services.pathfinder.sailing;

/**
 * A kind of boat, as the boat slot TYPE varbits and SAILING_BOARDED_BOAT_TYPE hold it
 */
public enum BoatType
{
    RAFT(0, 1, 0),
    SKIFF(1, 15, 2),
    SLOOP(2, 50, 5),
    /**
     * The boat Pandemonium lends before the raft is the player's own
     */
    TUTORIAL(3, 1, 0);

    private final int value;
    private final int level;
    private final int crew;

    BoatType(int value, int level, int crew)
    {
        this.value = value;
        this.level = level;
        this.crew = crew;
    }

    /**
     * Gets the varbit's value for this type
     * @return the varbit value
     */
    public int value()
    {
        return value;
    }

    /**
     * Gets the Sailing level the boat needs
     * @return the Sailing level
     */
    public int level()
    {
        return level;
    }

    /**
     * Gets how many crewmates the boat carries
     * @return the crew size
     */
    public int crew()
    {
        return crew;
    }

    /**
     * Finds the type for a varbit value
     * @param value the varbit value
     * @return the type, or null for a value no type has
     */
    public static BoatType of(int value)
    {
        for (BoatType type : values())
        {
            if (type.value == value)
            {
                return type;
            }
        }
        return null;
    }
}
