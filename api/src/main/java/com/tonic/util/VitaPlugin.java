package com.tonic.util;

import com.tonic.Logger;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;

import javax.inject.Inject;
import java.util.concurrent.Future;

/**
 * A VitaLite Plugin base class for looped plugins (threaded).
 */
public class VitaPlugin extends Plugin
{
    @Inject
    Client client;
    private Future<?> loopFuture = null;
    private volatile boolean shutdown = false;
    private volatile boolean started = false; // Track if plugin is actually enabled/started

    /**
     * Constructor
     */
    public VitaPlugin() {
        super();
    }

    /**
     * Subscriber to the gametick event to handle dealing with starting new futures for our loop() method
     * as necessary when logged in.
     */
    @Subscribe
    public final void onGameTick(GameTick event) {
        triggerLoop();
    }

    /**
     * Start a background thread to poll for login screen and trigger loop() 
     * Similar to how AutoLogin works in GameManager
     */
    @Override
    protected void startUp() throws Exception {
        super.startUp();
        started = true;
        shutdown = false; // Reset shutdown flag
        Logger.norm("[VitaPlugin] Plugin started, loop() enabled for " + getName());
        
        // Start login screen polling thread
        ThreadPool.submit(() -> {
            Logger.norm("[VitaPlugin] Polling thread started for " + getName());
            while (!isPluginShutdown()) {
                try {
                    if (client != null && (client.getGameState() == net.runelite.api.GameState.LOGIN_SCREEN || 
                        client.getGameState() == net.runelite.api.GameState.LOGIN_SCREEN_AUTHENTICATOR)) {
                        Logger.norm("[" + getName() + "] Login screen detected, triggering loop()");
                        triggerLoop();
                        // Wait a bit before next check (similar to game tick rate)
                        Thread.sleep(600);
                    } else {
                        // When not on login screen, check less frequently
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Logger.norm("[VitaPlugin] Polling thread interrupted");
                    break; // Exit thread if interrupted
                } catch (Exception e) {
                    Logger.error(e, "[" + getName() + "] Error in login screen polling: %e");
                }
            }
            Logger.norm("[VitaPlugin] Login screen polling thread stopped");
        });
    }

    @Override
    protected void shutDown() throws Exception {
        started = false;
        shutdown = true;
        super.shutDown();
        Logger.norm("[VitaPlugin] Plugin shut down, loop() disabled for " + getName());
    }
    
    private boolean isPluginShutdown() {
        return shutdown;
    }

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
     * Shared method to trigger loop() execution
     */
    private void triggerLoop() {
        if (!ReflectUtil.isOverridden(this, "loop")) {
            return;
        }

        if(loopFuture != null && !loopFuture.isDone()) {
            return;
        }

        loopFuture = ThreadPool.submit(new AsyncTask(() -> {
            try
            {
                loop();
            }
            catch (RuntimeException e)
            {
                // Log unexpected RuntimeExceptions
                Logger.norm("[" + getName() + "] Plugin::loop() has been interrupted.");
            }
            catch (Throwable e)
            {
                Logger.error(e, "[" + getName() + "] Error in loop(): %e");
                e.printStackTrace();
            }
            finally
            {
                AsyncTask.dispose();
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
        System.out.println("Halting " + getName() + " loop...");
        AsyncTask._cancel();
        ThreadPool.submit(() -> {
            while(!loopFuture.isDone())
            {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            System.out.println(getName() + " loop halted.");
            if(callback != null)
                callback.run();
        });
    }
}
