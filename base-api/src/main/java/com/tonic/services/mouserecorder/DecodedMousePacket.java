package com.tonic.services.mouserecorder;

import lombok.Getter;

import java.util.List;

/**
 * Represents a decoded mouse movement packet with all samples and metadata.
 * Result of decoding a natural client mouse movement packet.
 */
@Getter
public class DecodedMousePacket
{
    private final List<MousePacketDecoder.DecodedSample> samples;
    private final CompressionStats stats;
    private final int avgMillisDelta;
    private final int remainderMillisDelta;

    public DecodedMousePacket(List<MousePacketDecoder.DecodedSample> samples,
                              CompressionStats stats,
                              int avgMillisDelta,
                              int remainderMillisDelta)
    {
        this.samples = samples;
        this.stats = stats;
        this.avgMillisDelta = avgMillisDelta;
        this.remainderMillisDelta = remainderMillisDelta;
    }

    /**
     * Gets the number of samples in this packet.
     */
    public int getSampleCount()
    {
        return samples.size();
    }

    /**
     * Statistics about compression scheme usage in the decoded packet.
     */
    @Getter
    public static class CompressionStats
    {
        public int smallDelta;      // 2-byte encoding count
        public int mediumDelta;     // 3-byte encoding count
        public int largeDelta;      // 5-byte encoding count
        public int xlargeDelta;     // 6-byte encoding count

        public int getTotalSamples()
        {
            return smallDelta + mediumDelta + largeDelta + xlargeDelta;
        }

        public double getAverageBytesPerSample()
        {
            int total = getTotalSamples();
            if (total == 0) return 0;

            int totalBytes = (smallDelta * 2) + (mediumDelta * 3) +
                           (largeDelta * 5) + (xlargeDelta * 6);
            return (double) totalBytes / total;
        }

        @Override
        public String toString()
        {
            return String.format("CompressionStats{small=%d, medium=%d, large=%d, xlarge=%d, avg=%.2f bytes/sample}",
                    smallDelta, mediumDelta, largeDelta, xlargeDelta, getAverageBytesPerSample());
        }
    }

    @Override
    public String toString()
    {
        return String.format("DecodedMousePacket{samples=%d, avgDelta=%dms, remainder=%dms, stats=%s}",
                getSampleCount(), avgMillisDelta, remainderMillisDelta, stats);
    }

    /**
     * Converts decoded samples back to MouseDataPoint list for analysis.
     */
    public List<MouseDataPoint> toMouseDataPoints()
    {
        return samples.stream()
            .map(s -> new MouseDataPoint(s.getX(), s.getY(), s.getTimestampMillis()))
            .collect(java.util.stream.Collectors.toList());
    }
}
