package com.tonic.api.game;

import com.tonic.Static;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Scene API
 */
public class SceneAPI {

    /**
     * Returns a list of all reachable tiles from the player's current position using a breadth-first search algorithm.
     * This method considers the collision data to determine which tiles can be reached.
     *
     * @return A list of WorldPoint objects representing all reachable tiles.
     */
    public static List<WorldPoint> reachableTiles()
    {
        Client client = Static.getClient();
        return reachableTiles(client.getLocalPlayer().getWorldLocation());
    }

    /**
     * Returns a list of all reachable tiles from the origins position using a breadth-first search algorithm.
     * This method considers the collision data to determine which tiles can be reached.
     *
     * @param origin The point to query from
     * @return A list of WorldPoint objects representing all reachable tiles from the origin.
     */
    public static List<WorldPoint> reachableTiles(WorldPoint origin) {
        Client client = Static.getClient();
        boolean[][] visited = new boolean[104][104];
        CollisionData[] collisionData = client.getTopLevelWorldView().getCollisionMaps();
        if (collisionData == null) {
            return new ArrayList<>();
        }
        WorldView worldView = client.getTopLevelWorldView();
        int[][] flags = collisionData[worldView.getPlane()].getFlags();
        int firstPoint = (origin.getX()-worldView.getBaseX() << 16) | origin.getY()-worldView.getBaseY();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(firstPoint);
        while (!queue.isEmpty()) {
            int point = queue.poll();
            short x =(short)(point >> 16);
            short y = (short)point;
            if (y < 0 || x < 0 || y > 104 || x > 104) {
                continue;
            }
            if ((flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0 && (flags[x][y - 1] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && !visited[x][y - 1]) {
                queue.add((x << 16) | (y - 1));
                visited[x][y - 1] = true;
            }
            if ((flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0 && (flags[x][y + 1] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && !visited[x][y + 1]) {
                queue.add((x << 16) | (y + 1));
                visited[x][y + 1] = true;
            }
            if ((flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0 && (flags[x - 1][y] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && !visited[x - 1][y]) {
                queue.add(((x - 1) << 16) | y);
                visited[x - 1][y] = true;
            }
            if ((flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0 && (flags[x + 1][y] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && !visited[x + 1][y]) {
                queue.add(((x + 1) << 16) | y);
                visited[x + 1][y] = true;
            }
        }
        int baseX = worldView.getBaseX();
        int baseY = worldView.getBaseY();
        int plane = worldView.getPlane();
        List<WorldPoint> finalPoints = new ArrayList<>();
        for (int x = 0; x < 104; ++x) {
            for (int y = 0; y < 104; ++y) {
                if (visited[x][y]) {
                    finalPoints.add(new WorldPoint(baseX + x, baseY + y, plane));
                }
            }
        }
        return finalPoints;
    }

    /**
     * Returns a list of all tiles in the current scene that match the given filter.
     * @param filter A predicate to filter the tiles.
     * @return A list of tiles that match the filter.
     */
    public static List<Tile> getAll(Predicate<Tile> filter)
    {
        List<Tile> out = new ArrayList<>();
        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        for (int x = 0; x < Constants.SCENE_SIZE; x++)
        {
            for (int y = 0; y < Constants.SCENE_SIZE; y++)
            {
                Tile tile = worldView.getScene().getTiles()[worldView.getPlane()][x][y];
                if (tile != null && filter.test(tile))
                {
                    out.add(tile);
                }
            }
        }

        return out;
    }

    /**
     * Returns a list of all tiles in the current scene.
     * @return A list of all tiles.
     */
    public static List<Tile> getAll()
    {
        return getAll(x -> true);
    }

    /**
     * Returns the tile at the specified world point.
     * @param worldPoint The world point to get the tile at.
     * @return The tile at the specified world point, or null if the point is out of bounds.
     */
    public static Tile getAt(WorldPoint worldPoint)
    {
        return getAt(worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
    }

    /**
     * Returns the tile at the specified local point.
     * @param localPoint The local point to get the tile at.
     * @return The tile at the specified local point, or null if the point is out of bounds.
     */
    public static Tile getAt(LocalPoint localPoint)
    {
        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        return worldView.getScene().getTiles()[worldView.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
    }

    /**
     * Returns the tile at the specified world coordinates and plane.
     * @param worldX The world X coordinate.
     * @param worldY The world Y coordinate.
     * @param plane The plane (z-level).
     * @return The tile at the specified coordinates and plane, or null if the coordinates are out of bounds.
     */
    public static Tile getAt(int worldX, int worldY, int plane)
    {
        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        int correctedX = worldX < Constants.SCENE_SIZE ? worldX + worldView.getBaseX() : worldX;
        int correctedY = worldY < Constants.SCENE_SIZE ? worldY + worldView.getBaseY() : worldY;

        if (!WorldPoint.isInScene(worldView, correctedX, correctedY))
        {
            return null;
        }

        int x = correctedX - worldView.getBaseX();
        int y = correctedY - worldView.getBaseY();

        return worldView.getScene().getTiles()[plane][x][y];
    }

    /**
     * Returns a list of tiles surrounding the specified world point within the given radius.
     * @param worldPoint The center world point.
     * @param radius The radius around the center point to include tiles.
     * @return A list of tiles surrounding the specified world point.
     */
    public static List<Tile> getSurrounding(WorldPoint worldPoint, int radius)
    {
        List<Tile> out = new ArrayList<>();
        for (int x = -radius; x <= radius; x++)
        {
            for (int y = -radius; y <= radius; y++)
            {
                out.add(getAt(worldPoint.dx(x).dx(y)));
            }
        }

        return out;
    }

    /**
     * Returns the tile currently hovered by the mouse cursor.
     * @return The hovered tile, or null if no tile is hovered.
     */
    public static Tile getHoveredTile()
    {
        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        return worldView.getSelectedSceneTile();
    }

    /**
     * Finds a path from one world point to another and returns a list of waypoints (WorldPoints) along the path.
     * @param from The starting WorldPoint.
     * @param to The destination WorldPoint.
     * @return A list of WorldPoints representing the path, or null if no path is found or if the points are on different planes.
     */
    public static List<WorldPoint> pathTo(WorldPoint from, WorldPoint to)
    {
        if (from.getPlane() != to.getPlane())
        {
            return null;
        }

        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        int x = from.getX();
        int y = from.getY();
        int plane = from.getPlane();

        LocalPoint sourceLp = LocalPoint.fromWorld(worldView, x, y);
        LocalPoint targetLp = LocalPoint.fromWorld(worldView, to.getX(), to.getY());
        if (sourceLp == null || targetLp == null)
        {
            return null;
        }

        int thisX = sourceLp.getSceneX();
        int thisY = sourceLp.getSceneY();
        int otherX = targetLp.getSceneX();
        int otherY = targetLp.getSceneY();

        Tile[][][] tiles = worldView.getScene().getTiles();
        Tile sourceTile = tiles[plane][thisX][thisY];
        Tile targetTile = tiles[plane][otherX][otherY];

        if(sourceTile == null || targetTile == null)
            return new ArrayList<>();

        List<Tile> checkpointTiles = pathTo(sourceTile, targetTile);
        if (checkpointTiles == null)
        {
            return null;
        }
        List<WorldPoint> checkpointWPs = new ArrayList<>();
        for (Tile checkpointTile : checkpointTiles)
        {
            if (checkpointTile == null)
            {
                break;
            }
            checkpointWPs.add(checkpointTile.getWorldLocation());
        }
        return checkpointWPs;
    }

    /**
     * Finds a path from one tile to another and returns a list of tiles along the path.
     * @param from The starting tile.
     * @param to The destination tile.
     * @return A list of tiles representing the path, or null if no path is found or if the tiles are on different planes.
     */
    public static List<Tile> pathTo(Tile from, Tile to)
    {
        int z = from.getPlane();
        if (z != to.getPlane())
        {
            return null;
        }

        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        CollisionData[] collisionData = worldView.getCollisionMaps();
        if (collisionData == null)
        {
            return null;
        }

        int[][] directions = new int[128][128];
        int[][] distances = new int[128][128];
        int[] bufferX = new int[4096];
        int[] bufferY = new int[4096];

        // Initialise directions and distances
        for (int i = 0; i < 128; ++i)
        {
            for (int j = 0; j < 128; ++j)
            {
                directions[i][j] = 0;
                distances[i][j] = Integer.MAX_VALUE;
            }
        }

        Point p1 = from.getSceneLocation();
        Point p2 = to.getSceneLocation();

        int middleX = p1.getX();
        int middleY = p1.getY();
        int currentX = middleX;
        int currentY = middleY;
        int offsetX = 64;
        int offsetY = 64;
        // Initialise directions and distances for starting tile
        directions[offsetX][offsetY] = 99;
        distances[offsetX][offsetY] = 0;
        int index1 = 0;
        bufferX[0] = currentX;
        int index2 = 1;
        bufferY[0] = currentY;
        int[][] collisionDataFlags = collisionData[z].getFlags();

        boolean isReachable = false;

        while (index1 != index2)
        {
            currentX = bufferX[index1];
            currentY = bufferY[index1];
            index1 = index1 + 1 & 4095;
            // currentX is for the local coordinate while currentMapX is for the index in the directions and distances arrays
            int currentMapX = currentX - middleX + offsetX;
            int currentMapY = currentY - middleY + offsetY;
            if ((currentX == p2.getX()) && (currentY == p2.getY()))
            {
                isReachable = true;
                break;
            }

            int currentDistance = distances[currentMapX][currentMapY] + 1;
            if (currentMapX > 0 && directions[currentMapX - 1][currentMapY] == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0)
            {
                // Able to move 1 tile west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY;
                index2 = index2 + 1 & 4095;
                directions[currentMapX - 1][currentMapY] = 2;
                distances[currentMapX - 1][currentMapY] = currentDistance;
            }

            if (currentMapX < 127 && directions[currentMapX + 1][currentMapY] == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0)
            {
                // Able to move 1 tile east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY;
                index2 = index2 + 1 & 4095;
                directions[currentMapX + 1][currentMapY] = 8;
                distances[currentMapX + 1][currentMapY] = currentDistance;
            }

            if (currentMapY > 0 && directions[currentMapX][currentMapY - 1] == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0)
            {
                // Able to move 1 tile south
                bufferX[index2] = currentX;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX][currentMapY - 1] = 1;
                distances[currentMapX][currentMapY - 1] = currentDistance;
            }

            if (currentMapY < 127 && directions[currentMapX][currentMapY + 1] == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0)
            {
                // Able to move 1 tile north
                bufferX[index2] = currentX;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX][currentMapY + 1] = 4;
                distances[currentMapX][currentMapY + 1] = currentDistance;
            }

            if (currentMapX > 0 && currentMapY > 0 && directions[currentMapX - 1][currentMapY - 1] == 0 && (collisionDataFlags[currentX - 1][currentY - 1] & 19136782) == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0)
            {
                // Able to move 1 tile south-west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX - 1][currentMapY - 1] = 3;
                distances[currentMapX - 1][currentMapY - 1] = currentDistance;
            }

            if (currentMapX < 127 && currentMapY > 0 && directions[currentMapX + 1][currentMapY - 1] == 0 && (collisionDataFlags[currentX + 1][currentY - 1] & 19136899) == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0)
            {
                // Able to move 1 tile north-west
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX + 1][currentMapY - 1] = 9;
                distances[currentMapX + 1][currentMapY - 1] = currentDistance;
            }

            if (currentMapX > 0 && currentMapY < 127 && directions[currentMapX - 1][currentMapY + 1] == 0 && (collisionDataFlags[currentX - 1][currentY + 1] & 19136824) == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0)
            {
                // Able to move 1 tile south-east
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX - 1][currentMapY + 1] = 6;
                distances[currentMapX - 1][currentMapY + 1] = currentDistance;
            }

            if (currentMapX < 127 && currentMapY < 127 && directions[currentMapX + 1][currentMapY + 1] == 0 && (collisionDataFlags[currentX + 1][currentY + 1] & 19136992) == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0)
            {
                // Able to move 1 tile north-east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX + 1][currentMapY + 1] = 12;
                distances[currentMapX + 1][currentMapY + 1] = currentDistance;
            }
        }
        if (!isReachable)
        {
            // Try find a different reachable tile in the 21x21 area around the target tile, as close as possible to the target tile
            int upperboundDistance = Integer.MAX_VALUE;
            int pathLength = Integer.MAX_VALUE;
            int checkRange = 10;
            int approxDestinationX = p2.getX();
            int approxDestinationY = p2.getY();
            for (int i = approxDestinationX - checkRange; i <= checkRange + approxDestinationX; ++i)
            {
                for (int j = approxDestinationY - checkRange; j <= checkRange + approxDestinationY; ++j)
                {
                    int currentMapX = i - middleX + offsetX;
                    int currentMapY = j - middleY + offsetY;
                    if (currentMapX >= 0 && currentMapY >= 0 && currentMapX < 128 && currentMapY < 128 && distances[currentMapX][currentMapY] < 100)
                    {
                        int deltaX = 0;
                        if (i < approxDestinationX)
                        {
                            deltaX = approxDestinationX - i;
                        }
                        else if (i > approxDestinationX)
                        {
                            deltaX = i - (approxDestinationX);
                        }

                        int deltaY = 0;
                        if (j < approxDestinationY)
                        {
                            deltaY = approxDestinationY - j;
                        }
                        else if (j > approxDestinationY)
                        {
                            deltaY = j - (approxDestinationY);
                        }

                        int distanceSquared = deltaX * deltaX + deltaY * deltaY;
                        if (distanceSquared < upperboundDistance || distanceSquared == upperboundDistance && distances[currentMapX][currentMapY] < pathLength)
                        {
                            upperboundDistance = distanceSquared;
                            pathLength = distances[currentMapX][currentMapY];
                            currentX = i;
                            currentY = j;
                        }
                    }
                }
            }
            if (upperboundDistance == Integer.MAX_VALUE)
            {
                // No path found
                return null;
            }
        }

        // Getting path from directions and distances
        bufferX[0] = currentX;
        bufferY[0] = currentY;
        int index = 1;
        int directionNew;
        int directionOld;
        for (directionNew = directionOld = directions[currentX - middleX + offsetX][currentY - middleY + offsetY]; p1.getX() != currentX || p1.getY() != currentY; directionNew = directions[currentX - middleX + offsetX][currentY - middleY + offsetY])
        {
            if (directionNew != directionOld)
            {
                // "Corner" of the path --> new checkpoint tile
                directionOld = directionNew;
                bufferX[index] = currentX;
                bufferY[index++] = currentY;
            }

            if ((directionNew & 2) != 0)
            {
                ++currentX;
            }
            else if ((directionNew & 8) != 0)
            {
                --currentX;
            }

            if ((directionNew & 1) != 0)
            {
                ++currentY;
            }
            else if ((directionNew & 4) != 0)
            {
                --currentY;
            }
        }

        int checkpointTileNumber = 1;
        Tile[][][] tiles = worldView.getScene().getTiles();
        List<Tile> checkpointTiles = new ArrayList<>();
        while (index-- > 0)
        {
            checkpointTiles.add(tiles[from.getPlane()][bufferX[index]][bufferY[index]]);
            if (checkpointTileNumber == 25)
            {
                break;
            }
            checkpointTileNumber++;
        }
        return checkpointTiles;
    }

    /**
     * Determines if the destination tile is reachable from the starting tile.
     * @param from The starting tile.
     * @param to The destination tile.
     * @return True if the destination tile is reachable, false otherwise.
     */
    public static boolean isReachable(Tile from, Tile to) {
        List<Tile> path  = pathTo(from, to);
        if(path == null || path.isEmpty())
            return false;
        return (path.get(path.size()-1) == to);
    }

    /**
     * Determines if the destination world point is reachable from the starting world point.
     * @param from The starting WorldPoint.
     * @param to The destination WorldPoint.
     * @return True if the destination WorldPoint is reachable, false otherwise.
     */
    public static boolean isReachable(WorldPoint from, WorldPoint to)
    {
        Client client = Static.getClient();
        WorldView worldView = client.getTopLevelWorldView();
        if(worldView.getPlane() != to.getPlane())
            return false;
        LocalPoint lp = LocalPoint.fromWorld(worldView, to.getX(), to.getY());
        LocalPoint lp_from = LocalPoint.fromWorld(worldView, from.getX(), from.getY());
        if(lp == null || lp_from == null)
            return false;
        Tile[][][] tiles = worldView.getScene().getTiles();
        try
        {
            Tile tile_from = tiles[from.getPlane()][lp_from.getSceneX()][lp_from.getSceneY()];
            Tile tile_to = tiles[to.getPlane()][lp.getSceneX()][lp.getSceneY()];
            List<Tile> path  = pathTo(tile_from, tile_to);
            return path != null && (path.get(path.size()-1) == tile_to);
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    /**
     * Determines if the destination world point is reachable from the player's current position.
     * @param to The destination WorldPoint.
     * @return True if the destination WorldPoint is reachable, false otherwise.
     */
    public static boolean isReachable(WorldPoint to)
    {
        Client client = Static.getClient();
        WorldPoint from = client.getLocalPlayer().getWorldLocation();
        return isReachable(from, to);
    }
}
