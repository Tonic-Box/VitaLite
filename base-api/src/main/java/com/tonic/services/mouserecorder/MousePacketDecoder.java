package com.tonic.services.mouserecorder;

import com.tonic.packets.PacketBuffer;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodes OSRS mouse movement packets back into their original data.
 * Used for analyzing natural mouse movement patterns from logged client packets.
 */
public class MousePacketDecoder
{
    /**
     * Tests if a packet buffer contains mouse movement data.
     * Does NOT modify the buffer's read position.
     *
     * Lightweight validation: simulates decode loop structure without actual decoding.
     * Verifies sample byte counts add up to exact dataLength and buffer has capacity.
     *
     * @param buffer The packet buffer to test
     * @return true if the buffer appears to contain valid mouse movement data, false otherwise
     */
    public static boolean test(PacketBuffer buffer)
    {
        if (buffer == null)
        {
            return false;
        }

        int originalOffset = buffer.getOffset();

        try
        {
            int bufferCapacity = buffer.getPayload().capacity();
            int remaining = bufferCapacity - originalOffset;

            // Need at least 3 bytes for header + at least 2 bytes for one sample
            if (remaining < 5)
            {
                return false;
            }

            // Read header (convert signed byte to unsigned)
            int totalLength = buffer.readByte() & 0xFF;
            int avgMillisDelta = buffer.readByte() & 0xFF;
            int remainderMillisDelta = buffer.readByte() & 0xFF;

            // Validate header values
            if (totalLength < 2 || totalLength > 248)
            {
                return false;
            }

            int dataLength = totalLength - 2;
            if (dataLength < 2 || dataLength > 246)
            {
                return false;
            }

            // Check if we have enough data for the declared length
            remaining = bufferCapacity - buffer.getOffset();
            if (remaining < dataLength)
            {
                return false;
            }

            // Simulate decode loop: verify sample structure adds up to exact dataLength
            int bytesRead = 0;
            int currentPos = buffer.getOffset();

            while (bytesRead < dataLength)
            {
                // Check we can peek at compression scheme byte
                if (currentPos >= bufferCapacity)
                {
                    return false;
                }

                // Peek at compression scheme byte (don't modify buffer offset)
                int firstByte = buffer.getPayload().getByte(currentPos) & 0xFF;

                // Determine sample size based on compression scheme
                int sampleSize;
                if (firstByte < 128)
                {
                    sampleSize = 2;  // Small delta
                }
                else if (firstByte < 192)
                {
                    sampleSize = 3;  // Medium delta
                }
                else if (firstByte < 224)
                {
                    sampleSize = 5;  // Large delta
                }
                else
                {
                    sampleSize = 6;  // XLarge delta
                }

                // Verify this sample fits within declared dataLength
                if (bytesRead + sampleSize > dataLength)
                {
                    return false;
                }

                // Verify buffer has capacity for this sample
                if (currentPos + sampleSize > bufferCapacity)
                {
                    return false;
                }

                bytesRead += sampleSize;
                currentPos += sampleSize;
            }

            // Must consume exactly dataLength bytes, no more, no less
            if (bytesRead != dataLength)
            {
                return false;
            }

            return true;
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            // Always restore original position
            buffer.setOffset(originalOffset);
        }
    }

    /**
     * Decodes a mouse movement packet buffer into structured data.
     *
     * @param buffer The packet buffer to decode
     * @return DecodedMousePacket containing all decoded samples and metadata
     */
    public static DecodedMousePacket decode(PacketBuffer buffer)
    {
        // Read 3-byte header (convert signed bytes to unsigned)
        int totalLength = buffer.readByte() & 0xFF;
        int avgMillisDelta = buffer.readByte() & 0xFF;
        int remainderMillisDelta = buffer.readByte() & 0xFF;

        List<DecodedSample> samples = new ArrayList<>();
        DecodedMousePacket.CompressionStats stats = new DecodedMousePacket.CompressionStats();

        int currentX = 0;
        int currentY = 0;
        long currentTime = 0;

        // Decode samples until we've read all data
        int dataLength = totalLength - 2; // Subtract header bytes
        int bytesRead = 0;

        while (bytesRead < dataLength)
        {
            int startOffset = buffer.getOffset();

            // Peek at first byte/short to determine compression scheme
            int firstByte = buffer.readByte() & 0xFF;
            buffer.setOffset(startOffset); // Reset to re-read

            DecodedSample sample = null;

            // Determine compression scheme based on first byte value
            if (firstByte < 128)
            {
                // Small delta (2 bytes)
                sample = decodeSmallDelta(buffer, currentX, currentY, currentTime);
                stats.smallDelta++;
                bytesRead += 2;
            }
            else if (firstByte >= 128 && firstByte < 192)
            {
                // Medium delta (3 bytes)
                sample = decodeMediumDelta(buffer, currentX, currentY, currentTime);
                stats.mediumDelta++;
                bytesRead += 3;
            }
            else if (firstByte >= 192 && firstByte < 224)
            {
                // Large delta (5 bytes)
                sample = decodeLargeDelta(buffer, currentTime);
                stats.largeDelta++;
                bytesRead += 5;
            }
            else
            {
                // XLarge delta (6 bytes)
                sample = decodeXLargeDelta(buffer, currentTime);
                stats.xlargeDelta++;
                bytesRead += 6;
            }

            if (sample != null)
            {
                samples.add(sample);
                currentX = sample.x;
                currentY = sample.y;
                currentTime = sample.timestampMillis;
            }
        }

        return new DecodedMousePacket(samples, stats, avgMillisDelta, remainderMillisDelta);
    }

    /**
     * Decodes small delta compression (2 bytes).
     * Format: [deltaTicks:3][offsetX:6][offsetY:6][unused:1]
     */
    private static DecodedSample decodeSmallDelta(PacketBuffer buffer, int lastX, int lastY, long lastTime)
    {
        int encoded = buffer.readUnsignedShort();

        int deltaTicks = (encoded >> 12) & 0x7;
        int offsetX = (encoded >> 6) & 0x3F;
        int offsetY = encoded & 0x3F;

        int deltaX = offsetX - 32;
        int deltaY = offsetY - 32;

        int x = lastX + deltaX;
        int y = lastY + deltaY;
        long time = lastTime + (deltaTicks * 20);

        return new DecodedSample(x, y, time, deltaX, deltaY, deltaTicks, "SMALL");
    }

    /**
     * Decodes medium delta compression (3 bytes).
     * Format: [deltaTicks+128:8][offsetX:8][offsetY:8]
     */
    private static DecodedSample decodeMediumDelta(PacketBuffer buffer, int lastX, int lastY, long lastTime)
    {
        int deltaTicksPlusOffset = buffer.readByte() & 0xFF;
        int deltaTicks = deltaTicksPlusOffset - 128;

        int combined = buffer.readUnsignedShort();
        int offsetX = (combined >> 8) & 0xFF;
        int offsetY = combined & 0xFF;

        int deltaX = offsetX - 128;
        int deltaY = offsetY - 128;

        int x = lastX + deltaX;
        int y = lastY + deltaY;
        long time = lastTime + (deltaTicks * 20);

        return new DecodedSample(x, y, time, deltaX, deltaY, deltaTicks, "MEDIUM");
    }

    /**
     * Decodes large delta compression (5 bytes).
     * Format: [deltaTicks+192:8][absX:16][absY:16]
     */
    private static DecodedSample decodeLargeDelta(PacketBuffer buffer, long lastTime)
    {
        int deltaTicksPlusOffset = buffer.readByte() & 0xFF;
        int deltaTicks = deltaTicksPlusOffset - 192;

        int packed = buffer.readInt();

        if (packed == Integer.MIN_VALUE)
        {
            // Special case: mouse off-screen
            return new DecodedSample(-1, -1, lastTime + (deltaTicks * 20), 0, 0, deltaTicks, "LARGE");
        }

        int x = packed & 0xFFFF;
        int y = (packed >> 16) & 0xFFFF;
        long time = lastTime + (deltaTicks * 20);

        return new DecodedSample(x, y, time, 0, 0, deltaTicks, "LARGE");
    }

    /**
     * Decodes xlarge delta compression (6 bytes).
     * Format: [deltaTicks+57344:16][absX:16][absY:16]
     */
    private static DecodedSample decodeXLargeDelta(PacketBuffer buffer, long lastTime)
    {
        int deltaTicksPlusOffset = buffer.readUnsignedShort();  // This one already exists and is correct
        int deltaTicks = (deltaTicksPlusOffset - 57344) & 8191;

        int packed = buffer.readInt();

        if (packed == Integer.MIN_VALUE)
        {
            // Special case: mouse off-screen
            return new DecodedSample(-1, -1, lastTime + (deltaTicks * 20), 0, 0, deltaTicks, "XLARGE");
        }

        int x = packed & 0xFFFF;
        int y = (packed >> 16) & 0xFFFF;
        long time = lastTime + (deltaTicks * 20);

        return new DecodedSample(x, y, time, 0, 0, deltaTicks, "XLARGE");
    }

    /**
     * Represents a single decoded mouse sample.
     */
    @Getter
    public static class DecodedSample
    {
        private final int x;
        private final int y;
        private final long timestampMillis;
        private final int deltaX;
        private final int deltaY;
        private final int deltaTicks;
        private final String compressionType;

        public DecodedSample(int x, int y, long timestampMillis, int deltaX, int deltaY, int deltaTicks, String compressionType)
        {
            this.x = x;
            this.y = y;
            this.timestampMillis = timestampMillis;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
            this.deltaTicks = deltaTicks;
            this.compressionType = compressionType;
        }

        @Override
        public String toString()
        {
            return String.format("%s[x=%d, y=%d, time=%d, dx=%d, dy=%d, dt=%d]",
                compressionType, x, y, timestampMillis, deltaX, deltaY, deltaTicks);
        }
    }
}
