package com.tonic.services.streamer;

import com.tonic.Static;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.Overlay;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Tells stock RuneLite content (the client, its built-in plugins and Plugin Hub plugins) from content VitaLite or its plugins add, by the class loader and package of the class behind it. RuneLite's developer plugins, which a normal RuneLite user cannot run, do not count as stock.
 */
public final class StockContent
{
    private static final String VITA_LOADER = "com.tonic.services.hotswapper.PluginClassLoader";
    private static final String HUB_LOADER = "net.runelite.client.externalplugins.PluginHubClassLoader";
    private static final String RUNELITE_PACKAGE = "net.runelite.client.";
    private static final String PLUGIN_PACKAGE = "net.runelite.client.plugins.";

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final String STREAMER_PACKAGE = "com.tonic.services.streamer.";
    static final String NOTIFIER = "net.runelite.client.Notifier";

    private static volatile Set<String> developerPackages;

    private static final ClassValue<Boolean> STOCK_CLASSES = new ClassValue<>()
    {
        @Override
        protected Boolean computeValue(Class<?> type)
        {
            ClassLoader loader = type.getClassLoader();
            String loaderName = loader == null ? "" : loader.getClass().getName();
            if (loaderName.equals(VITA_LOADER))
            {
                return false;
            }
            return loaderName.equals(HUB_LOADER) || type.getName().startsWith(RUNELITE_PACKAGE);
        }
    };

    private static final ClassValue<Boolean> STOCK_PLUGINS = new ClassValue<>()
    {
        @Override
        protected Boolean computeValue(Class<?> type)
        {
            PluginDescriptor descriptor = type.getAnnotation(PluginDescriptor.class);
            return (descriptor == null || !descriptor.developerPlugin()) && STOCK_CLASSES.get(type) && !inDeveloperPlugin(type);
        }
    };

    private StockContent()
    {
    }

    /**
     * @param skippedPrefix the start of the class names of the stock code being called, whose own frames are passed over
     * @return whether the first caller outside that code and the streamer mirror belongs to stock RuneLite, true when there is none
     */
    static boolean callerIsStock(String skippedPrefix)
    {
        Class<?> caller = caller(type -> type.getName().startsWith(skippedPrefix));
        return caller == null || isStock(caller);
    }

    /**
     * @param skipped which classes to pass over, besides the streamer mirror's own
     * @return the class of the first caller not passed over, or null when there is none
     */
    static Class<?> caller(Predicate<Class<?>> skipped)
    {
        return WALKER.walk(frames -> frames
                .map(StackWalker.StackFrame::getDeclaringClass)
                .filter(type -> !type.getName().startsWith(STREAMER_PACKAGE) && !skipped.test(type))
                .findFirst()
                .orElse(null));
    }

    /**
     * @param type a class
     * @return whether it belongs to stock RuneLite
     */
    public static boolean isStock(Class<?> type)
    {
        return STOCK_CLASSES.get(type) && !inDeveloperPlugin(type);
    }

    /**
     * @param plugin a plugin
     * @return whether it is a stock RuneLite plugin that a normal RuneLite user could run, so not a developer plugin
     */
    public static boolean isStock(Plugin plugin)
    {
        return STOCK_PLUGINS.get(plugin.getClass());
    }

    /**
     * @param overlay an overlay
     * @return whether both the overlay and the plugin that owns it, if any, are stock
     */
    public static boolean isStock(Overlay overlay)
    {
        Plugin plugin = overlay.getPlugin();
        return (plugin == null || isStock(plugin)) && isStock(overlay.getClass());
    }

    /**
     * @param button a sidebar or title bar button
     * @return whether the panel it opens, or what it runs when it has no panel, is stock
     */
    public static boolean isStock(NavigationButton button)
    {
        if (BlockedPlugins.isStandIn(button))
        {
            return true;
        }
        Object owner = button.getPanel() != null ? button.getPanel() : button.getOnClick();
        return owner == null || isStock(owner.getClass());
    }

    private static boolean inDeveloperPlugin(Class<?> type)
    {
        String name = type.getName();
        if (!name.startsWith(PLUGIN_PACKAGE))
        {
            return false;
        }
        for (String developerPackage : developerPackages())
        {
            if (name.startsWith(developerPackage))
            {
                return true;
            }
        }
        return false;
    }

    private static Set<String> developerPackages()
    {
        Set<String> known = developerPackages;
        if (known != null)
        {
            return known;
        }
        Set<String> found = new HashSet<>();
        try
        {
            Collection<Plugin> plugins = Static.getInjector().getInstance(PluginManager.class).getPlugins();
            for (Plugin plugin : plugins)
            {
                PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
                if (descriptor != null && descriptor.developerPlugin())
                {
                    found.add(plugin.getClass().getPackageName() + ".");
                }
            }
            if (!plugins.isEmpty())
            {
                developerPackages = found;
            }
        }
        catch (RuntimeException e)
        {
            return found;
        }
        return found;
    }
}
