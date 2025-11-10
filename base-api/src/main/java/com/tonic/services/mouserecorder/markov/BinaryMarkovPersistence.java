package com.tonic.services.mouserecorder.markov;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Efficient binary serialization for Markov chain data.
 * Much faster and more compact than JSON for real-time incremental saves.
 *
 * Format:
 * Header (24 bytes):
 *   - Magic bytes: "MRKV" (4 bytes)
 *   - Version: int (4 bytes)
 *   - Bin size: int (4 bytes)
 *   - Time bin size: int (4 bytes)
 *   - Total transitions: long (8 bytes)
 *
 * Per State:
 *   - From state: 3 ints (12 bytes)
 *   - Transition count: int (4 bytes)
 *   - For each transition:
 *     - To state: 3 ints (12 bytes)
 *     - Count: long (8 bytes)
 */
public class BinaryMarkovPersistence
{
    private static final byte[] MAGIC = {'M', 'R', 'K', 'V'};
    private static final int VERSION = 1;

    /**
     * Saves Markov chain data in binary format.
     *
     * @param chain    Chain data to save
     * @param filePath Path to save to
     * @throws IOException If save fails
     */
    public static void saveBinary(MarkovChainData chain, Path filePath) throws IOException
    {
        Files.createDirectories(filePath.getParent());

        try (DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(Files.newOutputStream(filePath), 65536)))
        {
            writeChain(chain, dos);
        }
    }

    /**
     * Loads Markov chain data from binary format.
     *
     * @param filePath Path to load from
     * @return Loaded chain data
     * @throws IOException If load fails
     */
    public static MarkovChainData loadBinary(Path filePath) throws IOException
    {
        try (DataInputStream dis = new DataInputStream(
            new BufferedInputStream(Files.newInputStream(filePath), 65536)))
        {
            return readChain(dis);
        }
    }

    /**
     * Writes chain data to output stream.
     */
    private static void writeChain(MarkovChainData chain, DataOutputStream dos) throws IOException
    {
        dos.write(MAGIC);
        dos.writeInt(VERSION);
        dos.writeInt(chain.getBinSize());
        dos.writeInt(chain.getTimeBinSize());
        dos.writeLong(chain.getTotalTransitions());
        dos.writeLong(chain.getTotalSamples());
        dos.writeLong(chain.getCreatedTime());
        dos.writeLong(chain.getLastUpdateTime());
        dos.writeInt(chain.transitionCounts.size());
        for (Map.Entry<MovementState, Map<MovementState, AtomicLong>> entry : chain.transitionCounts.entrySet())
        {
            MovementState fromState = entry.getKey();
            Map<MovementState, AtomicLong> transitions = entry.getValue();
            writeState(fromState, dos);
            dos.writeInt(transitions.size());
            for (Map.Entry<MovementState, AtomicLong> transition : transitions.entrySet())
            {
                writeState(transition.getKey(), dos);
                dos.writeLong(transition.getValue().get());
            }
        }
    }

    /**
     * Reads chain data from input stream.
     */
    private static MarkovChainData readChain(DataInputStream dis) throws IOException
    {
        byte[] magic = new byte[4];
        dis.readFully(magic);
        if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] ||
            magic[2] != MAGIC[2] || magic[3] != MAGIC[3])
        {
            throw new IOException("Invalid file format: bad magic bytes");
        }

        int version = dis.readInt();
        if (version != VERSION)
        {
            throw new IOException("Unsupported version: " + version);
        }
        int binSize = dis.readInt();
        int timeBinSize = dis.readInt();
        MarkovChainData chain = new MarkovChainData(binSize, timeBinSize);
        int stateCount = dis.readInt();
        for (int i = 0; i < stateCount; i++)
        {
            MovementState fromState = readState(dis);
            int transitionCount = dis.readInt();
            for (int j = 0; j < transitionCount; j++)
            {
                MovementState toState = readState(dis);
                long count = dis.readLong();
                for (int k = 0; k < count; k++)
                {
                    chain.recordTransition(fromState, toState);
                }
            }
        }

        return chain;
    }

    /**
     * Writes a movement state to stream.
     */
    private static void writeState(MovementState state, DataOutputStream dos) throws IOException
    {
        dos.writeInt(state.getDeltaXBin());
        dos.writeInt(state.getDeltaYBin());
        dos.writeInt(state.getDeltaTimeBin());
    }

    /**
     * Reads a movement state from stream.
     */
    private static MovementState readState(DataInputStream dis) throws IOException
    {
        int dx = dis.readInt();
        int dy = dis.readInt();
        int dt = dis.readInt();
        return new MovementState(dx, dy, dt);
    }

    /**
     * Returns the size of the file in bytes.
     */
    public static long getFileSize(Path filePath) throws IOException
    {
        return Files.size(filePath);
    }

    /**
     * Checks if a file exists and is a valid Markov data file.
     */
    public static boolean isValidFile(Path filePath)
    {
        if (!Files.exists(filePath))
        {
            return false;
        }

        try (DataInputStream dis = new DataInputStream(
            new BufferedInputStream(Files.newInputStream(filePath))))
        {
            byte[] magic = new byte[4];
            dis.readFully(magic);
            return magic[0] == MAGIC[0] && magic[1] == MAGIC[1] &&
                   magic[2] == MAGIC[2] && magic[3] == MAGIC[3];
        }
        catch (IOException e)
        {
            return false;
        }
    }
}
