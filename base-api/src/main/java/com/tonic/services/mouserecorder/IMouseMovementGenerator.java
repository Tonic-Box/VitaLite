package com.tonic.services.mouserecorder;

/**
 * Interface for mouse movement generation strategies.
 * Implementations define different approaches to generating realistic mouse movement data.
 */
public interface IMouseMovementGenerator
{
    /**
     * Generates a sequence of mouse movements from start to end position.
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param endX   Ending X coordinate
     * @param endY   Ending Y coordinate
     * @return MouseMovementSequence representing the movement
     */
    MouseMovementSequence generate(int startX, int startY, int endX, int endY);

    /**
     * Generates a sequence of mouse movements from start to end position with custom start time.
     *
     * @param startX       Starting X coordinate
     * @param startY       Starting Y coordinate
     * @param endX         Ending X coordinate
     * @param endY         Ending Y coordinate
     * @param startTimeMs  Starting timestamp in milliseconds
     * @return MouseMovementSequence representing the movement
     */
    MouseMovementSequence generate(int startX, int startY, int endX, int endY, long startTimeMs);

    /**
     * Returns the name/identifier of this generator implementation
     */
    String getName();

    /**
     * Returns a description of this generator's strategy
     */
    default String getDescription()
    {
        return "No description provided";
    }
}
