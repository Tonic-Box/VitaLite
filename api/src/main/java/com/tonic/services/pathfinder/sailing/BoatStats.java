package com.tonic.services.pathfinder.sailing;

import net.runelite.api.gameval.VarbitID;

import java.util.Objects;

/**
 * The boarded boat as its sidepanel reads it
 */
public final class BoatStats
{
    private final BoatType type;
    private final int hp;
    private final int maxHp;
    private final int repairKits;
    private final int baseSpeed;
    private final int speedCap;
    private final int acceleration;
    private final int boostDuration;
    private final int stormResistance;
    private final int rapidResistance;
    private final Resistances resistances;
    private final Tiers tiers;

    public BoatStats(BoatType type, int hp, int maxHp, int repairKits, int baseSpeed, int speedCap, int acceleration, int boostDuration, int stormResistance, int rapidResistance, Resistances resistances, Tiers tiers)
    {
        this.type = type;
        this.hp = hp;
        this.maxHp = maxHp;
        this.repairKits = repairKits;
        this.baseSpeed = baseSpeed;
        this.speedCap = speedCap;
        this.acceleration = acceleration;
        this.boostDuration = boostDuration;
        this.stormResistance = stormResistance;
        this.rapidResistance = rapidResistance;
        this.resistances = resistances;
        this.tiers = tiers;
    }

    /**
     * The hazards a boat shrugs off, each from a part
     */
    public static final class Resistances
    {
        private final boolean fetidWater;
        private final boolean crystal;
        private final boolean kelp;
        private final boolean icy;

        public Resistances(boolean fetidWater, boolean crystal, boolean kelp, boolean icy)
        {
            this.fetidWater = fetidWater;
            this.crystal = crystal;
            this.kelp = kelp;
            this.icy = icy;
        }

        public boolean fetidWater()
        {
            return fetidWater;
        }

        public boolean crystal()
        {
            return crystal;
        }

        public boolean kelp()
        {
            return kelp;
        }

        public boolean icy()
        {
            return icy;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Resistances))
            {
                return false;
            }
            Resistances other = (Resistances) o;
            return fetidWater == other.fetidWater && crystal == other.crystal && kelp == other.kelp && icy == other.icy;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(fetidWater, crystal, kelp, icy);
        }

        @Override
        public String toString()
        {
            return "Resistances[" + "fetidWater=" + fetidWater + ", " + "crystal=" + crystal + ", " + "kelp=" + kelp + ", " + "icy=" + icy + "]";
        }
    }

    /**
     * The tier, 0 to 6, of each of the boat's parts
     */
    public static final class Tiers
    {
        private final int sail;
        private final int helm;
        private final int keel;
        private final int hull;

        public Tiers(int sail, int helm, int keel, int hull)
        {
            this.sail = sail;
            this.helm = helm;
            this.keel = keel;
            this.hull = hull;
        }

        public int sail()
        {
            return sail;
        }

        public int helm()
        {
            return helm;
        }

        public int keel()
        {
            return keel;
        }

        public int hull()
        {
            return hull;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Tiers))
            {
                return false;
            }
            Tiers other = (Tiers) o;
            return sail == other.sail && helm == other.helm && keel == other.keel && hull == other.hull;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(sail, helm, keel, hull);
        }

        @Override
        public String toString()
        {
            return "Tiers[" + "sail=" + sail + ", " + "helm=" + helm + ", " + "keel=" + keel + ", " + "hull=" + hull + "]";
        }
    }

    /**
     * Reads the boarded boat's stats as the varbits hold them now
     * @param world the game state to read
     * @return the boarded boat's stats
     */
    static BoatStats of(SeaWorld world)
    {
        BoatType type = world.varbit(VarbitID.SAILING_BOARDED_BOAT) == 1 ? BoatType.of(world.varbit(VarbitID.SAILING_BOARDED_BOAT_TYPE)) : null;
        int hp = world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_HP);
        int maxHp = world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_HP_MAX);
        int kits = world.varbit(VarbitID.SAILING_SIDEPANEL_REPAIRKITS);
        int base = world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_BASESPEED);
        int cap = world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_SPEEDCAP);
        int acceleration = world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_ACCELERATION);
        int boost = world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_SPEEDBOOST_DURATION);
        int storm = world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_STORMRESISTANCE);
        int rapid = world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_RAPIDRESISTANCE);
        Resistances resistances = new Resistances(world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_FETIDWATER_RESISTANT) > 0, world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_CRYSTALFLECKED_RESISTANT) > 0, world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_TANGLEDKELP_RESISTANT) > 0, world.varbit(VarbitID.SAILING_SIDEPANEL_BOAT_ICYSEAS_RESISTANT) > 0);
        Tiers tiers = new Tiers(world.varbit(VarbitID.SAILING_SIDEPANEL_FACILITY_SAIL), world.varbit(VarbitID.SAILING_SIDEPANEL_FACILITY_HELM), world.varbit(VarbitID.SAILING_SIDEPANEL_FACILITY_KEEL), world.varbit(VarbitID.SAILING_SIDEPANEL_FACILITY_HULL));
        return new BoatStats(type, hp, maxHp, kits, base, cap, acceleration, boost, storm, rapid, resistances, tiers);
    }

    public BoatType type()
    {
        return type;
    }

    public int hp()
    {
        return hp;
    }

    public int maxHp()
    {
        return maxHp;
    }

    public int repairKits()
    {
        return repairKits;
    }

    public int baseSpeed()
    {
        return baseSpeed;
    }

    public int speedCap()
    {
        return speedCap;
    }

    public int acceleration()
    {
        return acceleration;
    }

    public int boostDuration()
    {
        return boostDuration;
    }

    public int stormResistance()
    {
        return stormResistance;
    }

    public int rapidResistance()
    {
        return rapidResistance;
    }

    public Resistances resistances()
    {
        return resistances;
    }

    public Tiers tiers()
    {
        return tiers;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof BoatStats))
        {
            return false;
        }
        BoatStats other = (BoatStats) o;
        return Objects.equals(type, other.type) && hp == other.hp && maxHp == other.maxHp && repairKits == other.repairKits && baseSpeed == other.baseSpeed && speedCap == other.speedCap && acceleration == other.acceleration && boostDuration == other.boostDuration && stormResistance == other.stormResistance && rapidResistance == other.rapidResistance && Objects.equals(resistances, other.resistances) && Objects.equals(tiers, other.tiers);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, hp, maxHp, repairKits, baseSpeed, speedCap, acceleration, boostDuration, stormResistance, rapidResistance, resistances, tiers);
    }

    @Override
    public String toString()
    {
        return "BoatStats[" + "type=" + type + ", " + "hp=" + hp + ", " + "maxHp=" + maxHp + ", " + "repairKits=" + repairKits + ", " + "baseSpeed=" + baseSpeed + ", " + "speedCap=" + speedCap + ", " + "acceleration=" + acceleration + ", " + "boostDuration=" + boostDuration + ", " + "stormResistance=" + stormResistance + ", " + "rapidResistance=" + rapidResistance + ", " + "resistances=" + resistances + ", " + "tiers=" + tiers + "]";
    }
}
