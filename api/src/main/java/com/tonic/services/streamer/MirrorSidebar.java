package com.tonic.services.streamer;

import com.google.common.collect.Iterables;
import com.tonic.services.profiler.frames.FrameProfiler;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.TabbedPaneUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The mirror's sidebar and title bar buttons: a hidden copy of RuneLite's tab strip holding only stock tabs, with the real selected panel painted into it, or the Configuration panel while the real one is not stock, and look-alike title bar buttons for the stock ones. Icons and selection follow RuneLite's own, and hover follows the mirrored pointer, since the mirror's tabs and buttons sit in different places than VitaLite's.
 */
final class MirrorSidebar
{
    private static final String SIDEBAR_STYLE = "tabInsets: 2,5,2,5; variableSize: true; deselectable: true; tabHeight: 26";
    private static final int BUTTON_SIZE = 23;
    private static final int PAINT = FrameProfiler.timer("mirror.edtSidebar");

    private static Method rolloverSetter;

    private final JTabbedPane realSidebar;
    private final ClientUI clientUI;
    private final Object toolbarPanel;
    private final JTabbedPane sidebar = new JTabbedPane(SwingConstants.RIGHT);
    private final JPanel toolbar = new JPanel();
    private final Map<NavigationButton, AbstractButton> realButtons = new IdentityHashMap<>();
    private final List<JButton> copies = new ArrayList<>();
    private List<NavigationButton> tabs = List.of();
    private List<NavigationButton> buttons = List.of();
    private BufferedImage image;
    private BufferedImage spare;

    MirrorSidebar(ClientUI clientUI, boolean buttonsInTitleBar)
    {
        realSidebar = SidebarOrder.sidebar(clientUI);
        this.clientUI = clientUI;
        toolbarPanel = SidebarOrder.toolbar(clientUI);

        sidebar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        sidebar.setOpaque(true);
        sidebar.putClientProperty("FlatLaf.style", SIDEBAR_STYLE);
        toolbar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        toolbar.setLayout(buttonsInTitleBar ? new DynamicGridLayout(1, 0, 4, 0) : new DynamicGridLayout(0, 1, 0, 4));
        if (!buttonsInTitleBar)
        {
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            wrap.add(toolbar, BorderLayout.SOUTH);
            sidebar.putClientProperty("JTabbedPane.trailingComponent", wrap);
        }
    }

    /**
     * @return the panel of look-alike title bar buttons
     */
    JPanel toolbar()
    {
        return toolbar;
    }

    /**
     * @return the sidebar as last painted, or null while RuneLite's sidebar is closed
     */
    BufferedImage image()
    {
        return image;
    }

    /**
     * @return where RuneLite's sidebar sits in its window's content
     */
    Rectangle bounds()
    {
        return realSidebar.getBounds();
    }

    /**
     * Brings the title bar buttons and tabs up to date with RuneLite's and paints the sidebar, highlighting whatever lies under the mirrored pointer. Called on the event dispatch thread.
     *
     * @param contentPointer the pointer in RuneLite's content, which the mirror lays out the same way, or null when it is not over it
     * @param titlePointer the pointer where it falls on the mirror's title bar, in the title space's coordinates, or null when it is not over RuneLite's title bar
     * @param titleSpace the component the title bar pointer is measured in
     * @return whether the sidebar's image changed
     */
    boolean update(Point contentPointer, Point titlePointer, Component titleSpace)
    {
        updateToolbar(titlePointer, titleSpace);
        if (!realSidebar.isVisible() || realSidebar.getWidth() <= 0 || realSidebar.getHeight() <= 0)
        {
            boolean shown = image != null;
            image = null;
            return shown;
        }
        updateTabs();
        return paint(contentPointer);
    }

    private void updateToolbar(Point titlePointer, Component titleSpace)
    {
        TreeMap<NavigationButton, Component> real = SidebarOrder.buttons(toolbarPanel);
        List<NavigationButton> stock = new ArrayList<>();
        realButtons.clear();
        for (Map.Entry<NavigationButton, Component> entry : real.entrySet())
        {
            if (entry.getValue() instanceof AbstractButton && StockContent.isStock(entry.getKey()))
            {
                stock.add(entry.getKey());
                realButtons.put(entry.getKey(), (AbstractButton) entry.getValue());
            }
        }
        stock.addAll(BlockedPlugins.standInButtons());
        if (real.comparator() != null)
        {
            stock.sort(real.comparator());
        }
        if (!stock.equals(buttons))
        {
            buttons = stock;
            copies.clear();
            toolbar.removeAll();
            for (NavigationButton button : stock)
            {
                JButton copy = new JButton(iconOf(button));
                SwingUtil.removeButtonDecorations(copy);
                copy.setToolTipText(button.getTooltip());
                copy.setFocusable(false);
                copy.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
                copy.setAlignmentX(0.5f);
                copy.setAlignmentY(0.5f);
                copies.add(copy);
                toolbar.add(copy);
            }
            toolbar.setMaximumSize(toolbar.getPreferredSize());
            toolbar.revalidate();
            toolbar.repaint();
        }

        for (int i = 0; i < copies.size(); i++)
        {
            JButton copy = copies.get(i);
            Icon icon = iconOf(buttons.get(i));
            if (copy.getIcon() != icon)
            {
                copy.setIcon(icon);
            }
            copy.getModel().setRollover(titlePointer != null && copy.isShowing() && copy.contains(SwingUtilities.convertPoint(titleSpace, titlePointer, copy)));
        }
    }

    private void updateTabs()
    {
        List<NavigationButton> stock = new ArrayList<>();
        for (NavigationButton tab : SidebarOrder.tabs(clientUI))
        {
            if (StockContent.isStock(tab))
            {
                stock.add(tab);
            }
        }
        if (stock.equals(tabs))
        {
            return;
        }
        tabs = stock;
        sidebar.removeAll();
        for (NavigationButton tab : stock)
        {
            JPanel placeholder = new JPanel();
            placeholder.setOpaque(false);
            sidebar.addTab(null, realSidebar.getIconAt(realIndex(tab)), placeholder, tab.getTooltip());
        }
    }

    private boolean paint(Point contentPointer)
    {
        long began = FrameProfiler.begin();
        try
        {
            return paintSidebar(contentPointer);
        }
        finally
        {
            FrameProfiler.end(PAINT, began);
        }
    }

    private boolean paintSidebar(Point contentPointer)
    {
        NavigationButton shown = shownTab();
        int index = shown == null ? -1 : tabs.indexOf(shown);
        if (sidebar.getSelectedIndex() != index)
        {
            sidebar.setSelectedIndex(index);
        }
        Rectangle bounds = realSidebar.getBounds();
        sidebar.setBounds(0, 0, bounds.width, bounds.height);
        MirrorPrint.layOut(sidebar);
        int hovered = contentPointer != null && bounds.contains(contentPointer) ? sidebar.getUI().tabForCoordinate(sidebar, contentPointer.x - bounds.x, contentPointer.y - bounds.y) : -1;
        setRollover(sidebar.getUI(), hovered);

        if (spare == null || spare.getWidth() != bounds.width || spare.getHeight() != bounds.height)
        {
            spare = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D g = new MirrorPrintGraphics(spare.createGraphics());
        try
        {
            MirrorPrint.printAll(sidebar, g);
            if (index >= 0)
            {
                Rectangle area = sidebar.getComponentAt(index).getBounds();
                Point panelPointer = contentPointer == null ? null : new Point(contentPointer.x - bounds.x - area.x, contentPointer.y - bounds.y - area.y);
                paintPanel(g, shown.getPanel(), area, panelPointer);
            }
        }
        finally
        {
            g.dispose();
        }

        if (image != null && image.getWidth() == spare.getWidth() && image.getHeight() == spare.getHeight() && Arrays.equals(pixels(image), pixels(spare)))
        {
            return false;
        }
        BufferedImage painted = spare;
        spare = image;
        image = painted;
        return true;
    }

    private static int[] pixels(BufferedImage image)
    {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    private NavigationButton shownTab()
    {
        NavigationButton real = realTab(realSidebar.getSelectedIndex());
        if (real == null)
        {
            return null;
        }
        if (StockContent.isStock(real))
        {
            return real;
        }
        for (NavigationButton tab : tabs)
        {
            if (tab.getPanel() != null && MirrorConfiguration.isConfiguration(tab.getPanel()))
            {
                return tab;
            }
        }
        return null;
    }

    private NavigationButton realTab(int index)
    {
        return index < 0 ? null : Iterables.get(SidebarOrder.tabs(clientUI), index, null);
    }

    private int realIndex(NavigationButton button)
    {
        return Iterables.indexOf(SidebarOrder.tabs(clientUI), tab -> tab == button);
    }

    private void paintPanel(Graphics2D g, PluginPanel panel, Rectangle area, Point pointer)
    {
        if (panel == null || area.width <= 0 || area.height <= 0)
        {
            return;
        }
        JComponent wrapped = panel.getWrappedPanel();
        MirrorPrint.fit(wrapped, area);
        Graphics2D panelGraphics = (Graphics2D) g.create(area.x, area.y, area.width, area.height);
        try
        {
            if (!MirrorConfiguration.isConfiguration(panel))
            {
                MirrorPrint.printAll(wrapped, panelGraphics);
            }
            else if (!MirrorConfiguration.print(panel, panelGraphics, pointer))
            {
                MirrorPrint.run(() ->
                {
                    panelGraphics.setColor(wrapped.getBackground());
                    panelGraphics.fillRect(0, 0, area.width, area.height);
                });
            }
        }
        finally
        {
            panelGraphics.dispose();
        }
    }

    private Icon iconOf(NavigationButton button)
    {
        AbstractButton original = realButtons.get(button);
        return original != null ? original.getIcon() : BlockedPlugins.standInIcon(button);
    }

    private static void setRollover(TabbedPaneUI ui, int index)
    {
        try
        {
            if (rolloverSetter == null)
            {
                rolloverSetter = Members.method(BasicTabbedPaneUI.class, "setRolloverTab", int.class);
            }
            if (ui instanceof BasicTabbedPaneUI)
            {
                rolloverSetter.invoke(ui, index);
            }
        }
        catch (ReflectiveOperationException ignored)
        {
        }
    }
}
