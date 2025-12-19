package com.tonic.services.watchdog;

import com.tonic.Logger;
import com.tonic.Static;

import java.applet.Applet;
import java.lang.reflect.Method;

public class ClientReloader {

    public static boolean resetApplet() {
        try {
            Object client = Static.getClient();
            if (client == null) {
                Logger.error("[ClientReloader] Client is null, cannot reset applet");
                return false;
            }

            if (client instanceof Applet) {
                Applet applet = (Applet) client;
                Logger.info("[ClientReloader] Stopping applet...");
                applet.stop();

                Thread.sleep(500);

                Logger.info("[ClientReloader] Starting applet...");
                applet.start();

                Logger.info("[ClientReloader] Applet reset complete");
                return true;
            }

            Method stopMethod = findMethod(client.getClass(), "stop");
            Method startMethod = findMethod(client.getClass(), "start");

            if (stopMethod != null && startMethod != null) {
                Logger.info("[ClientReloader] Stopping client via reflection...");
                stopMethod.invoke(client);

                Thread.sleep(500);

                Logger.info("[ClientReloader] Starting client via reflection...");
                startMethod.invoke(client);

                Logger.info("[ClientReloader] Client reset complete via reflection");
                return true;
            }

            Logger.error("[ClientReloader] Client is not an Applet and no stop/start methods found");
            return false;
        } catch (Throwable t) {
            Logger.error("[ClientReloader] Failed to reset applet: " + t.getMessage());
            return false;
        }
    }

    public static boolean reloadClient() {
        try {
            Logger.info("[ClientReloader] Starting full client reload...");

            unregisterEventListeners();

            stopPlugins();

            clearStaticFields();

            Logger.info("[ClientReloader] Client reload prepared - requires re-initialization");
            Logger.warn("[ClientReloader] Full reload requires application restart for complete recovery");

            return true;
        } catch (Throwable t) {
            Logger.error("[ClientReloader] Failed to reload client: " + t.getMessage());
            return false;
        }
    }

    private static void unregisterEventListeners() {
        try {
            Object eventBus = Static.getRuneLite().getEventBus();
            if (eventBus != null) {
                Method unregisterAll = findMethod(eventBus.getClass(), "unregisterAll");
                if (unregisterAll != null) {
                    unregisterAll.invoke(eventBus);
                    Logger.info("[ClientReloader] Unregistered all event listeners");
                }
            }
        } catch (Throwable t) {
            Logger.warn("[ClientReloader] Failed to unregister event listeners: " + t.getMessage());
        }
    }

    private static void stopPlugins() {
        try {
            Object pluginManager = Static.getRuneLite().getPluginManager();
            if (pluginManager != null) {
                Method stopPlugins = findMethod(pluginManager.getClass(), "stopPlugins");
                if (stopPlugins != null) {
                    stopPlugins.invoke(pluginManager);
                    Logger.info("[ClientReloader] Stopped all plugins");
                }
            }
        } catch (Throwable t) {
            Logger.warn("[ClientReloader] Failed to stop plugins: " + t.getMessage());
        }
    }

    private static void clearStaticFields() {
        try {
            Static.resetForRecovery();
            Logger.info("[ClientReloader] Cleared static fields");
        } catch (Throwable t) {
            Logger.warn("[ClientReloader] Failed to clear static fields: " + t.getMessage());
        }
    }

    private static Method findMethod(Class<?> clazz, String name) {
        try {
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return method;
                }
            }
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return method;
                }
            }
        } catch (Throwable t) {
            Logger.warn("[ClientReloader] Failed to find method " + name + ": " + t.getMessage());
        }
        return null;
    }
}
