package com.tonic.services.pathfinder.sailing.sea;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import net.runelite.api.coords.WorldPoint;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The sea map's file form, VSEA
 */
final class SeaCodec
{
    static final int MAGIC = 0x56534541;
    static final int VERSION = 1;
    static final int REGION_TILES = SeaMap.REGION_SIZE * SeaMap.REGION_SIZE;
    private static final int NO_TILE = -1;

    private SeaCodec()
    {
    }

    static void write(OutputStream stream, Int2ObjectOpenHashMap<byte[]> info, Int2ObjectOpenHashMap<Short2ShortOpenHashMap> masks, List<SeaMap.Dock> docks) throws IOException
    {
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(stream)))
        {
            out.writeInt(MAGIC);
            out.writeByte(VERSION);
            int[] regions = info.keySet().toIntArray();
            Arrays.sort(regions);
            out.writeInt(regions.length);
            for (int region : regions)
            {
                out.writeInt(region);
                out.write(info.get(region));
                Short2ShortOpenHashMap partial = masks.get(region);
                out.writeInt(partial == null ? 0 : partial.size());
                if (partial != null)
                {
                    short[] indices = partial.keySet().toShortArray();
                    Arrays.sort(indices);
                    for (short index : indices)
                    {
                        out.writeShort(index);
                        out.writeShort(partial.get(index));
                    }
                }
            }
            out.writeInt(docks.size());
            for (SeaMap.Dock dock : docks)
            {
                out.writeByte(dock.dockId());
                out.writeInt(dock.locId());
                writeTile(out, dock.land());
                writeTile(out, dock.approach());
            }
        }
    }

    static SeaMap read(InputStream stream) throws IOException
    {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(stream)))
        {
            int magic = in.readInt();
            if (magic != MAGIC)
            {
                throw new IOException("not a sea map (bad magic 0x" + Integer.toHexString(magic) + ")");
            }
            int version = in.readUnsignedByte();
            if (version != VERSION)
            {
                throw new IOException("unsupported sea map version " + version);
            }
            Int2ObjectOpenHashMap<byte[]> info = new Int2ObjectOpenHashMap<>();
            Int2ObjectOpenHashMap<Short2ShortOpenHashMap> masks = new Int2ObjectOpenHashMap<>();
            int regions = in.readInt();
            for (int i = 0; i < regions; i++)
            {
                int region = in.readInt();
                byte[] tiles = new byte[REGION_TILES];
                in.readFully(tiles);
                info.put(region, tiles);
                int partialCount = in.readInt();
                if (partialCount > 0)
                {
                    Short2ShortOpenHashMap partial = new Short2ShortOpenHashMap(partialCount);
                    for (int p = 0; p < partialCount; p++)
                    {
                        partial.put(in.readShort(), in.readShort());
                    }
                    masks.put(region, partial);
                }
            }
            int dockCount = in.readInt();
            List<SeaMap.Dock> docks = new ArrayList<>(dockCount);
            for (int d = 0; d < dockCount; d++)
            {
                int dockId = in.readUnsignedByte();
                int locId = in.readInt();
                docks.add(new SeaMap.Dock(dockId, locId, readTile(in), readTile(in)));
            }
            return new SeaMap(info, masks, docks);
        }
    }

    private static void writeTile(DataOutput out, WorldPoint tile) throws IOException
    {
        out.writeShort(tile == null ? NO_TILE : tile.getX());
        out.writeShort(tile == null ? NO_TILE : tile.getY());
        out.writeByte(tile == null ? 0 : tile.getPlane());
    }

    private static WorldPoint readTile(DataInput in) throws IOException
    {
        int x = in.readShort();
        int y = in.readShort();
        int plane = in.readUnsignedByte();
        return x == NO_TILE ? null : new WorldPoint(x, y, plane);
    }
}
