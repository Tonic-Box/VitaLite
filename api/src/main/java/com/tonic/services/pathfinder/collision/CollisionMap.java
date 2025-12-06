package com.tonic.services.pathfinder.collision;

import net.runelite.api.coords.WorldPoint;

public interface CollisionMap {
    boolean walkable(int packed);
    boolean walkable(short x, short y, byte z);
    byte all(short x, short y, byte z);
    byte n(short x, short y, byte z);
    byte e(short x, short y, byte z);

    default byte s(short x, short y, byte z) {
        return n(x, (short) (y - 1), z);
    }

    default byte w(short x, short y, byte z) {
        return e((short)(x - 1), y, z);
    }

    default byte ne(short x, short y, byte z) {
        return (byte)(n(x, y, z) & e(x, y, z) & e(x, (short)(y + 1), z) & n((short)(x + 1), y, z));
    }

    default byte nw(short x, short y, byte z) {
        return (byte)(n(x, y, z) & w(x, y, z) & w(x, (short)(y + 1), z) & n((short)(x - 1), y, z));
    }

    default byte se(short x, short y, byte z) {
        return (byte)(s(x, y, z) & e(x, y, z) & e(x, (short)(y - 1), z) & s((short)(x + 1), y, z));
    }

    default byte sw(short x, short y, byte z) {
        return (byte)(s(x, y, z) & w(x, y, z) & w(x, (short)(y - 1), z) & s((short)(x - 1), y, z));
    }

    /**
     * Finds the nearest walkable point using Euclidean distance priority.
     * @param start The starting WorldPoint
     * @param maxRadius Maximum distance to search
     * @return The nearest walkable WorldPoint, or null if none found
     */
    default WorldPoint nearestWalkableEuclidean(WorldPoint start, int maxRadius) {
        int x = start.getX();
        int y = start.getY();
        byte z = (byte) start.getPlane();

        if (walkable((short) x, (short) y, z)) {
            return start;
        }

        WorldPoint best = null;
        int bestDistSq = Integer.MAX_VALUE;

        for (int r = 1; r <= maxRadius; r++) {
            // If we found something and current ring can't beat it, stop
            if (best != null && r * r > bestDistSq) {
                break;
            }

            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    // Only check perimeter of this ring
                    if (Math.abs(dx) != r && Math.abs(dy) != r) {
                        continue;
                    }

                    int distSq = dx * dx + dy * dy;
                    if (distSq < bestDistSq && walkable((short) (x + dx), (short) (y + dy), z)) {
                        bestDistSq = distSq;
                        best = new WorldPoint(x + dx, y + dy, z);
                    }
                }
            }
        }

        return best;
    }
}