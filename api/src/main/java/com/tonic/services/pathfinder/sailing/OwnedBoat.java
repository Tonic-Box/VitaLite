package com.tonic.services.pathfinder.sailing;

import java.util.Objects;

/**
 * One of the player's five boat slots
 */
final class OwnedBoat
{
    private final int slot;
    private final BoatType type;
    private final int dock;
    private final boolean capsized;
    private final boolean bottled;
    private final int keel;
    private final int hull;
    private final int sail;
    private final int steering;
    private final int teleportFocus;
    private final int hp;
    private final int maxHp;

    public OwnedBoat(int slot, BoatType type, int dock, boolean capsized, boolean bottled, int keel, int hull, int sail, int steering, int teleportFocus, int hp, int maxHp)
    {
        this.slot = slot;
        this.type = type;
        this.dock = dock;
        this.capsized = capsized;
        this.bottled = bottled;
        this.keel = keel;
        this.hull = hull;
        this.sail = sail;
        this.steering = steering;
        this.teleportFocus = teleportFocus;
        this.hp = hp;
        this.maxHp = maxHp;
    }

    /**
     * Checks whether the boat is moored at a dock
     * @param dockId the dock id
     * @return true if the boat is moored there, false otherwise
     */
    public boolean dockedAt(int dockId)
    {
        return dock >= 0 && dock == dockId;
    }

    public int slot()
    {
        return slot;
    }

    public BoatType type()
    {
        return type;
    }

    public int dock()
    {
        return dock;
    }

    public boolean capsized()
    {
        return capsized;
    }

    public boolean bottled()
    {
        return bottled;
    }

    public int keel()
    {
        return keel;
    }

    public int hull()
    {
        return hull;
    }

    public int sail()
    {
        return sail;
    }

    public int steering()
    {
        return steering;
    }

    public int teleportFocus()
    {
        return teleportFocus;
    }

    public int hp()
    {
        return hp;
    }

    public int maxHp()
    {
        return maxHp;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof OwnedBoat))
        {
            return false;
        }
        OwnedBoat other = (OwnedBoat) o;
        return slot == other.slot && Objects.equals(type, other.type) && dock == other.dock && capsized == other.capsized && bottled == other.bottled && keel == other.keel && hull == other.hull && sail == other.sail && steering == other.steering && teleportFocus == other.teleportFocus && hp == other.hp && maxHp == other.maxHp;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(slot, type, dock, capsized, bottled, keel, hull, sail, steering, teleportFocus, hp, maxHp);
    }

    @Override
    public String toString()
    {
        return "OwnedBoat[" + "slot=" + slot + ", " + "type=" + type + ", " + "dock=" + dock + ", " + "capsized=" + capsized + ", " + "bottled=" + bottled + ", " + "keel=" + keel + ", " + "hull=" + hull + ", " + "sail=" + sail + ", " + "steering=" + steering + ", " + "teleportFocus=" + teleportFocus + ", " + "hp=" + hp + ", " + "maxHp=" + maxHp + "]";
    }
}
