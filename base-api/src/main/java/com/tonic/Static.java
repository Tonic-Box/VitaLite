package com.tonic;

import com.google.inject.Injector;
import com.tonic.api.TClient;
import com.tonic.headless.HeadlessMode;
import com.tonic.model.RuneLite;
import com.tonic.util.ClientConfig;
import com.tonic.util.RuneliteConfigUtil;
import com.tonic.util.ThreadPool;
import com.tonic.util.config.ConfigFactory;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
    @Getter
    private static VitaLiteOptions cliArgs = new VitaLiteOptions();
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

    public static ClassLoader getClientClassLoader()
    {
        return CLIENT_OBJECT.getClass().getClassLoader();
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
                if(CLIENT_OBJECT != null)
                    return;
                CLIENT_OBJECT = object;
                break;
            case "RL":
                if(RL != null)
                    return;
                RL = (RuneLite) object;
                break;
            case "CLASSLOADER":
                if(classLoader != null)
                    return;
                classLoader = (ClassLoader) object;
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

    public static void invokeLater(Runnable runnable)
    {
        TClient T_CLIENT = (TClient) CLIENT_OBJECT;
        if (!T_CLIENT.isClientThread()) {
            getRuneLite().getClientThread().invokeLater(runnable);
        } else {
            runnable.run();
        }
    }

    public static <T> T invokeLater(Supplier<T> supplier) {
        TClient T_CLIENT = (TClient) CLIENT_OBJECT;
        if (!T_CLIENT.isClientThread()) {
            CompletableFuture<T> future = new CompletableFuture<>();
            Runnable runnable = () -> future.complete(supplier.get());
            invokeLater(runnable);
            return future.join();
        } else {
            return supplier.get();
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

    public static boolean isRunningFromShadedJar() {
        try {
            final Manifest manifest = new Manifest(Static.class.getClassLoader()
                    .getResourceAsStream("META-INF/MANIFEST.MF"));
            final Attributes attrs = manifest.getMainAttributes();
            final String version = attrs.getValue("Implementation-Version");
            return version != null;
        } catch (final IOException e) {
            return false;
        }
    }
}
