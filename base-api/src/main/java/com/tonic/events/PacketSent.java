package com.tonic.events;

import com.tonic.api.TPacketBufferNode;
import com.tonic.packets.PacketBuffer;
import com.tonic.packets.PacketMapReader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
public class PacketSent {
    private static final PacketSent INSTANCE = new PacketSent();

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

    public PacketBuffer getBuffer()
    {
        if(buffer == null)
        {
            buffer = new PacketBuffer(id, payload);
        }
        return buffer;
    }

    @Override
    public String toString()
    {
        PacketBuffer pb = getBuffer();
        String out = PacketMapReader.prettify(pb);
        pb.dispose();
        return out;
    }
}
