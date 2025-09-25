package com.tonic.services.ClickPacket;

import lombok.Getter;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Getter
public class ClickPacket {

    private final ZonedDateTime date;
    private final PacketInteractionType packetInteractionType;
    private final int x;
    private final int y;

    public ClickPacket(PacketInteractionType packetInteractionType, int x, int y) {
        this.packetInteractionType = packetInteractionType;
        this.date = ZonedDateTime.now(ZoneId.systemDefault());
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        String dt = date.toString().substring(11, 19);
        return "x=" + x + ", y=" + y + " (" + dt + ')';
    }
}
