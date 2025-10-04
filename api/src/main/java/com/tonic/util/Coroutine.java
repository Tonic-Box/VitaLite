package com.tonic.util;

import com.tonic.Logger;
import com.tonic.api.threaded.Delays;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class representing a coroutine that can be cancelled.
 * The coroutine is created from a given Runnable
 */
public class Coroutine implements Runnable {
    private static volatile boolean canceled = false;
    private static volatile Coroutine live = null;
    private final Runnable runnable;
    @Setter
    @Getter
    private boolean shouldYield = false;
    private boolean inYield = false;
    @Getter
    private final AtomicBoolean await = new AtomicBoolean(false);
    @Setter
    private Runnable interruptingChild;
    @Getter
    private long threadId = 0L;

    public Coroutine(@NonNull Runnable runnable) {
        super();
        this.runnable = runnable;
    }

    public static boolean _isCancelled()
    {
        return canceled;
    }

    public boolean isCancelled()
    {
        return canceled;
    }

    public static void _cancel()
    {
        canceled = true;
    }

    public void cancel()
    {
        canceled = true;
    }

    public static void dispose()
    {
        canceled = false;
        live = null;
    }

    public void yieldTo(Runnable task)
    {
        this.interruptingChild = task;
        this.shouldYield = true;
    }

    public static void _yieldTo(Runnable task) {
        if(live == null)
            return;
        live.setInterruptingChild(task);
        live.setShouldYield(true);
    }

    public static void checkYieldStatus()
    {
        if(live == null)
        {
            return;
        }

        if(live.getAwait().get())
        {
            while (live.getAwait().get())
            {
                Delays.wait(100);
            }
        }

        if(!live.inYield && live.shouldYield)
        {
            live.inYield = true;
            if(live.interruptingChild != null)
            {
                live.interruptingChild.run();
                live.interruptingChild = null;
            }
            live.shouldYield = false;
            live.inYield = false;
        }
    }

    public static boolean checkYield()
    {
        if(live == null)
        {
            return false;
        }
        return live.inYield;
    }

    public void await()
    {
        await.set(true);
    }

    public static void _await()
    {
        if(live == null)
        {
            return;
        }
        live.getAwait().set(true);
    }

    public void resume()
    {
        await.set(false);
    }

    public static void _resume()
    {
        if(live == null)
        {
            return;
        }
        live.getAwait().set(false);
    }

    @Override
    public void run() {
        threadId = Thread.currentThread().getId();
        live = this;
        if(runnable == null)
            return;
        try {
            runnable.run();
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e) {
            Logger.error(e);
        }
        finally {
            live = null;
            canceled = false;
        }
    }
}