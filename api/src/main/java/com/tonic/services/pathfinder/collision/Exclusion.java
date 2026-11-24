package com.tonic.services.pathfinder.collision;

import net.runelite.api.ObjectComposition;

import java.util.HashSet;
import java.util.Set;

/**
 * Which locs stand in the walker's way: a door, gate or curtain is passable because the walker opens it on the way, unless it is one of the doors listed here that it cannot open; a trapdoor or a loc with nothing to do to it always blocks. The collision map was dumped by the same rule.
 */
public final class Exclusion
{
    /**
     * Draynor Manor's front doors and back door.
     */
    private static final int[] DRAYNOR_MANOR = {135, 134, 136};

    /**
     * Misthalin Manor's front doors and gem doors, keyed or locked through Misthalin Mystery.
     */
    private static final int[] MISTHALIN_MANOR = {30110, 30111, 30112, 30113, 30114, 30115, 30116, 30117, 30118, 30119};

    /**
     * Al Kharid's toll gate.
     */
    private static final int[] AL_KHARID_TOLL_GATE = {44598, 44599};

    /**
     * The Wizards' Guild doors.
     */
    private static final int[] MAGIC_GUILD = {1733, 1732};

    /**
     * Lumbridge Castle's dining room doors.
     */
    private static final int[] LUMBRIDGE_DINING_ROOM = {12348, 12349, 12350};

    /**
     * The Varrock Museum doors and guard barrier, and the Gnome Stronghold's front gate.
     */
    private static final int[] MUSEUM_AND_STRONGHOLD_GATE = {24565, 24567, 24536, 190};

    /**
     * The Witch's House shed door.
     */
    private static final int[] WITCHS_SHED = {2863};

    /**
     * Burgh de Rott's north gate, locked until In Aid of the Myreque's food is in the chest.
     */
    private static final int[] BURGH_DE_ROTT_GATE = {12816, 12817};

    /**
     * McGrubor's Wood gate.
     */
    private static final int[] MCGRUBOR_GATE = {52, 53};

    /**
     * The West Ardougne stew door.
     */
    private static final int[] WEST_ARDOUGNE = {2036};

    /**
     * Dragon Slayer's magic door, Melzar's maze door and its coloured doors.
     */
    private static final int[] DRAGON_SLAYER = {25115, 2595, 2596, 2597, 2598, 2599, 2600, 2601};

    /**
     * Port Sarim jail's locked door.
     */
    private static final int[] SARIM_JAIL = {9563};

    /**
     * The Black Knights' Fortress doors that cannot be opened.
     */
    private static final int[] BLACK_KNIGHTS_FORTRESS = {73, 74};

    /**
     * The Grand Tree's doors.
     */
    private static final int[] GRAND_TREE = {1967, 1968};

    /**
     * The Khazard stronghold door in Tree Gnome Village.
     */
    private static final int[] TREE_GNOME_VILLAGE = {2184};

    /**
     * The Woodcutting Guild gates.
     */
    private static final int[] WOODCUTTING_GUILD = {28851, 28852};

    /**
     * The Varrock Museum's east gate.
     */
    private static final int[] MUSEUM_EAST_GATE = {24560, 24561};

    /**
     * Closed doors and gates that must block although their name says the walker opens them: locked, quest-gated, toll or one-way, crossed only by a transport that knows how.
     */
    public static final Set<Integer> BLOCKING_DOORS = setOf(DRAYNOR_MANOR, MISTHALIN_MANOR, AL_KHARID_TOLL_GATE, MAGIC_GUILD, LUMBRIDGE_DINING_ROOM, MUSEUM_AND_STRONGHOLD_GATE, WITCHS_SHED, BURGH_DE_ROTT_GATE, MCGRUBOR_GATE, WEST_ARDOUGNE, DRAGON_SLAYER, SARIM_JAIL, BLACK_KNIGHTS_FORTRESS, GRAND_TREE, TREE_GNOME_VILLAGE, WOODCUTTING_GUILD, MUSEUM_EAST_GATE);

    /**
     * The Keldagrim consortium's seated secretaries, whose tiles the server never lets a player onto.
     */
    private static final int[][] KELDAGRIM_SECRETARIES = {{2890, 10192, 1}, {2891, 10196, 1}, {2890, 10207, 1}, {2889, 10211, 1}, {2869, 10205, 1}};

    /**
     * The Lost Tribe tunnel traps, which drop a player stepping on one into the swamp caves.
     */
    private static final int[][] LOST_TRIBE_TRAPS = {{3249, 9646, 0}, {3250, 9646, 0}, {3255, 9616, 0}, {3256, 9616, 0}, {3260, 9636, 0}, {3260, 9637, 0}, {3269, 9617, 0}, {3269, 9618, 0}, {3273, 9641, 0}, {3273, 9642, 0}, {3238, 9622, 0}, {3239, 9622, 0}, {3244, 9635, 0}, {3244, 9636, 0}, {3275, 9630, 0}, {3275, 9631, 0}, {3287, 9631, 0}, {3288, 9631, 0}, {3301, 9618, 0}, {3302, 9618, 0}};

    /**
     * Tiles a path never crosses although no loc blocks them, as {x, y, plane}.
     */
    public static final int[][] BLOCKED_TILES = concat(KELDAGRIM_SECRETARIES, LOST_TRIBE_TRAPS);

    private Exclusion()
    {
    }

    /**
     * Whether a loc stands in the walker's way.
     *
     * @param definition the loc's definition, transformed where it transforms, or null when there is none
     * @param id the loc's id
     * @return true when a path may not cross it
     */
    public static boolean blocks(ObjectComposition definition, int id)
    {
        String name = definition == null || definition.getName() == null ? "" : definition.getName().toLowerCase();
        boolean block = BLOCKING_DOORS.contains(id) || !opensOnTheWay(name);
        String[] actions = definition == null ? null : definition.getActions();
        if (name.contains("trapdoor") || actions == null || actions.length == 0 || actions[0] == null)
        {
            block = true;
        }
        return block;
    }

    /**
     * Whether a loc of this name is one the walker opens when a route runs through it.
     *
     * @param name the loc's name, null when it has none
     * @return true for a door, gate or curtain
     */
    public static boolean opensOnTheWay(String name)
    {
        if (name == null)
        {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.contains("door") || lower.contains("gate") || lower.contains("curtain");
    }

    private static Set<Integer> setOf(int[]... groups)
    {
        Set<Integer> ids = new HashSet<>();
        for (int[] group : groups)
        {
            for (int id : group)
            {
                ids.add(id);
            }
        }
        return ids;
    }

    private static int[][] concat(int[][]... groups)
    {
        int count = 0;
        for (int[][] group : groups)
        {
            count += group.length;
        }
        int[][] all = new int[count][];
        int at = 0;
        for (int[][] group : groups)
        {
            for (int[] tile : group)
            {
                all[at++] = tile;
            }
        }
        return all;
    }
}
