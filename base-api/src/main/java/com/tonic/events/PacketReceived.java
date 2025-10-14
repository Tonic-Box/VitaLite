package com.tonic.events;

import com.tonic.packets.PacketBuffer;
import com.tonic.packets.PacketMapReader;
import lombok.Getter;

@Getter
public class PacketReceived {
    private static final PacketReceived INSTANCE = new PacketReceived();

    private int id;
    private int length;
    private byte[] payload;
    private PacketBuffer buffer;

    /**
     * Get a reusable instance of PacketSent
     * @param id packet id
     * @param length packet length
     * @param payload packet payload
     * @return a reusable instance of PacketSent
     */
    public static PacketReceived of(int id, int length, byte[] payload)
    {
        INSTANCE.id = id;
        INSTANCE.length = length;
        INSTANCE.payload = payload;
        INSTANCE.buffer = null;
        return INSTANCE;
    }

    private PacketReceived() {
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

    public String toHex()
    {
        StringBuilder sb = new StringBuilder();
        for(byte b : payload)
        {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
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
