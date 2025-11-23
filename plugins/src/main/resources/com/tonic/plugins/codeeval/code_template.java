import java.util.*;
import java.util.stream.*;
import static java.lang.System.out;
import com.tonic.Static;
import com.google.inject.Injector;

// RuneLite API classes (should be available from JARs)
import net.runelite.api.*;
import net.runelite.api.coords.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import com.tonic.data.wrappers.*;
import com.tonic.api.game.sailing.*;

import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

import net.runelite.api.gameval.*;
import net.runelite.client.events.*;
import net.runelite.client.game.*;

import com.tonic.*;
import com.tonic.services.*;
import com.tonic.services.pathfinder.*;
import com.tonic.api.*;
import com.tonic.api.entities.*;
import com.tonic.api.widgets.*;
import com.tonic.api.game.*;
import com.tonic.api.threaded.*;
import com.tonic.queries.*;
import com.tonic.util.*;
import com.tonic.data.*;

import com.tonic.util.handler.*;
import com.tonic.api.handlers.*;

// VitaLite classes (should be available since they're compiled)
import com.tonic.services.GameManager;

public class %CLASS_NAME% {
    private static ClassLoader getContextClassLoader() {
        // Use the thread's context classloader - should be the RLClassLoader when running inside VitaLite
        return Thread.currentThread().getContextClassLoader();
    }

    @SuppressWarnings("unchecked")
    private static <T> T inject(String className) {
        try {
            // Get injector from Static class directly
            Injector injector = Static.getInjector();

            // Load the target class
            Class<?> targetClass = getContextClassLoader().loadClass(className);

            // Get instance from injector using proper Guice API
            return (T) injector.getInstance(targetClass);
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + className);
            System.err.println("Available classloader: " + getContextClassLoader().getClass().getName());
            System.err.println("Classloader toString: " + getContextClassLoader().toString());
            return null;
        } catch (Exception e) {
            System.err.println("Failed to inject " + className + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T loadClass(String className) {
        try {
            return (T) getContextClassLoader().loadClass(className);
        } catch (Exception e) {
            System.err.println("Failed to load class " + className + ": " + e.getMessage());
            return null;
        }
    }

    public void run() {
        %USER_CODE%
    }
}