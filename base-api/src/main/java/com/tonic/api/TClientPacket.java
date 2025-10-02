package com.tonic.api;

public interface TClientPacket {
    /**
     * Gets the unique identifier for this packet type.
     *
     * @return the packet ID
     */
    int getId();

    /**
     * Gets the length of the packet data.
     *
     * @return the packet length, or -1 if the length is variable
     */
    int getLength();
}
