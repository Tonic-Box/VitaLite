package com.tonic.api;

public interface TPacketBufferNode {
    /**
     * Gets the packet buffer.
     * @return the packet buffer
     */
    TPacketBuffer getPacketBuffer();

    /**
     * Gets the client packet.
     * @return the client packet
     */
    TClientPacket getClientPacket();
}
