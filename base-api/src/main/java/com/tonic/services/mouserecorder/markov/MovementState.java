package com.tonic.services.mouserecorder.markov;

import lombok.Getter;

import java.util.Objects;

/**
 * Represents a discretized movement state for Markov chain analysis.
 * Quantizes continuous mouse deltas into discrete bins for probability analysis.
 */
@Getter
public class MovementState
{
    private final int deltaXBin;
    private final int deltaYBin;
    private final int deltaTimeBin;

    public MovementState(int deltaXBin, int deltaYBin, int deltaTimeBin)
    {
        this.deltaXBin = deltaXBin;
        this.deltaYBin = deltaYBin;
        this.deltaTimeBin = deltaTimeBin;
    }

    /**
     * Creates a movement state from continuous deltas using specified bin size.
     *
     * @param deltaX   Pixel delta in X
     * @param deltaY   Pixel delta in Y
     * @param deltaMs  Time delta in milliseconds
     * @param binSize  Pixels per bin (e.g., 5 = [-5,5) is one bin)
     * @param timeBinSize Time bin size in milliseconds
     * @return Quantized MovementState
     */
    public static MovementState fromDeltas(int deltaX, int deltaY, int deltaMs, int binSize, int timeBinSize)
    {
        int xBin = deltaX / binSize;
        int yBin = deltaY / binSize;
        int tBin = deltaMs / timeBinSize;

        return new MovementState(xBin, yBin, tBin);
    }

    /**
     * Converts bin back to approximate pixel delta (center of bin).
     *
     * @param binSize Pixels per bin
     * @return Approximate delta X in pixels
     */
    public int toApproxDeltaX(int binSize)
    {
        return deltaXBin * binSize;
    }

    /**
     * Converts bin back to approximate pixel delta (center of bin).
     *
     * @param binSize Pixels per bin
     * @return Approximate delta Y in pixels
     */
    public int toApproxDeltaY(int binSize)
    {
        return deltaYBin * binSize;
    }

    /**
     * Converts bin back to approximate time delta.
     *
     * @param timeBinSize Milliseconds per bin
     * @return Approximate delta time in milliseconds
     */
    public int toApproxDeltaTime(int timeBinSize)
    {
        return deltaTimeBin * timeBinSize;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovementState that = (MovementState) o;
        return deltaXBin == that.deltaXBin &&
               deltaYBin == that.deltaYBin &&
               deltaTimeBin == that.deltaTimeBin;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(deltaXBin, deltaYBin, deltaTimeBin);
    }

    @Override
    public String toString()
    {
        return String.format("State{dx=%d, dy=%d, dt=%d}", deltaXBin, deltaYBin, deltaTimeBin);
    }
}
