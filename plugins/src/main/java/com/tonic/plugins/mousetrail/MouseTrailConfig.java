package com.tonic.plugins.mousetrail;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

import java.awt.*;

/**
 * Configuration for Mouse Trail visualization plugin
 */
@ConfigGroup("mousetrail")
public interface MouseTrailConfig extends Config {

    @ConfigItem(
        keyName = "trailLength",
        name = "Trail Length",
        description = "Number of points in the trail (higher = longer trail)",
        position = 1
    )
    @Range(min = 10, max = 200)
    default int trailLength() {
        return 50;
    }

    @ConfigItem(
        keyName = "fadeTimeMs",
        name = "Fade Time (ms)",
        description = "How long trail points remain visible in milliseconds",
        position = 2
    )
    @Range(min = 100, max = 5000)
    default int fadeTimeMs() {
        return 1000;
    }

    @ConfigItem(
        keyName = "trailWidth",
        name = "Trail Width",
        description = "Width of the trail line in pixels",
        position = 3
    )
    @Range(min = 1, max = 10)
    default int trailWidth() {
        return 3;
    }

    @ConfigItem(
        keyName = "trailColor",
        name = "Trail Color",
        description = "Base color of the trail (tail color)",
        position = 4
    )
    default Color trailColor() {
        return new Color(0, 150, 255); // Cyan blue
    }

    @ConfigItem(
        keyName = "headColor",
        name = "Head Color",
        description = "Color at the head of the trail (most recent)",
        position = 5
    )
    default Color headColor() {
        return Color.WHITE;
    }

    @ConfigItem(
        keyName = "useGradient",
        name = "Use Gradient",
        description = "Fade from trail color to head color (disable for solid color)",
        position = 6
    )
    default boolean useGradient() {
        return true;
    }

    @ConfigItem(
        keyName = "smoothTrail",
        name = "Smooth Trail",
        description = "Use anti-aliasing for smoother appearance",
        position = 7
    )
    default boolean smoothTrail() {
        return true;
    }

    @ConfigItem(
        keyName = "fadeAlpha",
        name = "Fade Alpha",
        description = "Fade out alpha transparency at tail (0 = invisible, 255 = opaque)",
        position = 8
    )
    @Range(min = 0, max = 255)
    default int fadeAlpha() {
        return 50;
    }
}
