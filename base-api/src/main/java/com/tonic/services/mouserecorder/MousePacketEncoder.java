package com.tonic.services.mouserecorder;

import com.tonic.Logger;
import com.tonic.packets.PacketBuffer;
import com.tonic.packets.PacketMapReader;

import java.util.List;

/**
 * Encodes MouseMovementSequence data into the OSRS mouse movement packet format.
 * 1:1 implementation matching Jagex's MouseRecorder packet encoding
 */
public class MousePacketEncoder
{
    private static final int MAX_PACKET_SIZE = 246;

    // Persistent last encoded position (matches client's field498/field360)
    // Used to detect duplicates across flush cycles
    private static int persistentLastX = -1;
    private static int persistentLastY = -1;
    private static long persistentLastTime = -1;
    private static int mousePacket = 70;

    /**
     * Encodes a mouse movement sequence into packet format.
     * May only encode partial sequence if it exceeds max packet size.
     *
     * @param sequence The sequence to encode
     * @return EncodedMousePacket with encoded data and metadata, or null if all samples are duplicates
     */
    public static EncodedMousePacket encode(MouseMovementSequence sequence)
    {
        List<MouseDataPoint> points = sequence.getPoints();
        if (points.isEmpty())
        {
            throw new IllegalArgumentException("Cannot encode empty sequence");
        }

        if(mousePacket == -1)
        {
            mousePacket = PacketMapReader.getId("OP_MOUSE_MOVEMENT");
            if(mousePacket == -1)
            {
                Logger.error("MousePacketEncoder: Failed to get mouse movement packet ID");
                return null;
            }
        }
        PacketBuffer buffer = new PacketBuffer(mousePacket, 3 + MAX_PACKET_SIZE);

        EncodedMousePacket.CompressionStats stats = new EncodedMousePacket.CompressionStats();

        buffer.writeByte(0);
        buffer.writeByte(0);
        buffer.writeByte(0);

        int dataStartOffset = buffer.getOffset();
        int encodedCount = 0;  // Unique samples actually encoded
        int consumedCount = 0;  // Total samples consumed (including duplicates)
        int lastX = persistentLastX;  // Start from last encoded position
        int lastY = persistentLastY;
        long lastTime = persistentLastTime;
        int totalMillisDelta = 0;

        for (MouseDataPoint point : points)
        {
            // Check if we've hit packet size limit BEFORE processing
            if (buffer.getOffset() - dataStartOffset >= MAX_PACKET_SIZE)
            {
                break;
            }

            int x = point.getX();
            if (x < -1)
            {
                x = -1;
            }
            else if (x > 65534)
            {
                x = 65534;
            }

            int y = point.getY();
            if (y < -1)
            {
                y = -1;
            }
            else if (y > 65534)
            {
                y = 65534;
            }

            // Check for duplicate - consume but don't encode
            if (x == lastX && y == lastY)
            {
                consumedCount++;
                continue;
            }

            long time = point.getTimestampMillis();
            int deltaX, deltaY, deltaTicks;

            if (lastTime == -1)
            {
                deltaX = x;
                deltaY = y;
                deltaTicks = Integer.MAX_VALUE;
            }
            else
            {
                deltaX = x - lastX;
                deltaY = y - lastY;
                long deltaMs = time - lastTime;
                deltaTicks = (int) (deltaMs / 20L);
                totalMillisDelta += (int) (deltaMs % 20L);
            }

            if (!encodeSample(buffer, deltaX, deltaY, deltaTicks, x, y, stats))
            {
                break;  // Failed to encode, don't consume
            }

            // Successfully encoded
            consumedCount++;
            encodedCount++;
            lastX = x;
            lastY = y;
            lastTime = time;
        }

        // If no unique samples were encoded, don't send a packet but still consume the duplicates
        if (encodedCount == 0)
        {
            // Return null with consumedCount to clear duplicates from buffer without sending
            buffer.dispose();
            return null;
        }

        int dataLength = buffer.getOffset() - dataStartOffset;

        int totalLength = 2 + dataLength;
        buffer.getPayload().setByte(0, (byte) totalLength);

        int avg = totalMillisDelta / encodedCount;
        int remainder = totalMillisDelta % encodedCount;
        buffer.getPayload().setByte(1, (byte) avg);
        buffer.getPayload().setByte(2, (byte) remainder);

        // Update persistent position for next flush cycle (matches client's field498/field360 update)
        persistentLastX = lastX;
        persistentLastY = lastY;
        persistentLastTime = lastTime;

        // Return consumedCount so buffer knows how many samples to remove (including skipped duplicates)
        return new EncodedMousePacket(buffer, consumedCount, stats);
    }

    /**
     * Resets persistent encoder state.
     * Should be called when movement spoofing is stopped/started.
     */
    public static void reset()
    {
        persistentLastX = -1;
        persistentLastY = -1;
        persistentLastTime = -1;
    }

    /**
     * Encodes a single sample using the appropriate compression scheme.
     * Matches Jagex's 4 encoding schemes exactly (Client.java:3058-3081).
     */
    private static boolean encodeSample(PacketBuffer buffer, int deltaX, int deltaY, int deltaTicks,
                                        int absX, int absY, EncodedMousePacket.CompressionStats stats)
    {
        if (deltaTicks < 8 && deltaX >= -32 && deltaX <= 31 && deltaY >= -32 && deltaY <= 31)
        {
            int offsetX = deltaX + 32;
            int offsetY = deltaY + 32;
            int encoded = (deltaTicks << 12) + offsetY + (offsetX << 6);
            buffer.writeShort(encoded);
            stats.smallDelta++;
            return true;
        }

        if (deltaTicks < 32 && deltaX >= -128 && deltaX <= 127 && deltaY >= -128 && deltaY <= 127)
        {
            int offsetX = deltaX + 128;
            int offsetY = deltaY + 128;
            buffer.writeByte(deltaTicks + 128);
            buffer.writeShort(offsetY + (offsetX << 8));
            stats.mediumDelta++;
            return true;
        }

        if (deltaTicks < 32)
        {
            buffer.writeByte(deltaTicks + 192);
            if (absX != -1 && absY != -1)
            {
                buffer.writeInt(absX | (absY << 16));
            }
            else
            {
                buffer.writeInt(Integer.MIN_VALUE);
            }
            stats.largeDelta++;
            return true;
        }

        int clampedTicks = deltaTicks & 8191;
        buffer.writeShort(clampedTicks + 57344);
        if (absX != -1 && absY != -1)
        {
            buffer.writeInt(absX | (absY << 16));
        }
        else
        {
            buffer.writeInt(Integer.MIN_VALUE);
        }
        stats.xlargeDelta++;
        return true;
    }
}
