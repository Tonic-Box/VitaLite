package com.tonic.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tonic.Static;
import com.tonic.packets.types.MapEntry;
import com.tonic.packets.types.PacketDefinition;
import com.tonic.util.StaticIntFinder;
import net.runelite.api.NullNpcID;
import net.runelite.api.NullObjectID;
import net.runelite.api.gameval.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PacketMapReader
{
    private static List<MapEntry> defs;
    private static final Gson gson = new GsonBuilder().create();
    private static Map<Integer, MapEntry> idToEntryMap;

    public static List<MapEntry> get()
    {
        if(defs == null)
        {
            fillMaps();
        }
        return defs;
    }

    public static MapEntry get(String packet)
    {
        if(defs == null)
        {
            fillMaps();
        }
        return defs.stream()
                .filter(e -> e.getName().equals(packet))
                .findFirst().orElse(null);
    }

    public static int getId(String packet)
    {
        if(defs == null)
        {
            fillMaps();
        }
        var entry = defs.stream()
                .filter(e -> e.getName().equals(packet))
                .findFirst().orElse(null);
        if(entry == null)
            return -1;
        return entry.getPacket().getId();
    }

    public static MapEntry get(int id)
    {
        if(defs == null)
        {
            fillMaps();
        }
        return defs.stream()
                .filter(e -> e.getPacket().getId() == id)
                .findFirst().orElse(null);
    }

    public static String prettify(PacketBuffer buffer)
    {
        if(defs == null)
        {
            fillMaps();
        }

        if(!idToEntryMap.containsKey(buffer.getPacketId()))
        {
            return "[UNKNOWN(" + buffer.getPacketId() + ")] " + buffer;
        }

        MapEntry entry = idToEntryMap.get(buffer.getPacketId());
        if(entry.getName().equals("OP_MOUSE_MOVEMENT"))
            return "[UNKNOWN(" + buffer.getPacketId() + ")] " + buffer;

        StringBuilder out = new StringBuilder("[" + entry.getName() + "(" + entry.getPacket().getId() + ")] ");
        long num;
        String name;
        for(int i = 0; i < entry.getReads().size(); i++)
        {
            if(isParsableAsNumber(entry.getArgs().get(i)))
            {
                doRead(buffer, entry.getReads().get(i));
            }
            else
            {
                num = doRead(buffer, entry.getReads().get(i));
                if(num == 65535)
                {
                    num = -1;
                }
                if(Static.getVitaConfig().shouldLogNames())
                {
                    if(entry.getArgs().get(i).toLowerCase().contains("widgetid"))
                    {
                        name = StaticIntFinder.find(InterfaceID.class, (int) num);
                        out.append(entry.getArgs().get(i)).append("=").append(name).append(", ");
                        continue;
                    }
                    if(entry.getArgs().get(i).toLowerCase().contains("itemid"))
                    {
                        name = StaticIntFinder.find(ItemID.class, (int) num);
                        out.append(entry.getArgs().get(i)).append("=").append(name).append(", ");
                        continue;
                    }
                    if(entry.getName().startsWith("OP_GAME_OBJECT_ACTION_") && entry.getArgs().get(i).equals("identifier"))
                    {
                        name = StaticIntFinder.find(ObjectID.class, (int) num);
                        out.append(entry.getArgs().get(i)).append("=").append(name).append(", ");
                        continue;
                    }
                }
                out.append(entry.getArgs().get(i)).append("=").append(num).append(", ");
            }
        }
        buffer.setOffset(0);
        return out.toString();
    }

    /**
     * Analyzes a packet buffer and returns a packet definition.
     *
     * @param buffer The packet buffer to analyze.
     * @return The packet definition.
     */
    public static PacketDefinition analyze(PacketBuffer buffer)
    {
        MapEntry entry = get().stream()
                .filter(e -> e.getPacket().getId() == buffer.getPacketId())
                .findFirst().orElse(null);

        if(entry == null)
        {
            return null;
        }

        PacketDefinition definition = new PacketDefinition(entry.getName(), buffer);

        for(int i = 0; i < entry.getReads().size(); i++)
        {
            if(isParsableAsNumber(entry.getArgs().get(i)))
            {
                doRead(buffer, entry.getReads().get(i)); //shit to ignore
                continue;
            }
            definition.getMap().put(entry.getArgs().get(i), doRead(buffer, entry.getReads().get(i)));
        }
        buffer.setOffset(0);
        return definition;
    }

    private static long doRead(PacketBuffer buffer, String method)
    {
        switch (method) {
            case "readByte":
                return buffer.readByte();
            case "readByteAdd":
                return buffer.readByteAdd();
            case "readByteNeg":
                return buffer.readByteNeg();
            case "readByteSub":
                return buffer.readByteSub();
            case "readLengthByte":
                return buffer.readLengthByte();
            case "readBoolean":
                return buffer.readBoolean() ? 1 : 0;
            case "readBooleanAdd":
                return buffer.readBooleanAdd() ? 1 : 0;
            case "readBooleanNeg":
                return buffer.readBooleanNeg() ? 1 : 0;
            case "readBooleanSub":
                return buffer.readBooleanSub() ? 1 : 0;
            case "readShort":
                return buffer.readUnsignedShort();
            case "readShortAdd":
                return buffer.readUnsignedShortAdd();
            case "readShortLE":
                return buffer.readUnsignedShortLE();
            case "readShortAddLE":
                return buffer.readUnsignedShortAddLE();
            case "readLengthShort":
                return buffer.readLengthShort();
            case "readMedium":
                return buffer.readMedium();
            case "readInt":
                return buffer.readInt();
            case "readIntME":
                return buffer.readIntME();
            case "readIntLE":
                return buffer.readIntLE();
            case "readIntIME":
                return buffer.readIntIME();
            case "readVarInt":
                return buffer.readVarInt();
            case "readLengthInt":
                return buffer.readLengthInt();
            case "readLong":
                return buffer.readLong();
            case "readFloat":
                return (int) buffer.readFloat();
            default:
                return -1;
        }
    }

    public static PacketBuffer createBuffer(MapEntry entry, Map<String,Object> args)
    {
        PacketBuffer buffer = new PacketBuffer(entry.getPacket().getId(), entry.getPacket().getLength());
        for(int i = 0; i < entry.getWrites().size(); i++)
        {
            if (args.get(entry.getArgs().get(i)) != null)
            {
                Object object = args.get(entry.getArgs().get(i));
                if(object instanceof Integer)
                {
                    int number = (int) object;
                    doWrite(buffer, entry.getWrites().get(i), number);
                }
                else if(object instanceof String)
                {
                    String string = (String) object;
                    doWriteStr(buffer, entry.getWrites().get(i), string);
                }

            }
            else if(isParsableAsNumber(entry.getArgs().get(i)))
            {
                doWrite(buffer, entry.getWrites().get(i), Integer.parseInt(entry.getArgs().get(i)));
            }
            else if(entry.getArgs().get(i).equals("true") || entry.getArgs().get(i).equals("false"))
            {
                doWrite(buffer, entry.getWrites().get(i), (entry.getArgs().get(i).equals("true") ? 1 : 0));
            }
        }
        return buffer;
    }

    private static void doWriteStr(PacketBuffer buffer, String method, String value) {
        switch (method) {
            case "writeStringCp1252NullTerminated":
                buffer.writeStringCp1252NullTerminated(value);
                break;
            case "writeStringCp1252NullCircumfixed":
                buffer.writeStringCp1252NullCircumfixed(value);
                break;
            case "writeCESU8":
                buffer.writeCESU8(value);
                break;
        }
    }

    private static void doWrite(PacketBuffer buffer, String method, int value) {
        switch (method) {
            case "writeByte":
                buffer.writeByte(value);
                break;
            case "writeByteAdd":
                buffer.writeByteAdd(value);
                break;
            case "writeByteNeg":
                buffer.writeByteNeg(value);
                break;
            case "writeByteSub":
                buffer.writeByteSub(value);
                break;
            case "writeLengthByte":
                buffer.writeLengthByte(value);
                break;
            case "writeShort":
                buffer.writeShort(value);
                break;
            case "writeShortAdd":
                buffer.writeShortAdd(value);
                break;
            case "writeShortLE":
                buffer.writeShortLE(value);
                break;
            case "writeShortAddLE":
                buffer.writeShortAddLE(value);
                break;
            case "writeLengthShort":
                buffer.writeLengthShort(value);
                break;
            case "writeMedium":
                buffer.writeMedium(value);
                break;
            case "writeInt":
                buffer.writeInt(value);
                break;
            case "writeIntME":
                buffer.writeIntME(value);
                break;
            case "writeIntLE":
                buffer.writeIntLE(value);
                break;
            case "writeIntIME":
                buffer.writeIntIME(value);
                break;
            case "writeVarInt":
                buffer.writeVarInt(value);
                break;
            case "writeLengthInt":
                buffer.writeLengthInt(value);
                break;
            case "writeLong":
                buffer.writeLong(value);
                break;
            case "writeFloat":
                buffer.writeFloat(value);
                break;
            default:
                break;
        }
    }

    private static boolean isParsableAsNumber(String str) {
        if (str == null || str.isEmpty()) return false;

        int start = 0;
        if (str.charAt(0) == '-') {
            if (str.length() == 1) return false;
            start = 1;
        }

        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    public static void fillMaps()
    {
        try
        {
            try (InputStream inputStream = PacketMapReader.class.getResourceAsStream("packets.json")) {
                assert inputStream != null;
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String fileContent = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
                defs = gson.fromJson(fileContent, new TypeToken<ArrayList<MapEntry>>(){}.getType());
            }

            idToEntryMap = defs.stream()
                    .collect(Collectors.toMap(
                            e -> e.getPacket().getId(),
                            e -> e,
                            (e1, e2) -> e1
                    ));
            System.out.println("Loaded " + defs.size() + " packet definitions.");
        }
        catch (IOException e)
        {
            System.err.println("PacketMapReader::fillMaps // " + e.getMessage());
            System.exit(0);
            defs = new ArrayList<>();
        }
    }
}