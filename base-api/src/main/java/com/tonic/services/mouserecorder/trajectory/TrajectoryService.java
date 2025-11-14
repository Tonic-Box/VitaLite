package com.tonic.services.mouserecorder.trajectory;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.mouserecorder.MouseDataPoint;

import java.io.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service layer for managing trajectory recording, storage, and persistence.
 * Handles training data lifecycle and provides access to the trajectory database.
 */
public class TrajectoryService
{
    private static final TrajectoryDatabase database = new TrajectoryDatabase();
    private static final TrajectoryPacketCapture packetCapture = new TrajectoryPacketCapture(database);
    private static final ExecutorService saveExecutor = Executors.newSingleThreadExecutor();
    private static final String SAVE_PATH = Static.VITA_DIR
            .resolve("data")
            .resolve("trajectories.dat")
            .toString();
    private static volatile boolean isRecording = false;
    private static final Timer autoSaveTimer = new Timer("TrajectoryAutoSave", true);
    private static final long AUTO_SAVE_INTERVAL_MS = 30_000;
    private static volatile int lastSavedCount = 0;

    static
    {
        loadFromFile();
        startAutoSave();
    }

    public static TrajectoryDatabase getDatabase()
    {
        return database;
    }

    public static TrajectoryPacketCapture getPacketCapture()
    {
        return packetCapture;
    }

    public static int getUnsavedCount()
    {
        return database.getTrajectoryCount() - lastSavedCount;
    }

    public static void startRecording()
    {
        isRecording = true;
        packetCapture.startRecording();
        Logger.info("Trajectory recording started (packet capture enabled)");
    }

    public static void stopRecording()
    {
        isRecording = false;
        packetCapture.stopRecording();
        Logger.info("Trajectory recording stopped");
    }

    public static boolean isRecording()
    {
        return isRecording;
    }

    public static void recordTrajectory(List<MouseDataPoint> points)
    {
        if (points == null || points.isEmpty())
        {
            return;
        }

        if (points.size() < 2)
        {
            return;
        }

        if (!isRecording)
        {
            return;
        }

        database.addTrajectory(points);
    }

    private static void startAutoSave()
    {
        autoSaveTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                if (database.getTrajectoryCount() > 0)
                {
                    saveToFile();
                }
            }
        }, AUTO_SAVE_INTERVAL_MS, AUTO_SAVE_INTERVAL_MS);
    }

    public static void saveToFile()
    {
        saveExecutor.submit(() -> {
            try
            {
                File file = new File(SAVE_PATH);
                file.getParentFile().mkdirs();

                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file)))
                {
                    List<Trajectory> trajectories = database.getAllTrajectories();
                    oos.writeInt(trajectories.size());

                    for (Trajectory traj : trajectories)
                    {
                        oos.writeObject(traj.getPoints());
                    }

                    lastSavedCount = trajectories.size();
                }
            }
            catch (IOException e)
            {
                Logger.error("Failed to save trajectories: " + e.getMessage());
            }
        });
    }

    public static void loadFromFile()
    {
        File file = new File(SAVE_PATH);
        if (!file.exists())
        {
            Logger.info("No trajectory data file found");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file)))
        {
            int count = ois.readInt();
            database.clear();

            for (int i = 0; i < count; i++)
            {
                @SuppressWarnings("unchecked")
                List<MouseDataPoint> points = (List<MouseDataPoint>) ois.readObject();
                database.addTrajectory(points);
            }

            lastSavedCount = count;
            Logger.info("Loaded " + count + " trajectories");
        }
        catch (IOException | ClassNotFoundException e)
        {
            Logger.error("Failed to load trajectories: " + e.getMessage());
        }
    }

    public static void clearData()
    {
        database.clear();
        lastSavedCount = 0;
        Logger.info("Trajectory database cleared");
    }

    public static TrajectoryGenerator createGenerator()
    {
        return new TrajectoryGenerator(database);
    }
}
