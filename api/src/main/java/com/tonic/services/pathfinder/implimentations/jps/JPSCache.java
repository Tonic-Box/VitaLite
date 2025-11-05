package com.tonic.services.pathfinder.implimentations.jps;

import com.tonic.services.pathfinder.transports.Transport;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Optimized cache for JPS+ using Trove collections.
 */
public class JPSCache
{
    private final TIntIntHashMap parents;
    private final TIntIntHashMap gScores;
    private final TIntObjectHashMap<Transport> transports;

    public JPSCache(int expectedSize) {
        this.parents = new TIntIntHashMap(expectedSize, 0.5f, -1, -1);
        this.gScores = new TIntIntHashMap(expectedSize, 0.5f, -1, Integer.MAX_VALUE);
        this.transports = new TIntObjectHashMap<>(expectedSize / 10);
    }

    /**
     * Attempts to add a new position with its g-score and parent.
     * @return true if added (better path), false if already has better path
     */
    public boolean putIfBetter(int position, int gScore, int parent) {
        int existingGScore = gScores.get(position);
        if (gScore < existingGScore) {
            gScores.put(position, gScore);
            parents.put(position, parent);
            return true;
        }
        return false;
    }

    /**
     * Adds position with g-score, parent, and transport.
     */
    public boolean putIfBetter(int position, int gScore, int parent, Transport transport) {
        int existingGScore = gScores.get(position);
        if (gScore < existingGScore) {
            gScores.put(position, gScore);
            parents.put(position, parent);
            if (transport != null) {
                transports.put(position, transport);
            }
            return true;
        }
        return false;
    }

    /**
     * Gets current g-score for position.
     */
    public int getGScore(int position) {
        return gScores.get(position);
    }

    /**
     * Checks if position has been visited.
     */
    public boolean contains(int position) {
        return gScores.contains(position);
    }

    public int size() {
        return parents.size();
    }

    /**
     * Reconstructs path from target back to start, filling in ALL intermediate tiles.
     */
    public List<JPSStep> reconstructPath(int target, int playerStart) {
        LinkedList<JPSStep> jumpPoints = new LinkedList<>();
        int current = target;

        // First, collect all jump points
        while (current != -1) {
            Transport transport = transports.get(current);
            jumpPoints.addFirst(new JPSStep(current, transport));
            current = parents.get(current);
        }

        // Remove player's starting position (keep teleport destinations)
        if (!jumpPoints.isEmpty() && jumpPoints.getFirst().getPackedPosition() == playerStart) {
            jumpPoints.removeFirst();
        }

        // Now fill in all intermediate tiles between jump points
        List<JPSStep> fullPath = new ArrayList<>();

        for (int i = 0; i < jumpPoints.size(); i++) {
            JPSStep step = jumpPoints.get(i);

            // Add intermediate tiles if not first step
            if (i > 0) {
                JPSStep prevStep = jumpPoints.get(i - 1);

                // Only interpolate if current step has no transport
                // (Transport is stored on destination, so if step has transport, we teleported here)
                if (step.getTransport() == null) {
                    List<JPSStep> intermediate = interpolatePath(
                        prevStep.getPackedPosition(),
                        step.getPackedPosition()
                    );
                    fullPath.addAll(intermediate);
                }
            }

            // Add the jump point itself
            fullPath.add(step);
        }

        return fullPath;
    }

    /**
     * Fills in all tiles between two positions using linear interpolation.
     * Validates each step to ensure path doesn't go through walls.
     */
    private List<JPSStep> interpolatePath(int from, int to) {
        List<JPSStep> path = new ArrayList<>();

        short x1 = com.tonic.util.WorldPointUtil.getCompressedX(from);
        short y1 = com.tonic.util.WorldPointUtil.getCompressedY(from);
        byte plane1 = com.tonic.util.WorldPointUtil.getCompressedPlane(from);

        short x2 = com.tonic.util.WorldPointUtil.getCompressedX(to);
        short y2 = com.tonic.util.WorldPointUtil.getCompressedY(to);
        byte plane2 = com.tonic.util.WorldPointUtil.getCompressedPlane(to);

        // Can't interpolate between different planes
        if (plane1 != plane2) {
            return path;
        }

        int dx = Integer.compare(x2 - x1, 0);
        int dy = Integer.compare(y2 - y1, 0);

        int currentX = x1;
        int currentY = y1;

        com.tonic.services.pathfinder.collision.CollisionMap collisionMap =
            com.tonic.services.pathfinder.Walker.getCollisionMap();

        if (collisionMap == null) {
            return path;
        }

        // Walk step by step from 'from' to 'to'
        while (currentX != x2 || currentY != y2) {
            // Use collision map's directional methods to check validity
            boolean canMove;
            if (dx == 1 && dy == 1) {
                canMove = collisionMap.ne((short)currentX, (short)currentY, plane1) != 0;
            } else if (dx == -1 && dy == 1) {
                canMove = collisionMap.nw((short)currentX, (short)currentY, plane1) != 0;
            } else if (dx == 1 && dy == -1) {
                canMove = collisionMap.se((short)currentX, (short)currentY, plane1) != 0;
            } else if (dx == -1 && dy == -1) {
                canMove = collisionMap.sw((short)currentX, (short)currentY, plane1) != 0;
            } else if (dx == 1 && dy == 0) {
                canMove = collisionMap.e((short)currentX, (short)currentY, plane1) != 0;
            } else if (dx == -1 && dy == 0) {
                canMove = collisionMap.w((short)currentX, (short)currentY, plane1) != 0;
            } else if (dx == 0 && dy == 1) {
                canMove = collisionMap.n((short)currentX, (short)currentY, plane1) != 0;
            } else if (dx == 0 && dy == -1) {
                canMove = collisionMap.s((short)currentX, (short)currentY, plane1) != 0;
            } else {
                canMove = false;
            }

            if (!canMove) {
                // Hit a wall - path is invalid
                return new ArrayList<>();
            }

            if (currentX != x2) {
                currentX += dx;
            }
            if (currentY != y2) {
                currentY += dy;
            }

            // Don't add the final position (it will be added as the jump point)
            if (currentX == x2 && currentY == y2) {
                break;
            }

            int pos = com.tonic.util.WorldPointUtil.compress((short)currentX, (short)currentY, plane1);
            path.add(new JPSStep(pos, null));
        }

        return path;
    }
}
