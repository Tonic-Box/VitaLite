package com.tonic.services.pathfinder.sailing;

import com.tonic.services.pathfinder.sailing.sea.BoatHull;
import com.tonic.services.pathfinder.sailing.sea.BoatPose;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

/**
 * What the sailing runtime reads from the game, each value as of the current tick
 */
interface SeaWorld
{
    int tick();

    int revision();

    int varbit(int id);

    /**
     * Gets the game's database for the dock table
     * @return the dock table, or null when there is none to read
     */
    Ports.DockTable dockTable();

    /**
     * Checks whether a courier crate is held in the hands
     * @return true if a courier crate is held, false otherwise
     */
    boolean holdingCargo();

    /**
     * Gets the player's boat
     * @return the player's boat, or null when not aboard one
     */
    Boat boat();

    /**
     * Gets the world entities other than the player's boat
     * @return the other boats
     */
    List<Vessel> vessels();

    /**
     * Gets the NPCs on the top-level world
     * @return the NPCs on the top-level world
     */
    List<Creature> npcs();

    /**
     * Finds the top-level scene's locs whose transformed id matches
     * @param ids test for the transformed loc ids to keep
     * @return the matching locs
     */
    List<Loc> locs(IntPredicate ids);

    /**
     * Gets the player's tile on the main world
     * @return the player's tile
     */
    WorldPoint playerTile();

    WorldPoint sceneBase();

    boolean moving();

    boolean optionPresent(String option);

    void selectOption(String option);

    void note(String message);

    <T> T invoke(Supplier<T> supplier);

    void invoke(Runnable runnable);

    /**
     * The player's boat
     */
    final class Boat
    {
        private final int id;
        private final BoatPose pose;
        private final BoatHull hull;

        Boat(int id, BoatPose pose, BoatHull hull)
        {
            this.id = id;
            this.pose = pose;
            this.hull = hull;
        }

        int id()
        {
            return id;
        }

        BoatPose pose()
        {
            return pose;
        }

        BoatHull hull()
        {
            return hull;
        }
    }

    /**
     * Another boat: where it was last tick and where it moves to this tick
     */
    final class Vessel
    {
        private final int id;
        private final boolean npcOwned;
        private final BoatPose now;
        private final BoatPose next;
        private final BoatHull hull;

        Vessel(int id, boolean npcOwned, BoatPose now, BoatPose next, BoatHull hull)
        {
            this.id = id;
            this.npcOwned = npcOwned;
            this.now = now;
            this.next = next;
            this.hull = hull;
        }

        int id()
        {
            return id;
        }

        boolean npcOwned()
        {
            return npcOwned;
        }

        BoatPose now()
        {
            return now;
        }

        BoatPose next()
        {
            return next;
        }

        BoatHull hull()
        {
            return hull;
        }
    }

    /**
     * An NPC on the top-level world
     */
    final class Creature
    {
        private final int index;
        private final int id;
        private final String name;
        private final WorldPoint tile;
        private final int size;
        private final int animation;

        Creature(int index, int id, String name, WorldPoint tile, int size, int animation)
        {
            this.index = index;
            this.id = id;
            this.name = name;
            this.tile = tile;
            this.size = size;
            this.animation = animation;
        }

        int index()
        {
            return index;
        }

        int id()
        {
            return id;
        }

        String name()
        {
            return name;
        }

        WorldPoint tile()
        {
            return tile;
        }

        int size()
        {
            return size;
        }

        int animation()
        {
            return animation;
        }
    }

    /**
     * A loc on the top-level scene, its size turned by its rotation
     */
    final class Loc
    {
        private final int id;
        private final WorldPoint corner;
        private final int sizeX;
        private final int sizeY;

        Loc(int id, WorldPoint corner, int sizeX, int sizeY)
        {
            this.id = id;
            this.corner = corner;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
        }

        int id()
        {
            return id;
        }

        WorldPoint corner()
        {
            return corner;
        }

        int sizeX()
        {
            return sizeX;
        }

        int sizeY()
        {
            return sizeY;
        }
    }
}
