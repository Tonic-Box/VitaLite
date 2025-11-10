package com.tonic.services.mouserecorder.markov;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles saving and loading of trained Markov chain models to/from disk.
 * Uses JSON format for human-readability and portability.
 */
public class MarkovChainPersistence
{
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    /**
     * Saves a Markov chain to a file.
     *
     * @param chain    The chain data to save
     * @param filePath Path to save to
     * @throws IOException If save fails
     */
    public static void save(MarkovChainData chain, Path filePath) throws IOException
    {
        SerializableChainData serializable = toSerializable(chain);

        Files.createDirectories(filePath.getParent());

        try(Writer writer = Files.newBufferedWriter(filePath))
        {
            GSON.toJson(serializable, writer);
        }
    }

    /**
     * Saves a Markov chain to a file.
     *
     * @param chain The chain data to save
     * @param file  File to save to
     * @throws IOException If save fails
     */
    public static void save(MarkovChainData chain, File file) throws IOException
    {
        save(chain, file.toPath());
    }

    /**
     * Loads a Markov chain from a file.
     *
     * @param filePath Path to load from
     * @return Loaded chain data
     * @throws IOException If load fails
     */
    public static MarkovChainData load(Path filePath) throws IOException
    {
        try(Reader reader = Files.newBufferedReader(filePath))
        {
            SerializableChainData serializable = GSON.fromJson(reader, SerializableChainData.class);
            return fromSerializable(serializable);
        }
    }

    /**
     * Loads a Markov chain from a file.
     *
     * @param file File to load from
     * @return Loaded chain data
     * @throws IOException If load fails
     */
    public static MarkovChainData load(File file) throws IOException
    {
        return load(file.toPath());
    }

    /**
     * Converts MarkovChainData to a serializable format.
     */
    private static SerializableChainData toSerializable(MarkovChainData chain)
    {
        SerializableChainData data = new SerializableChainData();
        data.binSize = chain.getBinSize();
        data.timeBinSize = chain.getTimeBinSize();
        data.totalTransitions = chain.getTotalTransitions();
        data.totalSamples = chain.getTotalSamples();
        data.createdTime = chain.getCreatedTime();
        data.lastUpdateTime = chain.getLastUpdateTime();
        data.transitions = new HashMap<>();

        for(Map.Entry<MovementState, Map<MovementState, AtomicLong>> entry : chain.transitionCounts.entrySet())
        {
            String fromKey = stateToKey(entry.getKey());
            Map<String, Long> toStates = new HashMap<>();
            for (Map.Entry<MovementState, AtomicLong> transition : entry.getValue().entrySet())
            {
                String toKey = stateToKey(transition.getKey());
                toStates.put(toKey, transition.getValue().get());
            }
            data.transitions.put(fromKey, toStates);
        }

        return data;
    }

    /**
     * Converts serializable format back to MarkovChainData.
     */
    private static MarkovChainData fromSerializable(SerializableChainData data)
    {
        MarkovChainData chain = new MarkovChainData(data.binSize, data.timeBinSize);
        for (Map.Entry<String, Map<String, Long>> entry : data.transitions.entrySet())
        {
            MovementState fromState = keyToState(entry.getKey());

            for (Map.Entry<String, Long> transition : entry.getValue().entrySet())
            {
                MovementState toState = keyToState(transition.getKey());
                long count = transition.getValue();
                for (long i = 0; i < count; i++)
                {
                    chain.recordTransition(fromState, toState);
                }
            }
        }

        return chain;
    }

    /**
     * Converts a MovementState to a string key for serialization.
     */
    private static String stateToKey(MovementState state)
    {
        return state.getDeltaXBin() + "," + state.getDeltaYBin() + "," + state.getDeltaTimeBin();
    }

    /**
     * Converts a string key back to a MovementState.
     */
    private static MovementState keyToState(String key)
    {
        String[] parts = key.split(",");
        int dx = Integer.parseInt(parts[0]);
        int dy = Integer.parseInt(parts[1]);
        int dt = Integer.parseInt(parts[2]);
        return new MovementState(dx, dy, dt);
    }

    /**
     * Serializable representation of MarkovChainData.
     */
    private static class SerializableChainData
    {
        int binSize;
        int timeBinSize;
        long totalTransitions;
        long totalSamples;
        long createdTime;
        long lastUpdateTime;
        Map<String, Map<String, Long>> transitions;
    }
}
