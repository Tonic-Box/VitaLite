package com.tonic.services.ground;

import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Perspective;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Draws ground triangles for GPU renderers the ground pass cannot reach, such as 117 HD: flat, unlit, see-through models placed in the scene, one set per 8x8 zone, rebuilt only when a zone's triangles change.
 */
final class GroundMeshes
{
    private static final int[] SEED_MODELS = {43330, 5809};
    private static final int ZONE = 8 * 128;
    private static final int MAX_TRIANGLES = 600;
    private static final int SMALL_TRIANGLES = 64;
    private static final int HIDDEN = -2;
    private static final int FLAT = -1;
    private static final int COPY_SPREAD = 1 << 14;

    private final Map<Long, Zone> zones = new HashMap<>();
    private ModelData seed;

    void update(Client client, List<GroundLayer> layers)
    {
        if (seed == null && !loadSeed(client))
        {
            return;
        }

        Map<Long, Zone> current = new HashMap<>();
        for (GroundLayer layer : layers)
        {
            for (GroundTriangles triangles : layer.views())
            {
                for (int triangle = 0; triangle < triangles.count(); triangle++)
                {
                    if ((triangles.colour(triangle) >>> 24) == 0)
                    {
                        continue;
                    }
                    float centreX = (triangles.x(triangle, 0) + triangles.x(triangle, 1) + triangles.x(triangle, 2)) / 3f;
                    float centreY = (triangles.y(triangle, 0) + triangles.y(triangle, 1) + triangles.y(triangle, 2)) / 3f;
                    int zoneX = Math.floorDiv((int) centreX, ZONE);
                    int zoneY = Math.floorDiv((int) centreY, ZONE);
                    long key = key(triangles.worldViewId(), zoneX, zoneY);
                    Zone zone = current.get(key);
                    if (zone == null)
                    {
                        zone = zones.containsKey(key) ? zones.get(key) : new Zone(triangles.worldViewId(), zoneX, zoneY);
                        zone.begin();
                        current.put(key, zone);
                    }
                    zone.add(triangles, triangle);
                }
            }
        }

        for (Iterator<Map.Entry<Long, Zone>> it = zones.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<Long, Zone> entry = it.next();
            if (!current.containsKey(entry.getKey()))
            {
                entry.getValue().remove();
                it.remove();
            }
        }
        for (Map.Entry<Long, Zone> entry : current.entrySet())
        {
            zones.put(entry.getKey(), entry.getValue());
            entry.getValue().commit(client);
        }
    }

    void clear()
    {
        for (Zone zone : zones.values())
        {
            zone.remove();
        }
        zones.clear();
    }

    private boolean loadSeed(Client client)
    {
        for (int id : SEED_MODELS)
        {
            ModelData data = client.loadModelData(id);
            if (data != null && data.getVerticesCount() > 0 && data.getFaceCount() > 0)
            {
                seed = data;
                return true;
            }
        }
        return false;
    }

    private static long key(int worldViewId, int zoneX, int zoneY)
    {
        return ((long) worldViewId << 40) ^ (((long) zoneX & 0xFFFFF) << 20) ^ ((long) zoneY & 0xFFFFF);
    }

    private final class Zone
    {
        private final int worldViewId;
        private final int zoneX;
        private final int zoneY;
        private final List<Chunk> chunks = new ArrayList<>();
        private float[] corners = new float[MAX_TRIANGLES * 9];
        private int[] colours = new int[MAX_TRIANGLES];
        private int count;
        private long hash;
        private long builtHash = Long.MIN_VALUE;

        Zone(int worldViewId, int zoneX, int zoneY)
        {
            this.worldViewId = worldViewId;
            this.zoneX = zoneX;
            this.zoneY = zoneY;
        }

        void begin()
        {
            count = 0;
            hash = 1125899906842597L;
        }

        void add(GroundTriangles triangles, int triangle)
        {
            if (count == colours.length)
            {
                colours = Arrays.copyOf(colours, count * 2);
                corners = Arrays.copyOf(corners, count * 2 * 9);
            }
            int at = count * 9;
            for (int corner = 0; corner < 3; corner++)
            {
                corners[at++] = triangles.x(triangle, corner);
                corners[at++] = triangles.height(triangle, corner);
                corners[at++] = triangles.y(triangle, corner);
            }
            colours[count] = triangles.colour(triangle);
            hash = hash * 31 + colours[count];
            for (int i = count * 9; i < at; i++)
            {
                hash = hash * 31 + Float.floatToIntBits(corners[i]);
            }
            count++;
        }

        void commit(Client client)
        {
            if (hash == builtHash)
            {
                return;
            }

            WorldView worldView = client.getWorldView(worldViewId);
            if (worldView == null)
            {
                return;
            }
            int anchorX = zoneX * ZONE + ZONE / 2 + 64;
            int anchorY = zoneY * ZONE + ZONE / 2 + 64;
            LocalPoint anchor = new LocalPoint(anchorX, anchorY, worldViewId);
            int plane = worldView.getPlane();
            int anchorHeight = Perspective.getTileHeight(client, anchor, plane);

            int needed = (count + MAX_TRIANGLES - 1) / MAX_TRIANGLES;
            while (chunks.size() > needed)
            {
                chunks.remove(chunks.size() - 1).remove();
            }
            for (int index = 0; index < needed; index++)
            {
                if (index == chunks.size())
                {
                    chunks.add(new Chunk(client));
                }
                int from = index * MAX_TRIANGLES;
                int to = Math.min(count, from + MAX_TRIANGLES);
                chunks.get(index).build(client, corners, colours, from, to, anchor, plane, anchorX, anchorY, anchorHeight);
            }
            builtHash = hash;
        }

        void remove()
        {
            for (Chunk chunk : chunks)
            {
                chunk.remove();
            }
            chunks.clear();
            builtHash = Long.MIN_VALUE;
        }
    }

    private final class Chunk
    {
        private final RuneLiteObject object;

        Chunk(Client client)
        {
            object = client.createRuneLiteObject();
        }

        void build(Client client, float[] corners, int[] colours, int from, int to, LocalPoint anchor, int plane, int anchorX, int anchorY, int anchorHeight)
        {
            int triangles = to - from;
            Model built = blank(client, triangles <= SMALL_TRIANGLES ? SMALL_TRIANGLES : MAX_TRIANGLES);
            if (built == null || built.getFaceTransparencies() == null)
            {
                return;
            }

            float[] xs = built.getVerticesX();
            float[] ys = built.getVerticesY();
            float[] zs = built.getVerticesZ();
            int[] first = built.getFaceIndices1();
            int[] second = built.getFaceIndices2();
            int[] third = built.getFaceIndices3();
            int[] colours1 = built.getFaceColors1();
            int[] colours3 = built.getFaceColors3();
            byte[] transparencies = built.getFaceTransparencies();
            int faces = built.getFaceCount();
            int vertices = built.getVerticesCount();

            for (int triangle = 0; triangle < triangles; triangle++)
            {
                int at = (from + triangle) * 9;
                int vertex = triangle * 3;
                for (int corner = 0; corner < 3; corner++)
                {
                    xs[vertex + corner] = corners[at + corner * 3] - anchorX;
                    ys[vertex + corner] = corners[at + corner * 3 + 1] - anchorHeight;
                    zs[vertex + corner] = corners[at + corner * 3 + 2] - anchorY;
                }
                int argb = colours[from + triangle];
                int hsl = JagexColor.rgbToHSL(argb & 0xFFFFFF, 1.0d);
                byte transparency = (byte) (255 - (argb >>> 24));
                int face = triangle * 2;
                first[face] = vertex;
                second[face] = vertex + 1;
                third[face] = vertex + 2;
                first[face + 1] = vertex;
                second[face + 1] = vertex + 2;
                third[face + 1] = vertex + 1;
                for (int side = face; side <= face + 1; side++)
                {
                    colours1[side] = hsl;
                    colours3[side] = FLAT;
                    transparencies[side] = transparency;
                }
            }
            for (int vertex = triangles * 3; vertex < vertices; vertex++)
            {
                xs[vertex] = 0;
                ys[vertex] = 0;
                zs[vertex] = 0;
            }
            for (int face = triangles * 2; face < faces; face++)
            {
                first[face] = 0;
                second[face] = 0;
                third[face] = 0;
                colours3[face] = HIDDEN;
            }
            built.calculateBoundsCylinder();

            object.setModel(built);
            object.setLocation(anchor, plane);
            if (!object.isActive())
            {
                object.setActive(true);
            }
        }

        private Model blank(Client client, int capacity)
        {
            int vertices = capacity * 3;
            int faces = capacity * 2;
            int copies = Math.max((vertices + seed.getVerticesCount() - 1) / seed.getVerticesCount(), (faces + seed.getFaceCount() - 1) / seed.getFaceCount());
            ModelData[] parts = new ModelData[copies];
            for (int i = 0; i < copies; i++)
            {
                parts[i] = seed.shallowCopy().cloneVertices().translate(i * COPY_SPREAD, 0, 0);
            }
            ModelData merged = client.mergeModels(parts, copies);
            if (merged == null)
            {
                return null;
            }

            merged = merged.cloneVertices().cloneColors().cloneTransparencies(true);
            if (merged.getFaceTextures() != null)
            {
                merged = merged.cloneTextures();
                Arrays.fill(merged.getFaceTextures(), (short) -1);
            }
            Model lit = merged.light();
            if (lit == null || lit.getVerticesCount() < vertices || lit.getFaceCount() < faces)
            {
                return null;
            }
            return lit;
        }

        void remove()
        {
            object.setActive(false);
        }
    }
}
