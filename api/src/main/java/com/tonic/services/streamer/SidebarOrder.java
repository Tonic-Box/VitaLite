package com.tonic.services.streamer;

import com.tonic.Logger;
import com.tonic.Static;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.NavigationButton;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Orders RuneLite's sidebar tabs with the stock ones first, its title bar buttons with the stock ones last, and its plugin list with the stock rows first, while streamer mode is on, so they sit where the mirror shows them, and back in RuneLite's own order after. Runs on the event dispatch thread only.
 */
public final class SidebarOrder
{
    private static final String PLUGIN_LIST = "net.runelite.client.plugins.config.PluginListPanel";

    private static Comparator<? super NavigationButton> runeLiteOrder;
    private static Comparator<NavigationButton> stockFirst;
    private static Comparator<? super NavigationButton> runeLiteButtonOrder;
    private static Comparator<NavigationButton> stockLast;

    private static final Field SIDEBAR = Members.inherited(ClientUI.class, "sidebar");
    private static final Field SIDEBAR_ENTRIES = Members.inherited(ClientUI.class, "sidebarEntries");
    private static final Field SELECTED_TAB = Members.inherited(ClientUI.class, "selectedTab");
    private static final Field TOOLBAR = Members.inherited(ClientUI.class, "toolbarPanel");
    private static Field toolbarEntries;

    private SidebarOrder()
    {
    }

    /**
     * @param clientUI RuneLite's window
     * @return its sidebar
     */
    static JTabbedPane sidebar(ClientUI clientUI)
    {
        return (JTabbedPane) read(SIDEBAR, clientUI);
    }

    /**
     * @param clientUI RuneLite's window
     * @return its sidebar tabs, in their order
     */
    @SuppressWarnings("unchecked")
    static TreeSet<NavigationButton> tabs(ClientUI clientUI)
    {
        return (TreeSet<NavigationButton>) read(SIDEBAR_ENTRIES, clientUI);
    }

    /**
     * @param clientUI RuneLite's window
     * @return its title bar button panel
     */
    static Object toolbar(ClientUI clientUI)
    {
        return read(TOOLBAR, clientUI);
    }

    /**
     * @param toolbar the title bar button panel
     * @return its buttons with their components, in their order
     */
    @SuppressWarnings("unchecked")
    static TreeMap<NavigationButton, Component> buttons(Object toolbar)
    {
        if (toolbarEntries == null)
        {
            toolbarEntries = Members.inherited(toolbar.getClass(), "entries");
        }
        return (TreeMap<NavigationButton, Component>) read(toolbarEntries, toolbar);
    }

    private static Object read(Field field, Object owner)
    {
        if (field == null)
        {
            throw new IllegalStateException(owner.getClass().getName() + " no longer has a field the streamer mirror reads");
        }
        try
        {
            return field.get(owner);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Orders the sidebar's tabs and the title bar's buttons.
     *
     * @param streaming whether to put the stock ones where the mirror shows them, rather than in RuneLite's own order
     */
    static void apply(boolean streaming)
    {
        ClientUI clientUI = Static.getInjector().getInstance(ClientUI.class);
        orderTabs(clientUI, streaming);
        orderButtons(clientUI, streaming);
        orderPluginList(streaming);
    }

    /**
     * Orders the plugin list as the mirror shows it: the stock rows and the stand-ins for the plugins VitaLite never loads in RuneLite's order, then the others in RuneLite's order. Called by RuneLite's plugin list in place of its search while streamer mode is on.
     *
     * @param rows the plugin list's rows
     * @param query the search text
     * @return the rows to show, in order
     */
    public static List<Object> pluginRows(Collection<?> rows, String query)
    {
        List<Object> stock = new ArrayList<>();
        List<Object> others = new ArrayList<>();
        for (Object row : rows)
        {
            (MirrorConfiguration.isStockRow((Component) row) ? stock : others).add(row);
        }
        try
        {
            BlockedPlugins.removeRealRows();
            stock.addAll(BlockedPlugins.realRows(pluginList()));
            List<Object> ordered = new ArrayList<>(MirrorConfiguration.search(stock, query));
            ordered.addAll(MirrorConfiguration.search(others, query));
            return ordered;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            Logger.error("Streamer mode could not order the plugin list: " + e);
            return new ArrayList<>(rows);
        }
    }

    private static void orderPluginList(boolean streaming)
    {
        try
        {
            Object pluginList = pluginList();
            if (!streaming)
            {
                BlockedPlugins.removeRealRows();
            }
            Members.method(pluginList.getClass(), "onSearchBarChanged").invoke(pluginList);
            ((Component) pluginList).repaint();
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            Logger.error("Streamer mode could not reorder the plugin list: " + e);
        }
    }

    private static Object pluginList() throws ClassNotFoundException
    {
        return Static.getInjector().getInstance(Class.forName(PLUGIN_LIST, false, ClientUI.class.getClassLoader()));
    }

    private static void orderTabs(ClientUI clientUI, boolean stockFirstOrder)
    {
        try
        {
            TreeSet<NavigationButton> entries = tabs(clientUI);
            JTabbedPane sidebar = sidebar(clientUI);
            if (entries == null || sidebar == null)
            {
                return;
            }
            if (runeLiteOrder == null)
            {
                runeLiteOrder = entries.comparator();
                stockFirst = Comparator.<NavigationButton, Boolean>comparing(button -> !StockContent.isStock(button)).thenComparing(runeLiteOrder);
            }
            TreeSet<NavigationButton> ordered = new TreeSet<>(stockFirstOrder ? stockFirst : runeLiteOrder);
            ordered.addAll(entries);
            if (new ArrayList<>(ordered).equals(new ArrayList<>(entries)))
            {
                SIDEBAR_ENTRIES.set(clientUI, ordered);
                return;
            }

            Map<NavigationButton, Tab> tabs = new HashMap<>();
            int index = 0;
            for (NavigationButton button : entries)
            {
                if (index < sidebar.getTabCount())
                {
                    tabs.put(button, new Tab(sidebar.getComponentAt(index), sidebar.getIconAt(index), sidebar.getToolTipTextAt(index)));
                }
                index++;
            }
            NavigationButton selected = (NavigationButton) read(SELECTED_TAB, clientUI);

            List<ChangeListener> clientListeners = new ArrayList<>();
            for (ChangeListener listener : sidebar.getChangeListeners())
            {
                if (listener.getClass().getName().startsWith(ClientUI.class.getName()))
                {
                    clientListeners.add(listener);
                    sidebar.removeChangeListener(listener);
                }
            }
            try
            {
                SIDEBAR_ENTRIES.set(clientUI, ordered);
                sidebar.removeAll();
                int position = 0;
                int selectedIndex = -1;
                for (NavigationButton button : ordered)
                {
                    Tab tab = tabs.get(button);
                    if (tab == null)
                    {
                        continue;
                    }
                    sidebar.insertTab(null, tab.icon, tab.component, tab.tooltip, position);
                    if (button == selected)
                    {
                        selectedIndex = position;
                    }
                    position++;
                }
                sidebar.setSelectedIndex(selectedIndex);
            }
            finally
            {
                clientListeners.forEach(sidebar::addChangeListener);
            }
            sidebar.revalidate();
            sidebar.repaint();
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            Logger.error("Streamer mode could not reorder the sidebar: " + e);
        }
    }

    private static void orderButtons(ClientUI clientUI, boolean stockLastOrder)
    {
        try
        {
            Object toolbar = toolbar(clientUI);
            if (!(toolbar instanceof JComponent))
            {
                return;
            }
            TreeMap<NavigationButton, Component> entries = buttons(toolbar);
            if (entries == null)
            {
                return;
            }
            if (runeLiteButtonOrder == null)
            {
                runeLiteButtonOrder = entries.comparator();
                stockLast = Comparator.<NavigationButton, Boolean>comparing(StockContent::isStock).thenComparing(runeLiteButtonOrder);
            }
            TreeMap<NavigationButton, Component> ordered = new TreeMap<>(stockLastOrder ? stockLast : runeLiteButtonOrder);
            ordered.putAll(entries);
            toolbarEntries.set(toolbar, ordered);
            if (new ArrayList<>(ordered.keySet()).equals(new ArrayList<>(entries.keySet())))
            {
                return;
            }

            JComponent panel = (JComponent) toolbar;
            panel.removeAll();
            ordered.values().forEach(panel::add);
            panel.setMaximumSize(panel.getPreferredSize());
            panel.revalidate();
            panel.repaint();
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            Logger.error("Streamer mode could not reorder the title bar buttons: " + e);
        }
    }

    private static final class Tab
    {
        private final Component component;
        private final Icon icon;
        private final String tooltip;

        private Tab(Component component, Icon icon, String tooltip)
        {
            this.component = component;
            this.icon = icon;
            this.tooltip = tooltip;
        }
    }
}
