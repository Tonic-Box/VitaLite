package com.tonic.packets.types;

import com.google.gson.annotations.Expose;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MapEntry
{
    @Expose
    private String name;
    @Expose
    private PacketEntry packet;
    @Expose
    private List<String> writes;
    @Expose
    private List<String> reads;
    @Expose
    private List<String> obfuWrites;
    @Expose
    private List<String> args;

    @Override
    public String toString()
    {
        StringBuilder out = new StringBuilder(name + " [" + packet.getId() + "]\n");
        out.append("\tPacketNode packetNode = new PacketNode(ClientPacket.").append(packet.getField()).append(", isaacShit);\n");
        for(int i = 0; i < writes.size(); i++)
        {
            out.append("\tpacketNode.").append(writes.get(i)).append("(").append(args.get(i)).append(");\n");
        }
        out.append("\tpacketWriter.addNode(packetNode);\n");
        return out.toString();
    }
}