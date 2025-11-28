package com.tonic.services.pathfinder.sailing;

import net.runelite.api.coords.WorldPoint;

import java.util.List;

/**
 * Debug visualization for boat pathing.
 * Outputs ASCII representations of paths and waypoints to stdout.
 */
public class BoatPathingDebug
{
    /**
     * Prints an ASCII visualization of a path (List of WorldPoints).
     * Uses '.' for path tiles, 'S' for start, 'E' for end.
     */
    public static void printPath(List<WorldPoint> path)
    {
        if (path == null || path.isEmpty()) {
            System.out.println("(empty path)");
            return;
        }

        // Find bounds
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (WorldPoint wp : path) {
            minX = Math.min(minX, wp.getX());
            maxX = Math.max(maxX, wp.getX());
            minY = Math.min(minY, wp.getY());
            maxY = Math.max(maxY, wp.getY());
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;

        // Create grid
        char[][] grid = new char[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = ' ';
            }
        }

        // Plot path
        for (int i = 0; i < path.size(); i++) {
            WorldPoint wp = path.get(i);
            int gx = wp.getX() - minX;
            int gy = wp.getY() - minY;

            if (i == 0) {
                grid[gy][gx] = 'S';
            } else if (i == path.size() - 1) {
                grid[gy][gx] = 'E';
            } else {
                grid[gy][gx] = '.';
            }
        }

        // Print (Y inverted so north is up)
        System.out.println("=== Path (" + path.size() + " tiles) ===");
        for (int y = height - 1; y >= 0; y--) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < width; x++) {
                sb.append(grid[y][x]);
            }
            System.out.println(sb.toString());
        }
        System.out.println();
    }

    /**
     * Prints an ASCII visualization of waypoints with connecting lines.
     * Uses 'S' for start, 'E' for end, '*' for intermediate waypoints,
     * and line characters for connections: - | / \
     */
    public static void printWaypoints(List<BoatPathing.Waypoint> waypoints)
    {
        if (waypoints == null || waypoints.isEmpty()) {
            System.out.println("(empty waypoints)");
            return;
        }

        // Find bounds
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (BoatPathing.Waypoint wp : waypoints) {
            WorldPoint pos = wp.getPosition();
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;

        // Create grid
        char[][] grid = new char[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = ' ';
            }
        }

        // Draw lines between consecutive waypoints
        for (int i = 0; i < waypoints.size() - 1; i++) {
            WorldPoint from = waypoints.get(i).getPosition();
            WorldPoint to = waypoints.get(i + 1).getPosition();

            int x0 = from.getX() - minX;
            int y0 = from.getY() - minY;
            int x1 = to.getX() - minX;
            int y1 = to.getY() - minY;

            drawLine(grid, x0, y0, x1, y1);
        }

        // Plot waypoints (overwrite line chars)
        for (int i = 0; i < waypoints.size(); i++) {
            BoatPathing.Waypoint wp = waypoints.get(i);
            WorldPoint pos = wp.getPosition();
            int gx = pos.getX() - minX;
            int gy = pos.getY() - minY;

            if (i == 0) {
                grid[gy][gx] = 'S';
            } else if (i == waypoints.size() - 1) {
                grid[gy][gx] = 'E';
            } else {
                grid[gy][gx] = '*';
            }
        }

        // Print (Y inverted so north is up)
        System.out.println("=== Waypoints (" + waypoints.size() + ") ===");
        for (int i = 0; i < waypoints.size(); i++) {
            BoatPathing.Waypoint wp = waypoints.get(i);
            System.out.println("  " + i + ": " + wp.getPosition() + " -> " + wp.getHeading());
        }
        System.out.println();

        for (int y = height - 1; y >= 0; y--) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < width; x++) {
                sb.append(grid[y][x]);
            }
            System.out.println(sb.toString());
        }
        System.out.println();
    }

    /**
     * Draws a line between two points using Bresenham's algorithm.
     * Uses ASCII chars: - for horizontal, | for vertical, / and \ for diagonals.
     */
    private static void drawLine(char[][] grid, int x0, int y0, int x1, int y1)
    {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        while (true) {
            // Determine line character based on direction
            char lineChar = getLineChar(x1 - x0, y1 - y0);
            if (grid[y][x] == ' ') {
                grid[y][x] = lineChar;
            }

            if (x == x1 && y == y1) {
                break;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    /**
     * Gets the appropriate ASCII line character for a direction.
     */
    private static char getLineChar(int dx, int dy)
    {
        if (dy == 0) {
            return '-';  // Horizontal
        }
        if (dx == 0) {
            return '|';  // Vertical
        }
        // Diagonal
        if ((dx > 0 && dy > 0) || (dx < 0 && dy < 0)) {
            return '/';  // NE or SW
        }
        return '\\';  // NW or SE
    }
}
