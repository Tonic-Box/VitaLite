package com.tonic.util;

import com.tonic.Static;
import com.tonic.api.game.SceneAPI;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;

import java.util.ArrayList;
import java.util.List;

public class Location {
    /**
     * get the distance between 2 world points
     *
     * @param wp1 world point 1
     * @param wp2 world point 2
     * @return distance
     */
    public static int getDistance(WorldPoint wp1, WorldPoint wp2) {
        return (wp1.getPlane() == wp2.getPlane()) ?
                getDistance(wp1.getX(), wp1.getY(), wp2.getX(), wp2.getY()) :
                Integer.MAX_VALUE;
    }

    /**
     * get the distance between 2 world points
     *
     * @param x1 world point x 1
     * @param y1 world point y 1
     * @param x2 world point x 2
     * @param y2 world point y 2
     * @return distance;
     */
    public static int getDistance(int x1, int y1, int x2, int y2) {
        return (int) Math.hypot(x1 - x2, y1 - y2);
    }

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
     * @return
     */
    public static boolean inArea(int x1_sw, int y1_sw, int x2_ne, int y2_ne) {
        Client client = Static.getClient();
        if (!client.getGameState().equals(GameState.LOGGED_IN) && !client.getGameState().equals(GameState.LOADING))
            return false;
        WorldPoint player = client.getLocalPlayer().getWorldLocation();
        return player.getX() > x1_sw && player.getX() < x2_ne && player.getY() > y1_sw && player.getY() < y2_ne;
    }

    public static Tile toTile(WorldPoint wp)
    {
        Client client = Static.getClient();
        WorldView wv = client.getTopLevelWorldView();
        LocalPoint lp = LocalPoint.fromWorld(wv, wp.getX(), wp.getY());
        if(lp == null)
            return null;
        try
        {
            return wv.getScene().getTiles()[wp.getPlane()][lp.getSceneX()][lp.getSceneY()];
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    /**
     * check if a world point is reachable from another world point
     * @param start world point
     * @param end target world point
     * @return boolean
     */
    public static boolean isReachable(WorldPoint start, WorldPoint end) {
        if (start.getPlane() != end.getPlane()) {
            return false;
        }

        Client client = Static.getClient();
        LocalPoint sourceLp = LocalPoint.fromWorld(client, start.getX(), start.getY());
        LocalPoint targetLp = LocalPoint.fromWorld(client, end.getX(), end.getY());
        if (sourceLp == null || targetLp == null) {
            return false;
        }

        int thisX = sourceLp.getSceneX();
        int thisY = sourceLp.getSceneY();
        int otherX = targetLp.getSceneX();
        int otherY = targetLp.getSceneY();

        try {
            Tile[][][] tiles = client.getScene().getTiles();
            Tile sourceTile = tiles[start.getPlane()][thisX][thisY];
            Tile targetTile = tiles[end.getPlane()][otherX][otherY];
            return isReachable(sourceTile, targetTile);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static List<Tile> pathTo(WorldPoint start, WorldPoint end) {
        if (start.getPlane() != end.getPlane()) {
            return new ArrayList<>();
        }

        Client client = Static.getClient();
        LocalPoint sourceLp = LocalPoint.fromWorld(client, start.getX(), start.getY());
        LocalPoint targetLp = LocalPoint.fromWorld(client, end.getX(), end.getY());
        if (sourceLp == null || targetLp == null) {
            return new ArrayList<>();
        }

        int thisX = sourceLp.getSceneX();
        int thisY = sourceLp.getSceneY();
        int otherX = targetLp.getSceneX();
        int otherY = targetLp.getSceneY();

        try {
            Tile[][][] tiles = client.getScene().getTiles();
            Tile source = tiles[start.getPlane()][thisX][thisY];
            Tile dest = tiles[end.getPlane()][otherX][otherY];
            return SceneAPI.pathTo(source, dest);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public static  boolean isReachable(Tile source, Tile dest) {
        List<Tile> path  = SceneAPI.pathTo(source, dest);
        return path != null && path.get(path.size()-1) == dest;
    }

    /**
     * get the respective RSTile from a world point
     * @param wp world point
     * @return RSTile
     */
    public static Tile getTile(WorldPoint wp)
    {
        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        LocalPoint lp = LocalPoint.fromWorld(worldView, wp.getX(), wp.getY());
        Tile[][][] tiles = worldView.getScene().getTiles();
        if(lp == null || tiles == null)
            return null;
        try
        {
            return tiles[wp.getPlane()][lp.getSceneX()][lp.getSceneY()];
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    public static boolean within(WorldPoint wp1, WorldPoint wp2, int x)
    {
        return within(wp1.getX(), wp1.getY(), wp2.getX(), wp2.getY(), x);
    }

    public static boolean within(int x1, int y1, int x2, int y2, int x) {
        return getDistance(x1, y1, x2, y2) <= x;
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