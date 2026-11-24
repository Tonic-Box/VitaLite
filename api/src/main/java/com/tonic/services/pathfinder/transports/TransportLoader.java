package com.tonic.services.pathfinder.transports;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.transports.hardcoded.CodeTransports;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The transport graph: every transport in the packaged transports.json plus the code-defined ones, by the packed tile they leave from. The graph is replaced whole on every change, so a search reads a graph no one changes under it. In a dev run the transports file in the source tree can be edited, and edits reach the running graph at once.
 */
public final class TransportLoader
{
    private static final String RESOURCE = "transports.json";
    private static final Path SOURCE = Paths.get("api", "src", "main", "resources", "com", "tonic", "services", "pathfinder", RESOURCE);
    private static final Path BACKUP = Static.VITA_DIR.resolve("transports.backup.json");

    private static volatile Int2ObjectOpenHashMap<ObjectArrayList<Transport>> graph = new Int2ObjectOpenHashMap<>();
    private static boolean backedUp;

    private TransportLoader()
    {
    }

    /**
     * Loads the packaged transports and the code-defined ones into a fresh graph.
     */
    public static synchronized void init()
    {
        Int2ObjectOpenHashMap<ObjectArrayList<Transport>> loaded = new Int2ObjectOpenHashMap<>();
        try (InputStream stream = Walker.class.getResourceAsStream(RESOURCE))
        {
            if (stream == null)
            {
                Logger.error("[Pathfinder] " + RESOURCE + " is missing");
            }
            else
            {
                for (Transport transport : TransportJson.read(new String(stream.readAllBytes(), StandardCharsets.UTF_8)))
                {
                    add(loaded, transport);
                }
            }
        }
        catch (IOException e)
        {
            Logger.error(e, "[Pathfinder] Failed to load transports: %e");
        }
        CodeTransports.load(transport -> add(loaded, transport));
        graph = loaded;
        Logger.info("[Pathfinder] Loaded transports from " + loaded.size() + " tiles");
    }

    /**
     * @return the graph as it stands, by the packed tile each transport leaves from; never changed after it is handed out
     */
    public static Int2ObjectOpenHashMap<ObjectArrayList<Transport>> getTransports()
    {
        return graph;
    }

    /**
     * @return every transport in the graph, grouped by the tile it leaves from
     */
    public static Collection<ObjectArrayList<Transport>> all()
    {
        return graph.values();
    }

    /**
     * @return the transports defined by the transports file, the code-defined ones left out
     */
    public static List<Transport> live()
    {
        List<Transport> live = new ArrayList<>();
        for (ObjectArrayList<Transport> list : graph.values())
        {
            for (Transport transport : list)
            {
                if (transport.getClass() == Transport.class)
                {
                    live.add(transport);
                }
            }
        }
        return live;
    }

    /**
     * @param transport a transport
     * @return the file-defined transport in the graph that is the same edge, or null
     */
    public static Transport live(Transport transport)
    {
        String key = transport.identityKey();
        ObjectArrayList<Transport> list = graph.get(transport.getLocation().getPacked());
        if (list == null)
        {
            return null;
        }
        for (Transport candidate : list)
        {
            if (candidate.getClass() == Transport.class && candidate.identityKey().equals(key))
            {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Swaps a file-defined transport for an edited one, in the graph and in the source file.
     *
     * @param old the transport as it is now
     * @param updated what it becomes
     */
    public static synchronized void replace(Transport old, Transport updated)
    {
        Int2ObjectOpenHashMap<ObjectArrayList<Transport>> copy = copy();
        removeFrom(copy, old);
        add(copy, updated);
        graph = copy;

        List<Transport> file = readSource();
        int index = indexOf(file, old.identityKey());
        if (index >= 0)
        {
            file.set(index, updated);
        }
        else
        {
            file.add(updated);
        }
        writeSource(file);
    }

    /**
     * Takes a file-defined transport out of the graph and the source file.
     *
     * @param transport the transport
     */
    public static synchronized void remove(Transport transport)
    {
        Int2ObjectOpenHashMap<ObjectArrayList<Transport>> copy = copy();
        removeFrom(copy, transport);
        graph = copy;

        List<Transport> file = readSource();
        int index = indexOf(file, transport.identityKey());
        if (index >= 0)
        {
            file.remove(index);
            writeSource(file);
        }
    }

    /**
     * Adds transports to the source file and the graph, skipping any already there.
     *
     * @param transports the transports
     */
    public static synchronized void promote(List<Transport> transports)
    {
        List<Transport> file = readSource();
        boolean changed = false;
        Int2ObjectOpenHashMap<ObjectArrayList<Transport>> copy = copy();
        for (Transport transport : transports)
        {
            if (indexOf(file, transport.identityKey()) < 0)
            {
                file.add(transport);
                changed = true;
            }
            if (live(transport) == null)
            {
                add(copy, transport);
            }
        }
        graph = copy;
        if (changed)
        {
            writeSource(file);
        }
    }

    /**
     * @return whether the source file is on disk, as in a dev run, so edits are saved to it
     */
    public static boolean sourceEditable()
    {
        return Files.isRegularFile(SOURCE);
    }

    /**
     * @return the source file edits are saved to
     */
    public static Path sourcePath()
    {
        return SOURCE;
    }

    /**
     * @return where the source file is copied before the first save of a run
     */
    public static Path backupPath()
    {
        return BACKUP;
    }

    private static void add(Int2ObjectOpenHashMap<ObjectArrayList<Transport>> target, Transport transport)
    {
        ObjectArrayList<Transport> list = target.get(transport.getLocation().getPacked());
        if (list == null)
        {
            list = new ObjectArrayList<>();
            target.put(transport.getLocation().getPacked(), list);
        }
        list.add(transport);
    }

    private static void removeFrom(Int2ObjectOpenHashMap<ObjectArrayList<Transport>> target, Transport transport)
    {
        int key = transport.getLocation().getPacked();
        ObjectArrayList<Transport> list = target.get(key);
        if (list == null)
        {
            return;
        }
        String identity = transport.identityKey();
        list.removeIf(candidate -> candidate == transport || candidate.getClass() == Transport.class && candidate.identityKey().equals(identity));
        if (list.isEmpty())
        {
            target.remove(key);
        }
    }

    private static Int2ObjectOpenHashMap<ObjectArrayList<Transport>> copy()
    {
        Int2ObjectOpenHashMap<ObjectArrayList<Transport>> copy = new Int2ObjectOpenHashMap<>(graph.size());
        for (Int2ObjectOpenHashMap.Entry<ObjectArrayList<Transport>> entry : graph.int2ObjectEntrySet())
        {
            copy.put(entry.getIntKey(), new ObjectArrayList<>(entry.getValue()));
        }
        return copy;
    }

    private static int indexOf(List<Transport> transports, String identity)
    {
        for (int i = 0; i < transports.size(); i++)
        {
            if (transports.get(i).identityKey().equals(identity))
            {
                return i;
            }
        }
        return -1;
    }

    private static List<Transport> readSource()
    {
        if (!sourceEditable())
        {
            return new ArrayList<>();
        }
        try
        {
            return TransportJson.read(Files.readString(SOURCE));
        }
        catch (IOException e)
        {
            Logger.error(e, "[Pathfinder] Failed to read " + SOURCE + ": %e");
            return new ArrayList<>();
        }
    }

    private static void writeSource(List<Transport> transports)
    {
        if (!sourceEditable())
        {
            Logger.warn("[Pathfinder] " + SOURCE + " is not on disk; the change reaches the running walker only");
            return;
        }
        try
        {
            if (!backedUp)
            {
                Files.createDirectories(BACKUP.getParent());
                Files.copy(SOURCE, BACKUP, StandardCopyOption.REPLACE_EXISTING);
                backedUp = true;
            }
            Files.write(SOURCE, TransportJson.write(transports).getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            Logger.error(e, "[Pathfinder] Failed to write " + SOURCE + ": %e");
        }
    }
}
