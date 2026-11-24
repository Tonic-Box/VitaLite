package com.tonic.services.pathfinder.recorder;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.services.pathfinder.transports.TransportJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The staged transports, recorded or added in the editor, kept in recorded-transports.json apart from the live transports file until promoted.
 */
public final class TransportRecordStore
{
    private static final Path FILE = Static.VITA_DIR.resolve("recorded-transports.json");

    private final List<Transport> records = new ArrayList<>();

    /**
     * Loads the staging file, starting empty when there is none.
     */
    public TransportRecordStore()
    {
        load();
    }

    /**
     * @return a copy of the staged transports, in staging order
     */
    public synchronized List<Transport> getRecords()
    {
        return new ArrayList<>(records);
    }

    /**
     * @return how many transports are staged
     */
    public synchronized int size()
    {
        return records.size();
    }

    /**
     * Stages a transport unless the same edge is already staged.
     *
     * @param transport the transport
     * @return whether it was added
     */
    public synchronized boolean add(Transport transport)
    {
        if (find(transport) != null)
        {
            return false;
        }

        records.add(transport);
        save();
        return true;
    }

    /**
     * @param transport a transport
     * @return whether the same edge is staged
     */
    public synchronized boolean contains(Transport transport)
    {
        return find(transport) != null;
    }

    /**
     * @param transport a transport
     * @return the staged transport that is the same edge, or null
     */
    public synchronized Transport find(Transport transport)
    {
        String key = transport.identityKey();
        for (Transport record : records)
        {
            if (record.identityKey().equals(key))
            {
                return record;
            }
        }
        return null;
    }

    /**
     * Swaps the staged transport that is this very object for another.
     *
     * @param old the staged transport
     * @param updated what it becomes
     * @return whether it was staged and replaced
     */
    public synchronized boolean replaceExact(Transport old, Transport updated)
    {
        int index = indexOfExact(old);
        if (index < 0)
        {
            return false;
        }

        records.set(index, updated);
        save();
        return true;
    }

    /**
     * Swaps a staged transport for another, matching it as this very object or else as the same edge.
     *
     * @param old the staged transport
     * @param updated what it becomes
     * @return whether a staged transport was replaced
     */
    public synchronized boolean replace(Transport old, Transport updated)
    {
        int index = indexOfExact(old);
        if (index < 0)
        {
            index = records.indexOf(find(old));
        }
        if (index < 0)
        {
            return false;
        }

        records.set(index, updated);
        save();
        return true;
    }

    /**
     * Unstages the given transports, matched as these very objects only.
     *
     * @param transports the transports
     * @return the ones that were staged and are now removed
     */
    public synchronized List<Transport> removeExact(Collection<Transport> transports)
    {
        List<Transport> removed = new ArrayList<>();
        for (Transport transport : transports)
        {
            int index = indexOfExact(transport);
            if (index >= 0)
            {
                records.remove(index);
                removed.add(transport);
            }
        }
        if (!removed.isEmpty())
        {
            save();
        }
        return removed;
    }

    /**
     * Unstages every staged transport that is the same edge as the given one.
     *
     * @param transport the transport
     * @return whether anything was removed
     */
    public synchronized boolean remove(Transport transport)
    {
        String key = transport.identityKey();
        boolean removed = records.removeIf(record -> record == transport || record.identityKey().equals(key));
        if (removed)
        {
            save();
        }
        return removed;
    }

    /**
     * @return the staged transports as a transports file
     */
    public synchronized String toJson()
    {
        return TransportJson.write(records);
    }

    private int indexOfExact(Transport transport)
    {
        for (int i = 0; i < records.size(); i++)
        {
            if (records.get(i) == transport)
            {
                return i;
            }
        }
        return -1;
    }

    private void load()
    {
        if (!Files.isRegularFile(FILE))
        {
            return;
        }

        try
        {
            records.addAll(TransportJson.read(Files.readString(FILE)));
        }
        catch (Exception e)
        {
            Logger.error(e, "[TransportRecorder] Failed to load " + FILE + ": %e");
        }
    }

    private void save()
    {
        try
        {
            Files.createDirectories(FILE.getParent());
            Files.write(FILE, TransportJson.write(records).getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            Logger.error(e, "[TransportRecorder] Failed to save " + FILE + ": %e");
        }
    }
}
