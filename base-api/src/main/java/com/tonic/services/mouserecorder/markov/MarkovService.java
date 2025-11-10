package com.tonic.services.mouserecorder.markov;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.services.mouserecorder.markov.ui.MarkovTrainerMonitor;

/**
 * Service facade for Markov Mouse Trainer.
 * Provides static access to trainer and monitoring UI.
 */
public class MarkovService
{
    private static final MarkovMouseTrainer TRAINER = new MarkovMouseTrainer(MarkovService::getMouseX, MarkovService::getMouseY);

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (TRAINER.isRunning())
            {
                TRAINER.stop();
            }
        }, "MarkovService-Shutdown"));
    }

    /**
     * Toggles recording on/off.
     */
    public static void toggle(boolean toggle)
    {
        if(toggle)
        {
            TRAINER.start();
        }
        else
        {
            TRAINER.stop();
        }
    }

    /**
     * Starts recording and training.
     */
    public static void start()
    {
        TRAINER.start();
    }

    /**
     * Stops recording and training.
     */
    public static void stop()
    {
        TRAINER.stop();
    }

    /**
     * Checks if currently recording.
     */
    public static boolean isRunning()
    {
        return TRAINER.isRunning();
    }

    /**
     * Gets comprehensive training statistics.
     */
    public static MarkovMouseTrainer.TrainingStatistics getStatistics()
    {
        return TRAINER.getStatistics();
    }

    /**
     * Calculates overall training quality score (0.0 to 1.0).
     * Based on quantity (30%), diversity (40%), and breadth (30%).
     *
     * @return Quality score where 0.5 = "Fair", 0.7 = "Good", 0.85+ = "Excellent"
     */
    public static double getQualityScore()
    {
        MarkovChainData.ChainStatistics stats = TRAINER.getChainData().getStatistics();

        double quantityScore = Math.min(stats.totalTransitions / 10000.0, 1.0);
        double diversityScore = Math.min(stats.uniqueMovements / 500.0, 1.0);
        double breadthScore = Math.min(stats.avgTransitionsPerState / 20.0, 1.0);

        return (quantityScore * 0.3) + (diversityScore * 0.4) + (breadthScore * 0.3);
    }

    /**
     * Gets the trainer instance.
     */
    public static MarkovMouseTrainer getTrainer()
    {
        return TRAINER;
    }

    /**
     * Toggles the monitoring UI window.
     */
    public static void toggleMonitor()
    {
        MarkovTrainerMonitor.toggleMonitor(TRAINER);
    }

    /**
     * Shows the monitoring UI window.
     */
    public static void showMonitor()
    {
        MarkovTrainerMonitor.show(TRAINER);
    }

    /**
     * Hides the monitoring UI window.
     */
    public static void hideMonitor()
    {
        MarkovTrainerMonitor.hideMonitor();
    }

    /**
     * Checks if monitoring UI is currently visible.
     */
    public static boolean isMonitorShowing()
    {
        return MarkovTrainerMonitor.isMonitorVisible();
    }

    private static int getMouseX()
    {
        TClient client = Static.getClient();
        if (client == null || client.getMouseHandler() == null)
        {
            return -1;
        }
        return client.getMouseHandler().getMouseX();
    }

    private static int getMouseY()
    {
        TClient client = Static.getClient();
        if(client == null || client.getMouseHandler() == null)
        {
            return -1;
        }
        return client.getMouseHandler().getMouseY();
    }
}
