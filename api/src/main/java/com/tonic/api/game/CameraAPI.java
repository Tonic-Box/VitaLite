package com.tonic.api.game;

import com.tonic.Static;
import com.tonic.data.wrappers.PlayerEx;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

/**
 * Camera API for programmatic camera control
 */
public class CameraAPI
{
    // Camera angle constants
    public static final int YAW_MIN = 0;
    public static final int YAW_MAX = 2047;
    public static final int PITCH_MIN = 128;
    public static final int PITCH_MAX = 383;

    // Camera rotation speeds (per game tick)
    private static final int YAW_SPEED_NORMAL = 24;
    private static final int PITCH_SPEED_NORMAL = 12;

    // Conversion constants
    private static final double UNITS_PER_DEGREE = 2048.0 / 360.0;
    private static final double DEGREES_PER_UNIT = 360.0 / 2048.0;

    /**
     * Gets the current camera yaw (horizontal rotation)
     * @return Camera yaw in units (0-2047)
     */
    public static int getYaw()
    {
        Client client = Static.getClient();
        return Static.invoke(client::getCameraYaw);
    }

    /**
     * Gets the current camera pitch (vertical tilt)
     * @return Camera pitch in units (128-383)
     */
    public static int getPitch()
    {
        Client client = Static.getClient();
        return Static.invoke(client::getCameraPitch);
    }

    /**
     * Gets the target camera yaw based on player input
     * @return Target camera yaw in units (0-2047)
     */
    public static int getYawTarget()
    {
        Client client = Static.getClient();
        return Static.invoke(client::getCameraYawTarget);
    }

    /**
     * Gets the target camera pitch based on player input
     * @return Target camera pitch in units (128-383)
     */
    public static int getPitchTarget()
    {
        Client client = Static.getClient();
        return Static.invoke(client::getCameraPitchTarget);
    }

    /**
     * Sets the target camera yaw (horizontal rotation)
     * The camera will smoothly interpolate to this target
     * @param yaw Target yaw in units (0-2047), will be wrapped to valid range
     */
    public static void setYawTarget(int yaw)
    {
        Client client = Static.getClient();
        Static.invoke(() -> client.setCameraYawTarget(yaw & 2047));
    }

    /**
     * Sets the target camera pitch (vertical tilt)
     * The camera will smoothly interpolate to this target
     * @param pitch Target pitch in units (128-383), will be clamped to valid range
     */
    public static void setPitchTarget(int pitch)
    {
        int clampedPitch = Math.max(PITCH_MIN, Math.min(PITCH_MAX, pitch));
        Client client = Static.getClient();
        Static.invoke(() -> client.setCameraPitchTarget(clampedPitch));
    }

    /**
     * Sets the camera interpolation speed
     * @param speed Speed multiplier (1.0 = normal, higher = faster interpolation)
     */
    public static void setSpeed(float speed)
    {
        Client client = Static.getClient();
        Static.invoke(() -> client.setCameraSpeed(speed));
    }

    /**
     * Converts degrees to camera units
     * @param degrees Angle in degrees
     * @return Angle in camera units
     */
    public static int degreesToUnits(double degrees)
    {
        return (int) Math.round(degrees * UNITS_PER_DEGREE);
    }

    /**
     * Converts camera units to degrees
     * @param units Angle in camera units
     * @return Angle in degrees
     */
    public static double unitsToDegrees(int units)
    {
        return units * DEGREES_PER_UNIT;
    }

    /**
     * Gets the current camera yaw in degrees
     * @return Camera yaw in degrees (0-360)
     */
    public static double getYawDegrees()
    {
        return unitsToDegrees(getYaw());
    }

    /**
     * Gets the current camera pitch in degrees
     * @return Camera pitch in degrees
     */
    public static double getPitchDegrees()
    {
        return unitsToDegrees(getPitch());
    }

    /**
     * Sets the camera yaw target in degrees
     * @param degrees Target yaw in degrees
     */
    public static void setYawTargetDegrees(double degrees)
    {
        setYawTarget(degreesToUnits(degrees));
    }

    /**
     * Sets the camera pitch target in degrees
     * @param degrees Target pitch in degrees
     */
    public static void setPitchTargetDegrees(double degrees)
    {
        setPitchTarget(degreesToUnits(degrees));
    }

    /**
     * Rotates the camera yaw (horizontal) by a relative amount
     * @param deltaUnits Amount to rotate in camera units (positive = clockwise)
     */
    public static void rotateYaw(int deltaUnits)
    {
        int newYaw = (getYawTarget() + deltaUnits) & 2047;
        setYawTarget(newYaw);
    }

    /**
     * Rotates the camera pitch (vertical) by a relative amount
     * @param deltaUnits Amount to rotate in camera units (positive = down)
     */
    public static void rotatePitch(int deltaUnits)
    {
        int newPitch = getPitchTarget() + deltaUnits;
        setPitchTarget(newPitch);
    }

    /**
     * Rotates the camera yaw by degrees
     * @param deltaDegrees Amount to rotate in degrees (positive = clockwise)
     */
    public static void rotateYawDegrees(double deltaDegrees)
    {
        rotateYaw(degreesToUnits(deltaDegrees));
    }

    /**
     * Rotates the camera pitch by degrees
     * @param deltaDegrees Amount to rotate in degrees (positive = down)
     */
    public static void rotatePitchDegrees(double deltaDegrees)
    {
        rotatePitch(degreesToUnits(deltaDegrees));
    }

    /**
     * Turns the camera left by the default speed
     */
    public static void turnLeft()
    {
        rotateYaw(-YAW_SPEED_NORMAL);
    }

    /**
     * Turns the camera right by the default speed
     */
    public static void turnRight()
    {
        rotateYaw(YAW_SPEED_NORMAL);
    }

    /**
     * Turns the camera left by a specific amount
     * @param units Amount to turn in camera units
     */
    public static void turnLeft(int units)
    {
        rotateYaw(-units);
    }

    /**
     * Turns the camera right by a specific amount
     * @param units Amount to turn in camera units
     */
    public static void turnRight(int units)
    {
        rotateYaw(units);
    }

    /**
     * Tilts the camera up by the default speed
     */
    public static void tiltUp()
    {
        rotatePitch(-PITCH_SPEED_NORMAL);
    }

    /**
     * Tilts the camera down by the default speed
     */
    public static void tiltDown()
    {
        rotatePitch(PITCH_SPEED_NORMAL);
    }

    /**
     * Tilts the camera up by a specific amount
     * @param units Amount to tilt in camera units
     */
    public static void tiltUp(int units)
    {
        rotatePitch(-units);
    }

    /**
     * Tilts the camera down by a specific amount
     * @param units Amount to tilt in camera units
     */
    public static void tiltDown(int units)
    {
        rotatePitch(units);
    }

    /**
     * Points the camera toward a specific world point
     * @param target The world point to look at
     */
    public static void lookAt(WorldPoint target)
    {
        WorldPoint player = PlayerEx.getLocal().getWorldPoint();
        if (player == null || target == null)
        {
            return;
        }

        int dx = target.getX() - player.getX();
        int dy = target.getY() - player.getY();

        // Calculate angle in radians, then convert to camera units
        double angleRadians = Math.atan2(dy, dx);
        double angleDegrees = Math.toDegrees(angleRadians);

        // Convert to OSRS camera coordinates (0 = south, increases clockwise)
        // OSRS: 0=south, 512=west, 1024=north, 1536=east
        // Math.atan2: 0=east, 90=north, 180/-180=west, -90=south
        double osrsAngle = 90 - angleDegrees; // Adjust to OSRS coordinate system

        int targetYaw = degreesToUnits(osrsAngle) & 2047;
        setYawTarget(targetYaw);
    }

    /**
     * Points the camera toward a specific world point with given pitch
     * @param target The world point to look at
     * @param pitch The pitch angle in units (128-383)
     */
    public static void lookAt(WorldPoint target, int pitch)
    {
        lookAt(target);
        setPitchTarget(pitch);
    }

    /**
     * Resets the camera to default position (north-facing, medium pitch)
     */
    public static void reset()
    {
        setYawTarget(1024); // North
        setPitchTarget(256); // Medium pitch
    }

    /**
     * Sets the camera to face north
     */
    public static void faceNorth()
    {
        setYawTarget(1024);
    }

    /**
     * Sets the camera to face south
     */
    public static void faceSouth()
    {
        setYawTarget(0);
    }

    /**
     * Sets the camera to face east
     */
    public static void faceEast()
    {
        setYawTarget(1536);
    }

    /**
     * Sets the camera to face west
     */
    public static void faceWest()
    {
        setYawTarget(512);
    }

    /**
     * Gets the compass direction the camera is facing
     * @return Cardinal direction as a string (N, NE, E, SE, S, SW, W, NW)
     */
    public static String getCompassDirection()
    {
        int yaw = getYaw();
        double degrees = unitsToDegrees(yaw);

        // OSRS: 0=south, 90=west, 180=north, 270=east
        // Adjust to standard compass: 0=north, 90=east, 180=south, 270=west
        double compassDegrees = (450 - degrees) % 360;

        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round(compassDegrees / 45.0) % 8;
        return directions[index];
    }

    /**
     * Checks if the camera is currently moving/interpolating
     * @return true if camera is moving toward target, false if at target
     */
    public static boolean isMoving()
    {
        int yawDiff = Math.abs(getYaw() - getYawTarget());
        int pitchDiff = Math.abs(getPitch() - getPitchTarget());

        // Account for yaw wrapping (e.g., 0 and 2047 are close)
        if (yawDiff > 1024)
        {
            yawDiff = 2048 - yawDiff;
        }

        return yawDiff > 2 || pitchDiff > 2;
    }

    /**
     * Gets the angle difference between current yaw and target
     * @return Angle difference in units (0-1024)
     */
    public static int getYawDifference()
    {
        int diff = Math.abs(getYaw() - getYawTarget());
        return Math.min(diff, 2048 - diff);
    }

    /**
     * Gets the angle difference between current pitch and target
     * @return Angle difference in units
     */
    public static int getPitchDifference()
    {
        return Math.abs(getPitch() - getPitchTarget());
    }
}
