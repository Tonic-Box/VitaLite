package com.tonic.api.game.sailing;

import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * Boat Heading Enum
 */
@RequiredArgsConstructor
@Getter
public enum Heading
{
    SOUTH(0),
    SOUTH_SOUTH_WEST(1),
    SOUTH_WEST(2),
    WEST_SOUTH_WEST(3),
    WEST(4),
    WEST_NORTH_WEST(5),
    NORTH_WEST(6),
    NORTH_NORTH_WEST(7),
    NORTH(8),
    NORTH_NORTH_EAST(9),
    NORTH_EAST(10),
    EAST_NORTH_EAST(11),
    EAST(12),
    EAST_SOUTH_EAST(13),
    SOUTH_EAST(14),
    SOUTH_SOUTH_EAST(15)
    ;

    /**
     * Get the optimal heading from the current player location to the target location
     *
     * @param target The target WorldPoint
     * @return The optimal Heading
     */
    public static Heading getOptimalHeading(WorldPoint target) {
        WorldPoint current = WorldPointUtil.getTopWorldViewLocation();

        int deltaX = target.getX() - current.getX();
        int deltaY = target.getY() - current.getY();

        // Calculate angle from current to target using atan2
        // atan2 returns angle in radians where:
        // 0 = East, π/2 = North, π = West, -π/2 = South
        double angleRadians = Math.atan2(deltaY, deltaX);

        // Convert to degrees for easier calculation
        double angleDegrees = Math.toDegrees(angleRadians);

        // atan2 gives us: 0° = East, 90° = North, ±180° = West, -90° = South
        // But our heading system has: 0 = South, 4 = West, 8 = North, 12 = East
        // Each heading unit represents 22.5 degrees (360/16)

        // Transform the angle to match our heading system
        // We need to rotate and flip to get the correct mapping
        double headingDegrees = (270 - angleDegrees);

        // Normalize to 0-360 range
        while (headingDegrees < 0) {
            headingDegrees += 360;
        }
        while (headingDegrees >= 360) {
            headingDegrees -= 360;
        }

        // Convert degrees to heading value (0-15)
        // Add 11.25 to round to nearest heading (half of 22.5)
        int headingValue = (int) ((headingDegrees + 11.25) / 22.5) % 16;

        // Find and return the corresponding Heading enum
        for (Heading heading : Heading.values()) {
            if (heading.getValue() == headingValue) {
                return heading;
            }
        }

        // Fallback (shouldn't happen)
        return Heading.SOUTH;
    }

    public static Heading fromValue(int value)
    {
        for(Heading heading : values())
        {
            if(heading.getValue() == value)
            {
                return heading;
            }
        }
        return null;
    }

    private final int value;
}