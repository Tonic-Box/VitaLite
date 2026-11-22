package com.tonic.services.streamer;

import com.formdev.flatlaf.util.DerivedColor;
import com.tonic.Logger;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.laf.RuneLiteLAF;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stock RuneLite's look for the streamer mirror: the look-and-feel colours VitaLite restyled switch to stock while the mirror prints, and components VitaLite added or restyled are marked so the mirror prints them as stock RuneLite would. Prints run on the event dispatch thread.
 */
public final class MirrorLook
{
    /**
     * Client property marking a component the mirror leaves out.
     */
    public static final String HIDDEN = "streamer.mirrorHidden";

    /**
     * Client property naming the component whose preferred size a container takes in the mirror.
     */
    public static final String SIZE_OF = "streamer.mirrorSizeOf";

    /**
     * Client property marking a button the mirror prints with its look-and-feel icons.
     */
    public static final String STOCK_ICONS = "streamer.mirrorStockIcons";

    private static boolean building;

    private MirrorLook()
    {
    }

    /**
     * Has every look-and-feel colour VitaLite's accent changed show its stock value while the mirror prints. Called by RuneLite's look and feel as it builds its defaults, before any component uses them.
     *
     * @param real the defaults just built
     * @return the same defaults
     */
    public static UIDefaults defaults(UIDefaults real)
    {
        if (building || !(ColorScheme.BRAND_ORANGE instanceof MirrorColor))
        {
            return real;
        }
        building = true;
        try
        {
            Color stockBrand = ((MirrorColor) ColorScheme.BRAND_ORANGE).stock();
            RuneLiteLAF stockLaf = new RuneLiteLAF();
            Map<String, String> extras = new HashMap<>(stockLaf.getExtraDefaults());
            extras.put("@BRAND_ORANGE", String.format("#%06x%02x", stockBrand.getRGB() & 0xFFFFFF, stockBrand.getRGB() >>> 24));
            stockLaf.setExtraDefaults(extras);
            Map<Object, Object> stock = new HashMap<>(stockLaf.getDefaults());
            Map<Object, Color> switched = new HashMap<>();
            for (Map.Entry<Object, Object> entry : new ArrayList<>(real.entrySet()))
            {
                Object value = entry.getValue();
                Object stockValue = stock.get(entry.getKey());
                if (value instanceof Color && stockValue instanceof Color && !(value instanceof DerivedColor) && !(value instanceof MirrorColor.Resource) && ((Color) value).getRGB() != ((Color) stockValue).getRGB())
                {
                    switched.put(entry.getKey(), new MirrorColor.Resource((Color) value, (Color) stockValue));
                }
            }
            real.putAll(switched);
        }
        catch (RuntimeException e)
        {
            Logger.error("Streamer mirror could not prepare the stock look: " + e);
        }
        finally
        {
            building = false;
        }
        return real;
    }

    /**
     * Lays a component tree out as stock RuneLite shows it, for one print.
     *
     * @param root the tree
     * @param restore where to add what puts each change back, run last to first
     * @param skip the parts of the tree that are not printed, left as they are with everything in them
     */
    static void apply(Container root, List<Runnable> restore, Predicate<Component> skip)
    {
        Set<Container> changed = new LinkedHashSet<>();
        restore.add(() -> changed.forEach(MirrorPrint::layOut));
        visit(root, restore, changed, skip);
        changed.forEach(MirrorPrint::layOut);
    }

    private static void visit(Component component, List<Runnable> restore, Set<Container> changed, Predicate<Component> skip)
    {
        if (skip.test(component))
        {
            return;
        }
        if (component instanceof JComponent)
        {
            JComponent marked = (JComponent) component;
            if (Boolean.TRUE.equals(marked.getClientProperty(HIDDEN)) && marked.isVisible())
            {
                marked.setVisible(false);
                restore.add(() -> marked.setVisible(true));
                addParent(marked, changed);
            }
            Object sizeOf = marked.getClientProperty(SIZE_OF);
            if (sizeOf instanceof Component)
            {
                Dimension set = marked.isPreferredSizeSet() ? marked.getPreferredSize() : null;
                marked.setPreferredSize(((Component) sizeOf).getPreferredSize());
                restore.add(() -> marked.setPreferredSize(set));
                addParent(marked, changed);
            }
            if (Boolean.TRUE.equals(marked.getClientProperty(STOCK_ICONS)) && marked instanceof AbstractButton)
            {
                AbstractButton button = (AbstractButton) marked;
                Icon icon = button.getIcon();
                Icon selected = button.getSelectedIcon();
                button.setIcon(null);
                button.setSelectedIcon(null);
                restore.add(() ->
                {
                    button.setIcon(icon);
                    button.setSelectedIcon(selected);
                });
                addParent(button, changed);
            }
        }
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                visit(child, restore, changed, skip);
            }
        }
    }

    private static void addParent(Component component, Set<Container> changed)
    {
        Container parent = component.getParent();
        if (parent != null)
        {
            changed.add(parent.getParent() != null ? parent.getParent() : parent);
        }
    }
}
