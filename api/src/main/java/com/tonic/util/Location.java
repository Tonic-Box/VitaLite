package com.tonic.util;

import com.tonic.Static;
import com.tonic.api.game.SceneAPI;
import com.tonic.services.pathfinder.LocalPathfinder;
import com.tonic.services.pathfinder.implimentations.hybridbfs.HybridBFSStep;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Various location related utility methods.
 */
public class Location {

    /**
     * check if a world point is inside an area
     * @param point point
     * @param sw southwest world point of area
     * @param nw northwest world point of area
     * @return boolean
     */
    public static boolean inArea(WorldPoint point, WorldPoint sw, WorldPoint nw) {
        return inArea(point.getX(), point.getY(), sw.getX(), sw.getY(), nw.getX(), nw.getY());
    }

    /**
     * check if a world point is inside an area
     * @param point_x point x
     * @param point_y point y
     * @param x1_sw sw x
     * @param y1_sw sw y
     * @param x2_ne ne x
     * @param y2_ne ne y
     * @return boolean
     */
    public static boolean inArea(int point_x, int point_y, int x1_sw, int y1_sw, int x2_ne, int y2_ne) {
        Client client = Static.getClient();
        if (!client.getGameState().equals(GameState.LOGGED_IN) && !client.getGameState().equals(GameState.LOADING))
            return false;
        return point_x > x1_sw && point_x < x2_ne && point_y > y1_sw && point_y < y2_ne;
    }

    /**
     * check if player is inside an area
     * @param sw southwest world point of area
     * @param nw northwest world point of area
     * @return boolean
     */
    public static boolean inArea(WorldPoint sw, WorldPoint nw) {
        return inArea(sw.getX(), sw.getY(), nw.getX(), nw.getY());
    }

    /**
     * check if player is inside an area
     * @param x1_sw sw x
     * @param y1_sw sw y
     * @param x2_ne ne x
     * @param y2_ne ne y
     * @return boolean
     */
    public static boolean inArea(int x1_sw, int y1_sw, int x2_ne, int y2_ne) {
        Client client = Static.getClient();
        if (!client.getGameState().equals(GameState.LOGGED_IN) && !client.getGameState().equals(GameState.LOADING))
            return false;
        WorldPoint player = client.getLocalPlayer().getWorldLocation();
        return player.getX() > x1_sw && player.getX() < x2_ne && player.getY() > y1_sw && player.getY() < y2_ne;
    }

    @Getter
    private static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    @Getter
    private static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);

    public static boolean isInWilderness(WorldPoint p) {
        return WILDERNESS_ABOVE_GROUND.distanceTo(p) == 0 ||
                WILDERNESS_UNDERGROUND.distanceTo(p) == 0;
    }
}