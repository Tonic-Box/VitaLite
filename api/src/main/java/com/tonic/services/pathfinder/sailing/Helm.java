package com.tonic.services.pathfinder.sailing;

import net.runelite.api.coords.WorldPoint;

/**
 * What the sail driver does to the world, each call one tick's action on the client thread
 */
interface Helm
{
    /**
     * Steers toward a compass point, as "Set heading" on the water does
     * @param heading the compass point to steer toward
     */
    void setHeading(int heading);

    /**
     * Clicks a child of the sidepanel's facilities layer
     * @param child the child of the facilities layer to click
     */
    void throttle(int child);

    /**
     * Clicks the helm on the deck
     * @return whether the helm was clicked
     */
    boolean takeHelm();

    /**
     * Steps away from the helm onto a free deck tile, which leaves it
     * @return whether the step away from the helm was clicked
     */
    boolean leaveHelm();

    /**
     * Deposits the held cargo into the deck's cargo hold
     * @return whether the cargo hold offered Deposit-held and it was clicked
     */
    boolean stowCargo();

    /**
     * Trims the deck's sails
     * @return whether the sails offered Trim and it was clicked
     */
    boolean trimSails();

    /**
     * Checks whether the deck's sails offer Trim
     * @return true if the sails offer Trim
     */
    boolean canTrim();

    /**
     * Clicks the port's gangplank from the boat
     * @param port the port to dock at
     * @return whether the gangplank was clicked
     */
    boolean dock(Port port);

    /**
     * Starts the land walk to the port's gangplank
     * @param port the port whose gangplank to walk to
     */
    void walkToGangplank(Port port);

    /**
     * Clicks the port's gangplank from the shore
     * @param port the port to board at
     * @return whether the gangplank was clicked
     */
    boolean board(Port port);

    /**
     * Starts the land walk to a tile
     * @param tile the tile to walk to
     */
    void walkTo(WorldPoint tile);

    /**
     * Stops a land walk under way
     */
    void stopWalking();

    /**
     * Asks a nearby shipwright to recover a boat
     * @param shipwright the shipwright's NPC id
     * @return whether the shipwright stands nearby and was asked
     */
    boolean recoverBoat(int shipwright);

    /**
     * Picks a boat on the open boat choice
     * @param slot the boat's slot
     * @return whether the boat in that slot was picked
     */
    boolean chooseSlot(int slot);

    /**
     * Continues an open dialogue
     * @return whether an open dialogue was continued
     */
    boolean continueDialogue();
}
