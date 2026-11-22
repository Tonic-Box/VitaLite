package com.tonic.services.streamer;

import com.tonic.Logger;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.materialtabs.MaterialTab;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Prints RuneLite's Configuration panel as the mirror shows it: without the Vita Hub tab, without plugins that are not stock, with the plugin list in place of any panel that is not stock, and hovered where the mirror's pointer is. The real panel is laid out and hovered that way only for the moment it is printed and put back before it can paint. Runs on the event dispatch thread only.
 */
final class MirrorConfiguration
{
    private static final String CONFIGURATION_PANEL = "net.runelite.client.plugins.config.TopLevelConfigPanel";
    private static final String VITA_HUB_TAB = "Vita Hub";
    private static final int VIEW_MARGIN = 64;
    private static final ClassValue<Optional<Method>> PLUGIN_CONFIG_GETTER = publicGetter("getPluginConfig");
    private static final ClassValue<Optional<Method>> PLUGIN_GETTER = publicGetter("getPlugin");

    private static Field tabGroupField;
    private static Field contentField;
    private static Field pluginListField;
    private static Field configurationTabField;
    private static Field currentField;
    private static Field muxerCurrentField;
    private static Field mainPanelField;
    private static Field configPluginField;
    private static Method muxerGetter;
    private static Field searchBarField;
    private static Method searchMethod;
    private static boolean broken;
    private static Component realEntered;
    private static final AWTEventListener ENTER_TRACKER = event ->
    {
        MouseEvent mouse = (MouseEvent) event;
        if (mouse.getID() == MouseEvent.MOUSE_ENTERED)
        {
            realEntered = mouse.getComponent();
        }
        else if (mouse.getID() == MouseEvent.MOUSE_EXITED && mouse.getComponent() == realEntered)
        {
            realEntered = null;
        }
    };
    private static boolean printFailed;

    private MirrorConfiguration()
    {
    }

    /**
     * Starts following which component Swing last sent the mouse into, the one whose hover the print sets aside, as the mirror opens. The pointer's position cannot tell: Swing moves hover only when the mouse moves, not when the list scrolls under it.
     */
    static void startTracking()
    {
        Toolkit.getDefaultToolkit().addAWTEventListener(ENTER_TRACKER, AWTEvent.MOUSE_EVENT_MASK);
    }

    /**
     * Stops following the mouse, as the mirror closes.
     */
    static void stopTracking()
    {
        Toolkit.getDefaultToolkit().removeAWTEventListener(ENTER_TRACKER);
        realEntered = null;
    }

    /**
     * @param panel a sidebar panel
     * @return whether it is RuneLite's Configuration panel
     */
    static boolean isConfiguration(PluginPanel panel)
    {
        return panel.getClass().getName().equals(CONFIGURATION_PANEL);
    }

    /**
     * Prints the Configuration panel as the mirror shows it, with the hover highlight on whatever lies under the mirror's pointer. Prints nothing when the panel cannot be filtered, so the mirror never shows it unfiltered.
     *
     * @param panel the Configuration panel, laid out at its size
     * @param graphics graphics at the panel's top left
     * @param pointer the mirror's pointer in the panel's coordinates, or null when it is elsewhere
     * @return whether the panel was printed
     */
    static boolean print(PluginPanel panel, Graphics2D graphics, Point pointer)
    {
        if (!resolve(panel.getClass()))
        {
            return false;
        }
        List<Runnable> restore = new ArrayList<>();
        try
        {
            Container tabGroup = (Container) tabGroupField.get(panel);
            JComponent content = (JComponent) contentField.get(panel);
            PluginPanel pluginList = (PluginPanel) pluginListField.get(panel);
            MaterialTab configurationTab = (MaterialTab) configurationTabField.get(panel);
            PluginPanel current = (PluginPanel) currentField.get(panel);
            PluginPanel muxer = (PluginPanel) muxerGetter.invoke(pluginList);

            boolean currentHidden = current != null && !StockContent.isStock(current.getClass());
            PluginPanel shown = currentHidden ? muxer : current;
            if (shown == muxer && !isStock((PluginPanel) muxerCurrentField.get(muxer)))
            {
                shown = pluginList;
            }
            Rectangle area = content.getBounds();
            if (shown != current)
            {
                MirrorPrint.fit(shown.getWrappedPanel(), area);
            }
            Component realHover = realEntered != null && SwingUtilities.isDescendingFrom(realEntered, panel) ? realEntered : null;

            Container mainPanel = (Container) mainPanelField.get(pluginList);
            boolean listShown = shown == pluginList || (shown == muxer && muxerCurrentField.get(muxer) == pluginList);
            Component printed = shown != current ? shown.getWrappedPanel() : panel;
            Point origin = shown != current ? area.getLocation() : new Point();
            Point printedPointer = pointer == null ? null : new Point(pointer.x - origin.x, pointer.y - origin.y);

            Arrangement arrangement = planPluginList(pluginList, mainPanel);
            Container viewport = listShown ? SwingUtilities.getAncestorOfClass(JViewport.class, mainPanel) : null;
            Rectangle inView = viewIn(viewport, mainPanel);
            MirrorLook.apply(panel, restore, component -> component.getParent() == mainPanel && !arrangement.shows(component, listShown, inView));
            hideHubTab(tabGroup, configurationTab, currentHidden, restore);
            List<JComponent> standIns = arrangement.apply(mainPanel, restore);
            Rectangle view = visibleList(viewport, printed);

            Component mirrorHover = view == null ? null : standInHover(standIns, mainPanel, printed, view, printedPointer);
            if (mirrorHover == null)
            {
                if (shown != current && pointer != null && area.contains(pointer))
                {
                    mirrorHover = hoverTarget(shown.getWrappedPanel(), printedPointer);
                }
                else
                {
                    mirrorHover = hoverTarget(panel, pointer);
                }
            }
            Component mirrorHovered = mirrorHover;
            if (mirrorHovered != realHover)
            {
                hover(realHover, MouseEvent.MOUSE_EXITED);
                hover(mirrorHovered, MouseEvent.MOUSE_ENTERED);
                restore.add(() ->
                {
                    hover(mirrorHovered, MouseEvent.MOUSE_EXITED);
                    hover(realHover, MouseEvent.MOUSE_ENTERED);
                });
            }

            MirrorPrint.printAll(panel, graphics);
            if (shown != current)
            {
                printInstead(graphics, shown.getWrappedPanel(), area);
            }
            if (view != null)
            {
                printStandIns(graphics, standIns, mainPanel, printed, origin, view);
            }
            return true;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            if (!printFailed)
            {
                printFailed = true;
                Logger.error("Streamer mirror could not filter the Configuration panel: " + e);
            }
            return false;
        }
        finally
        {
            for (int i = restore.size() - 1; i >= 0; i--)
            {
                restore.get(i).run();
            }
        }
    }

    private static void hideHubTab(Container tabGroup, MaterialTab configurationTab, boolean currentHidden, List<Runnable> restore)
    {
        Component hub = null;
        MaterialTab selected = null;
        List<Component> kept = new ArrayList<>();
        for (Component tab : tabGroup.getComponents())
        {
            if (tab instanceof MaterialTab && ((MaterialTab) tab).isSelected())
            {
                selected = (MaterialTab) tab;
            }
            if (tab instanceof MaterialTab && VITA_HUB_TAB.equals(((MaterialTab) tab).getToolTipText()))
            {
                hub = tab;
            }
            else
            {
                kept.add(tab);
            }
        }
        if (hub == null || !(tabGroup.getLayout() instanceof GridLayout))
        {
            return;
        }

        for (Component tab : tabGroup.getComponents())
        {
            Rectangle bounds = tab.getBounds();
            restore.add(() -> tab.setBounds(bounds));
        }
        hub.setBounds(0, 0, 0, 0);

        GridLayout grid = (GridLayout) tabGroup.getLayout();
        Insets insets = tabGroup.getInsets();
        int columns = kept.size();
        int gaps = (columns - 1) * grid.getHgap();
        int width = tabGroup.getWidth() - insets.left - insets.right;
        int height = tabGroup.getHeight() - insets.top - insets.bottom;
        int tabWidth = (width - gaps) / columns;
        int x = insets.left + (width - (tabWidth * columns + gaps)) / 2;
        for (Component tab : kept)
        {
            tab.setBounds(x, insets.top, tabWidth, height);
            x += tabWidth + grid.getHgap();
        }

        if (currentHidden && selected != null && selected != configurationTab)
        {
            Border border = configurationTab.getBorder();
            Color foreground = configurationTab.getForeground();
            configurationTab.setBorder(selected.getBorder());
            configurationTab.setForeground(selected.getForeground());
            restore.add(() ->
            {
                configurationTab.setBorder(border);
                configurationTab.setForeground(foreground);
            });
        }
    }

    private static Component hoverTarget(Component root, Point point)
    {
        if (point == null || !root.contains(point))
        {
            return null;
        }
        Component target = SwingUtilities.getDeepestComponentAt(root, point.x, point.y);
        while (target != null)
        {
            for (MouseListener listener : target.getMouseListeners())
            {
                if (!(listener instanceof ToolTipManager))
                {
                    return target;
                }
            }
            if (target == root)
            {
                return null;
            }
            target = target.getParent();
        }
        return null;
    }

    private static void hover(Component target, int id)
    {
        if (target == null)
        {
            return;
        }
        MouseEvent event = new MouseEvent(target, id, System.currentTimeMillis(), 0, 0, 0, 0, false);
        for (MouseListener listener : target.getMouseListeners())
        {
            if (listener instanceof ToolTipManager)
            {
                continue;
            }
            if (id == MouseEvent.MOUSE_ENTERED)
            {
                listener.mouseEntered(event);
            }
            else
            {
                listener.mouseExited(event);
            }
        }
    }

    private static Arrangement planPluginList(PluginPanel pluginList, Container mainPanel) throws ReflectiveOperationException
    {
        Arrangement arrangement = new Arrangement();
        if (!(mainPanel.getLayout() instanceof GridLayout))
        {
            return arrangement;
        }
        int gap = ((GridLayout) mainPanel.getLayout()).getVgap();
        Insets insets = mainPanel.getInsets();
        List<Object> rows = new ArrayList<>();
        for (Component row : mainPanel.getComponents())
        {
            if (isStockRow(row) && !BlockedPlugins.isRealRow(row))
            {
                rows.add(row);
                arrangement.stock.add(row);
            }
        }
        List<Object> standIns = BlockedPlugins.rows(pluginList);
        rows.addAll(standIns);
        String text = ((IconTextField) searchBarField.get(pluginList)).getText();
        int y = insets.top;
        for (Object entry : search(rows, text))
        {
            Component row = (Component) entry;
            int height = standIns.contains(row) ? row.getPreferredSize().height : row.getHeight();
            arrangement.ordered.add(row);
            arrangement.planned.put(row, new Rectangle(0, y, 1, height));
            y += height + gap;
        }
        arrangement.end = y - gap;
        arrangement.standIns = standIns;
        arrangement.grid = true;
        return arrangement;
    }

    private static Rectangle viewIn(Container viewport, Container mainPanel)
    {
        if (viewport == null)
        {
            return null;
        }
        Rectangle view = SwingUtilities.convertRectangle(viewport, new Rectangle(viewport.getSize()), mainPanel);
        view.grow(0, VIEW_MARGIN);
        return view;
    }

    private static Rectangle visibleList(Container viewport, Component printed)
    {
        if (viewport == null || viewport.getParent() == null)
        {
            return null;
        }
        return SwingUtilities.convertRectangle(viewport.getParent(), viewport.getBounds(), printed);
    }

    private static Component standInHover(List<JComponent> standIns, Container mainPanel, Component printed, Rectangle view, Point pointer)
    {
        if (pointer == null || !view.contains(pointer))
        {
            return null;
        }
        Point inList = SwingUtilities.convertPoint(printed, pointer, mainPanel);
        for (JComponent row : standIns)
        {
            if (row.getBounds().contains(inList))
            {
                return hoverTarget(row, new Point(inList.x - row.getX(), inList.y - row.getY()));
            }
        }
        return null;
    }

    private static void printStandIns(Graphics2D graphics, List<JComponent> standIns, Container mainPanel, Component printed, Point origin, Rectangle view)
    {
        for (JComponent row : standIns)
        {
            Point at = SwingUtilities.convertPoint(mainPanel, row.getLocation(), printed);
            Graphics2D rowGraphics = (Graphics2D) graphics.create();
            try
            {
                rowGraphics.clipRect(origin.x + view.x, origin.y + view.y, view.width, view.height);
                rowGraphics.translate(origin.x + at.x, origin.y + at.y);
                MirrorPrint.printAll(row, rowGraphics);
            }
            finally
            {
                rowGraphics.dispose();
            }
        }
    }

    private static void printInstead(Graphics2D graphics, JComponent wrapped, Rectangle area)
    {
        Graphics2D at = (Graphics2D) graphics.create(area.x, area.y, area.width, area.height);
        try
        {
            MirrorPrint.run(() ->
            {
                at.setColor(wrapped.getBackground());
                at.fillRect(0, 0, area.width, area.height);
                wrapped.printAll(at);
            });
        }
        finally
        {
            at.dispose();
        }
    }

    /**
     * The plugin list as the mirror shows it: its stock rows and the stand-in rows in RuneLite's search order, worked out before any of it is moved.
     */
    private static final class Arrangement
    {
        private final Set<Component> stock = Collections.newSetFromMap(new IdentityHashMap<>());
        private final List<Component> ordered = new ArrayList<>();
        private final Map<Component, Rectangle> planned = new IdentityHashMap<>();
        private List<Object> standIns = List.of();
        private boolean grid;
        private int end;

        private boolean shows(Component row, boolean listShown, Rectangle view)
        {
            if (!listShown)
            {
                return false;
            }
            if (!grid)
            {
                return true;
            }
            if (!stock.contains(row))
            {
                return false;
            }
            Rectangle target = planned.get(row);
            return target == null || view == null || target.y < view.y + view.height && target.y + target.height > view.y;
        }

        private List<JComponent> apply(Container mainPanel, List<Runnable> restore)
        {
            if (!grid)
            {
                return List.of();
            }
            Insets insets = mainPanel.getInsets();
            Rectangle first = null;
            for (Component row : mainPanel.getComponents())
            {
                Rectangle bounds = row.getBounds();
                restore.add(() -> row.setBounds(bounds));
                if (first == null)
                {
                    first = bounds;
                }
                if (!stock.contains(row))
                {
                    row.setBounds(0, 0, 0, 0);
                }
            }

            int x = first != null ? first.x : insets.left;
            int width = first != null ? first.width : mainPanel.getWidth() - insets.left - insets.right;
            List<JComponent> placed = new ArrayList<>();
            for (Component row : ordered)
            {
                Rectangle target = planned.get(row);
                row.setBounds(x, target.y, width, target.height);
                if (standIns.contains(row))
                {
                    MirrorPrint.layOut((Container) row);
                    placed.add((JComponent) row);
                }
            }

            int needed = end + insets.bottom;
            if (needed > mainPanel.getHeight())
            {
                Dimension size = mainPanel.getSize();
                mainPanel.setSize(size.width, needed);
                restore.add(() -> mainPanel.setSize(size));
                Container parent = mainPanel.getParent();
                if (parent != null && mainPanel.getY() + needed > parent.getHeight())
                {
                    Dimension parentSize = parent.getSize();
                    parent.setSize(parentSize.width, mainPanel.getY() + needed);
                    restore.add(() -> parent.setSize(parentSize));
                }
            }
            return placed;
        }
    }

    private static boolean isStock(PluginPanel panel)
    {
        if (panel == null)
        {
            return true;
        }
        if (!StockContent.isStock(panel.getClass()))
        {
            return false;
        }
        Plugin plugin = pluginOf(panel, configPluginField);
        return plugin == null || StockContent.isStock(plugin);
    }

    /**
     * Filters and sorts plugin list rows as RuneLite's plugin search does.
     *
     * @param rows the rows
     * @param query the search text
     * @return the rows that match, in RuneLite's order
     * @throws IllegalStateException if RuneLite's plugin search cannot be called
     */
    static List<?> search(Collection<?> rows, String query)
    {
        try
        {
            if (searchMethod == null)
            {
                searchMethod = Members.method(Class.forName("net.runelite.client.plugins.config.PluginSearch", false, ClientUI.class.getClassLoader()), "search", Collection.class, String.class);
            }
            return (List<?>) searchMethod.invoke(null, rows, query);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("RuneLite's plugin search could not be called: " + e, e);
        }
    }

    /**
     * @param row a plugin list row
     * @return whether its plugin is stock, or it has none, as RuneLite's own settings entries
     */
    static boolean isStockRow(Component row)
    {
        Plugin plugin = pluginOf(row, null);
        return plugin == null || StockContent.isStock(plugin);
    }

    private static Plugin pluginOf(Object owner, Field descriptorField)
    {
        try
        {
            Object descriptor;
            if (descriptorField != null)
            {
                if (!descriptorField.getDeclaringClass().isInstance(owner))
                {
                    return null;
                }
                descriptor = descriptorField.get(owner);
            }
            else
            {
                Method getter = PLUGIN_CONFIG_GETTER.get(owner.getClass()).orElse(null);
                if (getter == null)
                {
                    return null;
                }
                descriptor = getter.invoke(owner);
            }
            if (descriptor == null)
            {
                return null;
            }
            Method plugin = PLUGIN_GETTER.get(descriptor.getClass()).orElse(null);
            return plugin == null ? null : (Plugin) plugin.invoke(descriptor);
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    private static ClassValue<Optional<Method>> publicGetter(String name)
    {
        return new ClassValue<>()
        {
            @Override
            protected Optional<Method> computeValue(Class<?> type)
            {
                try
                {
                    return Optional.of(Members.accessible(type.getMethod(name)));
                }
                catch (NoSuchMethodException e)
                {
                    return Optional.empty();
                }
            }
        };
    }

    private static boolean resolve(Class<?> configurationPanel)
    {
        if (broken)
        {
            return false;
        }
        if (configPluginField != null)
        {
            return true;
        }
        try
        {
            tabGroupField = Members.field(configurationPanel, "tabGroup");
            contentField = Members.field(configurationPanel, "content");
            pluginListField = Members.field(configurationPanel, "pluginListPanel");
            configurationTabField = Members.field(configurationPanel, "pluginListPanelTab");
            currentField = Members.field(configurationPanel, "current");
            Class<?> pluginList = pluginListField.getType();
            mainPanelField = Members.field(pluginList, "mainPanel");
            searchBarField = Members.field(pluginList, "searchBar");
            muxerGetter = Members.accessible(pluginList.getMethod("getMuxer"));
            Class<?> muxer = muxerGetter.getReturnType();
            muxerCurrentField = Members.field(muxer, "current");
            Class<?> configPanel = Class.forName("net.runelite.client.plugins.config.ConfigPanel", false, configurationPanel.getClassLoader());
            configPluginField = Members.field(configPanel, "pluginConfig");
            return true;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            broken = true;
            Logger.error("Streamer mirror could not find the Configuration panel's parts: " + e);
            return false;
        }
    }
}
