package com.tonic.events;

import com.tonic.packets.PacketBuffer;
import com.tonic.packets.PacketMapReader;
import lombok.Getter;

/**
 * Event fired when a packet is sent to the server.
 */
@Getter
public class PacketSent {
    private static final PacketSent INSTANCE = new PacketSent();

    /**
     * Get a reusable instance of PacketSent
     * @param id packet id
     * @param length packet length
     * @param payload packet payload
     * @return a reusable instance of PacketSent
     */
    public static PacketSent of(int id, int length, byte[] payload)
    {
        INSTANCE.id = id;
        INSTANCE.length = length;
        INSTANCE.payload = payload;
        INSTANCE.buffer = null;
        return INSTANCE;
    }

    private int id;
    private int length;
    private byte[] payload;
    private PacketBuffer buffer;

    private PacketSent() {
        this.id = 0;
        this.length = 0;
        this.payload = new byte[0];
    }

    /**
     * Get a PacketBuffer for the packet payload.
     * This is lazily initialized and cached.
     * @return PacketBuffer for the packet payload.
     */
    public PacketBuffer getBuffer()
    {
        if(buffer == null)
        {
            buffer = new PacketBuffer(id, payload);
        }
        return buffer;
    }

    /**
     * Prettify the packet payload using PacketMapReader.
     * Disposes of the PacketBuffer after use.
     * @return prettified string representation of the packet payload.
     */
    @Override
    public String toString()
    {
        PacketBuffer pb = getBuffer();
        String out = PacketMapReader.prettify(pb);
        pb.dispose();
        return out;
    }
}
