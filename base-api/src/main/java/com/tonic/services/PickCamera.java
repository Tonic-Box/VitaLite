package com.tonic.services;

/**
 * A camera the game picks from instead of its own, as a VR pointer needs: while set, each frame's scene
 * is drawn by the game from a camera standing on the ray and looking down it, so with the mouse at the
 * viewport's middle the game's own hover, menus, tile picking and actions all follow the ray. The
 * game's camera is given back once the scene is drawn.
 */
public final class PickCamera
{
    private static volatile float[] ray;

    private PickCamera()
    {
    }

    /**
     * Has the game pick down a ray of the world from the next frame on.
     *
     * @param originX    where the ray starts, across
     * @param originY    where the ray starts, down
     * @param originZ    where the ray starts, along
     * @param directionX which way it goes, across
     * @param directionY which way it goes, down
     * @param directionZ which way it goes, along
     */
    public static void set(float originX, float originY, float originZ, float directionX, float directionY, float directionZ)
    {
        ray = new float[]{originX, originY, originZ, directionX, directionY, directionZ};
    }

    /**
     * Gives picking back to the game's own camera.
     */
    public static void clear()
    {
        ray = null;
    }

    /**
     * @return the ray picked down, origin then direction, or null for the game's own camera
     */
    public static float[] ray()
    {
        return ray;
    }
}
