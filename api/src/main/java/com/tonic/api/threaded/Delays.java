package com.tonic.api.threaded;

import com.tonic.Static;
import com.tonic.api.entities.NpcAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.util.AsyncTask;
import com.tonic.services.GameManager;
import net.runelite.api.Client;
import net.runelite.api.Player;

import java.util.function.Supplier;

/**
 * Utility class for handling delays and ticks in the game. (For threaded automation)
 */
public class Delays
{
    /**
     * sleeps the thread for x1 tick
     */
    public static void tick()
    {
        tick(1);
    }
    /**
     * sleeps the thread for x ticks
     * @param ticks ticks
     */
    public static void tick(int ticks)
    {
        int tick = GameManager.getTickCount() + ticks;
        int start = GameManager.getTickCount();
        while(GameManager.getTickCount() < tick && GameManager.getTickCount() >= start)
        {
            if(Thread.currentThread().isInterrupted() || AsyncTask._isCancelled())
            {
                throw new RuntimeException();
            }
            wait(20);
        }
    }

    /**
     * sleep for a defined duration in milliseconds
     * @param ms milliseconds
     */
    public static void wait(int ms)
    {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Waits until the specified condition is true.
     * @param condition the condition to be met
     */
    public static void waitUntil(Supplier<Boolean> condition)
    {
        while(!condition.get())
        {
            if(Thread.currentThread().isInterrupted() || AsyncTask._isCancelled())
            {
                throw new RuntimeException();
            }
            wait(100);
        }
    }

    /**
     * Waits until the specified condition is true or the timeout is reached.
     * @param condition the condition to be met
     * @param timeoutMS the maximum time to wait in milliseconds
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean waitUntil(Supplier<Boolean> condition, long timeoutMS)
    {
        long start = System.currentTimeMillis();
        while(!condition.get())
        {
            if(System.currentTimeMillis() - start > timeoutMS)
            {
                return false;
            }
            if(Thread.currentThread().isInterrupted() || AsyncTask._isCancelled())
            {
                throw new RuntimeException();
            }
            wait(100);
        }
        return true;
    }

    /**
     * Waits until the specified condition is true or the timeout is reached.
     * @param condition the condition to be met
     * @param ticks the maximum time to wait in game ticks
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean waitUntil(Supplier<Boolean> condition, int ticks)
    {
        int end = GameManager.getTickCount() + ticks;
        while(!condition.get())
        {
            if(GameManager.getTickCount() >= end)
            {
                return false;
            }
            if(Thread.currentThread().isInterrupted() || AsyncTask._isCancelled())
            {
                throw new RuntimeException();
            }
            wait(100);
        }
        return true;
    }

    public static void waitUntilIdle()
    {
        tick(1);
        while(!Static.invoke(() -> PlayerEx.getLocal().isIdle()))
        {
            tick(1);
        }
    }

    public static void waitUntilOnTile(int worldX, int worldY)
    {
        Client client = Static.getClient();
        Player player = client.getLocalPlayer();
        while((player.getWorldLocation().getX() != worldX || player.getWorldLocation().getY() != worldY))
        {
            tick(1);
        }
    }
}
