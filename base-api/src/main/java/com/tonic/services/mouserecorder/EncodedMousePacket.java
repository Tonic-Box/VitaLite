package com.tonic.services.mouserecorder;

import com.tonic.packets.PacketBuffer;
import lombok.Getter;

/**
 * Represents an encoded mouse movement packet ready for transmission.
 */
@Getter
public class EncodedMousePacket
{
    /**
     * -- GETTER --
     *  The encoded packet buffer
     */
    private final PacketBuffer buffer;
    /**
     * -- GETTER --
     *  Number of samples that were encoded into this packet
     */
    private final int samplesEncoded;
    /**
     * -- GETTER --
     *  Compression statistics for this packet
     */
    private final CompressionStats stats;

    public EncodedMousePacket(PacketBuffer buffer, int samplesEncoded, CompressionStats stats)
    {
        this.buffer = buffer;
        this.samplesEncoded = samplesEncoded;
        this.stats = stats;
    }

    /**
     * Size of the encoded packet in bytes
     */
    public int getSize()
    {
        return buffer.getTrueLength();
    }

    /**
     * Statistics about compression scheme usage
     */
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
        return String.format("EncodedMousePacket{size=%d bytes, samples=%d, stats=%s}",
                getSize(), samplesEncoded, stats);
    }
}
