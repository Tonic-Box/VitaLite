package com.tonic.util;

import com.tonic.Logger;
import com.tonic.util.Coroutine;
import com.tonic.util.ReflectUtil;
import com.tonic.util.ThreadPool;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.Config;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import java.util.concurrent.Future;

public class VitaPlugin extends Plugin
{
    private Future<?> loopFuture = null;

    /**
     * Overridable loop() method. It is safe to sleep in, but as a result is
     * not thread safe, so you must use invoke()'s to do operations that require
     * thread safety. It is started from the start of a gametick.
     * @throws Exception exception
     */
    public void loop() throws Exception
    {
    }

    /**
     * Subscriber to the gametick event to handle dealing with starting new futures for our loop() method
     * as necessary.
     */
    @Subscribe
    public final void onGameTick(GameTick event) {
        if (!ReflectUtil.isOverridden(this, "loop"))
            return;

        if(loopFuture != null && !loopFuture.isDone() && !loopFuture.isCancelled())
            return;

        loopFuture = ThreadPool.submit(new Coroutine(() -> {
            try
            {
                loop();
            }
            catch (RuntimeException e)
            {
                Logger.norm("[" + getName() + "] Plugin::loop() has been interrupted.");
            }
            catch (Throwable e)
            {
                Logger.error(e, "[" + getName() + "] Error in loop(): %e");
            }
            finally
            {
                Coroutine.dispose();
            }
        }));
    }

    /**
     * Gracefully prematurely end/cancel a running async loop().
     * @param callback callback
     */
    public void haltLoop(Runnable callback)
    {
        if(loopFuture == null || loopFuture.isDone())
            callback.run();
        Coroutine._cancel();
        ThreadPool.submit(() -> {
            while(!loopFuture.isDone())
            {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            if(callback != null)
                callback.run();
        });
    }
}
