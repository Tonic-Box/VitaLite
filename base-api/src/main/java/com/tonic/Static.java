package com.tonic;

import com.google.inject.Injector;
import com.tonic.api.TClient;
import com.tonic.headless.HeadlessMode;
import com.tonic.model.RuneLite;
import com.tonic.util.ClientConfig;
import com.tonic.util.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Static access to important stuff
 */
public class Static
{
    public static final Path RUNELITE_DIR = Path.of(System.getProperty("user.home"), ".runelite");
    public static final Path VITA_DIR = Path.of(RUNELITE_DIR.toString(), "vitalite");
    @Getter
    private static boolean headless = false;
    @Getter
    private static final ClientConfig vitaConfig = ConfigFactory.create(ClientConfig.class);
    @Getter
    private static ClassLoader classLoader;
    private static Object CLIENT_OBJECT;
    private static RuneLite RL;

    /**
     * get client instance
     * @return client instance
     * @param <T> client type (TClient/Client)
     */
    public static <T> T getClient()
    {
        return (T) CLIENT_OBJECT;
    }

    /**
     * get runelite wrapper instance
     * @return runelite wrapper instance
     */
    public static RuneLite getRuneLite()
    {
        return RL;
    }

    /**
     * get guice injector
     * @return guice injector
     */
    public static Injector getInjector()
    {
        return RL.getInjector().getInjector();
    }

    /**
     * INTERNAL USE ONLY
     */
    public static void set(Object object, String name)
    {
        switch (name)
        {
            case "RL_CLIENT":
                CLIENT_OBJECT = Objects.requireNonNull(object, "RL_CLIENT cannot be null");
                break;
            case "RL":
                RL = (RuneLite) Objects.requireNonNull(object, "RL cannot be null");
                break;
            case "CLASSLOADER":
                classLoader = (ClassLoader) Objects.requireNonNull(object, "CLASSLOADER cannot be null");
                break;
            default:
                throw new IllegalArgumentException("Unknown class name: " + name);
        }
    }

    /**
     * invoke on client thread with return
     *
     * @param supplier runnable block
     * @return return value
     */
    public static <T> T invoke(Supplier<T> supplier) {
        TClient T_CLIENT = (TClient) CLIENT_OBJECT;
        if (!T_CLIENT.isClientThread()) {
            CompletableFuture<T> future = new CompletableFuture<>();
            Runnable runnable = () -> future.complete(supplier.get());
            invoke(runnable);
            return future.join();
        } else {
            return supplier.get();
        }
    }

    /**
     * invoke on client thread
     *
     * @param runnable runnable block
     */
    public static void invoke(Runnable runnable) {
        TClient T_CLIENT = (TClient) CLIENT_OBJECT;
        if (!T_CLIENT.isClientThread()) {
            getRuneLite().getClientThread().invoke(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * post event to event bus
     * @param event event object
     */
    public static void post(Object event)
    {
        getRuneLite().getEventBus().post(event);
    }

    /**
     * Enable or disable headless mode.
     *
     * @param headless true to enable headless mode, false to disable it
     */
    public static void setHeadless(boolean headless) {
        Static.headless = headless;
        HeadlessMode.toggleHeadless(headless);
    }
}
