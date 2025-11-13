package com.tonic.services.mouserecorder;

import com.tonic.Static;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main API for building and managing synthetic mouse movement data.
 *
 * This API provides a flexible, modular approach to generating mouse movement
 * telemetry that can be sent to the game server. It separates concerns between:
 * - Movement generation (how movements are created)
 * - Packet encoding (how movements are encoded into packet format)
 * - Data submission (how packets are queued/transmitted)
 * Usage Example:
 * // Create API with a generator
 * MouseRecorderAPI api = new MouseRecorderAPI(new LinearMouseGenerator());
 * // Generate movement from (100, 200) to (500, 400)
 * api.recordMovement(100, 200, 500, 400);
 * // Build packet when ready
 * EncodedMousePacket packet = api.buildPacket();
 * // Submit packet data (implement transmission logic)
 * submitToServer(packet.getData());
 *
 */
public class MouseRecorderAPI
{
    @Getter
    private IMouseMovementGenerator generator;
    private final List<MouseDataPoint> pendingSamples;
    private boolean autoClamp;

    /**
     * Creates a new MouseRecorderAPI with the specified generator.
     *
     * @param generator The movement generator to use
     */
    public MouseRecorderAPI(IMouseMovementGenerator generator)
    {
        this.generator = generator;
        this.pendingSamples = new CopyOnWriteArrayList<>();
        this.autoClamp = true;
    }

    /**
     * Sets the movement generator to use.
     *
     * @param generator The new generator
     */
    public void setGenerator(IMouseMovementGenerator generator)
    {
        this.generator = generator;
    }

    /**
     * Sets whether coordinates should be automatically clamped to valid range.
     * Default: true
     *
     * @param autoClamp True to enable auto-clamping
     */
    public void setAutoClamp(boolean autoClamp)
    {
        this.autoClamp = autoClamp;
    }

    /**
     * Records a mouse movement from current position to target position.
     * Uses the configured generator to create the movement sequence.
     *
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param endX   Ending X coordinate
     * @param endY   Ending Y coordinate
     */
    public void recordMovement(int startX, int startY, int endX, int endY)
    {
        MouseMovementSequence sequence = generator.generate(startX, startY, endX, endY);
        addSequence(sequence);
    }

    /**
     * Records a mouse movement with custom start time.
     *
     * @param startX      Starting X coordinate
     * @param startY      Starting Y coordinate
     * @param endX        Ending X coordinate
     * @param endY        Ending Y coordinate
     * @param startTimeMs Starting timestamp in milliseconds
     */
    public void recordMovement(int startX, int startY, int endX, int endY, long startTimeMs)
    {
        MouseMovementSequence sequence = generator.generate(startX, startY, endX, endY, startTimeMs);
        addSequence(sequence);
    }

    /**
     * Adds a pre-generated movement sequence to the pending samples.
     *
     * @param sequence The sequence to add
     */
    public void addSequence(MouseMovementSequence sequence)
    {
        if (autoClamp)
        {
            sequence = sequence.clamped();
        }
        pendingSamples.addAll(sequence.getPoints());

        if (Static.getVitaConfig().shouldVisualizeMovements())
        {
            MovementVisualization.MovementSource source = MovementVisualization.MovementSource.TRAJECTORY_GENERATED;
            if (!sequence.getPoints().isEmpty() && sequence.getPoints().size() >= 2)
            {
                MouseDataPoint start = sequence.getPoints().get(0);
                MouseDataPoint end = sequence.getPoints().get(sequence.getPoints().size() - 1);
                double distance = Math.sqrt(Math.pow(end.getX() - start.getX(), 2) + Math.pow(end.getY() - start.getY(), 2));
                if (distance < 20)
                {
                    source = MovementVisualization.MovementSource.IDLE_JITTER;
                }
            }
            MovementVisualization.recordMovement(sequence.getPoints(), source);
        }
    }

    /**
     * Manually adds a single mouse data point.
     *
     * @param point The point to add
     */
    public void addSample(MouseDataPoint point)
    {
        if (autoClamp)
        {
            point = point.clamped();
        }
        pendingSamples.add(point);
    }

    /**
     * Manually adds a single mouse sample with coordinates and timestamp.
     *
     * @param x           X coordinate
     * @param y           Y coordinate
     * @param timestampMs Timestamp in milliseconds
     */
    public void addSample(int x, int y, long timestampMs)
    {
        addSample(new MouseDataPoint(x, y, timestampMs));
    }

    /**
     * Returns the number of pending samples waiting to be encoded.
     */
    public int getPendingSampleCount()
    {
        return pendingSamples.size();
    }

    /**
     * Clears all pending samples.
     */
    public void clearPendingSamples()
    {
        pendingSamples.clear();
    }

    /**
     * Builds an encoded packet from pending samples.
     * This may only encode a portion of pending samples if they exceed max packet size.
     * Encoded samples are removed from the pending queue.
     *
     * @return EncodedMousePacket, or null if no samples are pending
     */
    public EncodedMousePacket buildPacket()
    {
        if (pendingSamples.isEmpty())
        {
            return null;
        }

        // Create sequence from pending samples
        MouseMovementSequence sequence = new MouseMovementSequence(new ArrayList<>(pendingSamples));

        // Encode the sequence
        EncodedMousePacket packet = MousePacketEncoder.encode(sequence);

        // Remove encoded samples from pending queue
        int encoded = packet.getSamplesEncoded();
        for (int i = 0; i < encoded && !pendingSamples.isEmpty(); i++)
        {
            pendingSamples.remove(0);
        }

        return packet;
    }

    /**
     * Checks if there are enough samples to build a packet.
     * Recommended minimum is 40 samples (matching Jagex's threshold).
     *
     * @param minSamples Minimum sample count
     * @return True if pending samples >= minSamples
     */
    public boolean hasEnoughSamples(int minSamples)
    {
        return pendingSamples.size() >= minSamples;
    }

    /**
     * Checks if there are at least 40 samples (Jagex's transmission threshold).
     */
    public boolean shouldTransmit()
    {
        return hasEnoughSamples(40);
    }
}
