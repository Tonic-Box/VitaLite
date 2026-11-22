package com.tonic.services.streamer;

import com.google.common.collect.MapMaker;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.ui.overlay.tooltip.TooltipOverlay;

import java.awt.Graphics2D;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tooltips as the mirror shows them. Each tooltip is marked when it is added with whether stock content added it and whether only the real view or only the mirror should show it; when the two lists differ, RuneLite's tooltips are drawn for the real view only and the mirror's list is drawn for the mirror, so they stack as they would in RuneLite. Runs on the client thread only.
 */
public final class MirrorTooltips
{
    private static final String TOOLTIP_PACKAGE = "net.runelite.client.ui.overlay.tooltip.";

    private static final Map<Tooltip, Mark> marks = new MapMaker().weakKeys().makeMap();

    private static Field managerField;
    private static Field widthField;
    private static Field heightField;
    private static Method renderTooltips;
    private static boolean broken;
    private static int mirrorWidth;
    private static int mirrorHeight;

    private MirrorTooltips()
    {
    }

    /**
     * Marks a tooltip being added. Called by the tooltip manager while streamer mode is on.
     *
     * @param tooltip the tooltip
     */
    public static void added(Tooltip tooltip)
    {
        Overlay owner = MirrorOverlays.drawing();
        boolean stock = owner != null ? StockContent.isStock(owner) : StockContent.callerIsStock(TOOLTIP_PACKAGE);
        MirrorOverlays.Side side = MirrorOverlays.side();
        if (!stock || side != MirrorOverlays.Side.BOTH)
        {
            marks.put(tooltip, new Mark(stock, side));
        }
    }

    /**
     * Splits the tooltips about to be drawn: those only the mirror shows are taken out of RuneLite's list.
     *
     * @param overlay the tooltip overlay
     * @return the tooltips the mirror shows, or null when it shows the same ones as the real view
     */
    static List<Tooltip> split(Overlay overlay)
    {
        if (!(overlay instanceof TooltipOverlay) || !resolve())
        {
            return null;
        }
        List<Tooltip> tooltips;
        try
        {
            tooltips = ((TooltipManager) managerField.get(overlay)).getTooltips();
        }
        catch (ReflectiveOperationException e)
        {
            broken = true;
            return null;
        }
        if (marks.isEmpty() || tooltips.isEmpty())
        {
            return null;
        }
        List<Tooltip> mirror = new ArrayList<>(tooltips.size());
        boolean differs = false;
        for (int i = 0; i < tooltips.size(); i++)
        {
            Tooltip tooltip = tooltips.get(i);
            Mark mark = marks.get(tooltip);
            if (mark == null)
            {
                mirror.add(tooltip);
                continue;
            }
            differs = true;
            if (mark.stock && mark.side != MirrorOverlays.Side.REAL)
            {
                mirror.add(tooltip);
            }
            if (mark.side == MirrorOverlays.Side.MIRROR)
            {
                tooltips.remove(i--);
            }
        }
        return differs ? mirror : null;
    }

    /**
     * Draws the mirror's tooltips, leaving the overlay's own state as RuneLite's draw left it.
     *
     * @param overlay the tooltip overlay
     * @param graphics graphics drawing for the mirror
     * @param tooltips the tooltips the mirror shows
     */
    static void render(Overlay overlay, Graphics2D graphics, List<Tooltip> tooltips)
    {
        if (tooltips.isEmpty())
        {
            return;
        }
        int realWidth;
        int realHeight;
        try
        {
            realWidth = widthField.getInt(overlay);
            realHeight = heightField.getInt(overlay);
        }
        catch (ReflectiveOperationException e)
        {
            broken = true;
            return;
        }
        try
        {
            widthField.setInt(overlay, mirrorWidth);
            heightField.setInt(overlay, mirrorHeight);
            renderTooltips.invoke(overlay, graphics, tooltips);
            mirrorWidth = widthField.getInt(overlay);
            mirrorHeight = heightField.getInt(overlay);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            mirrorWidth = 0;
            mirrorHeight = 0;
        }
        finally
        {
            try
            {
                widthField.setInt(overlay, realWidth);
                heightField.setInt(overlay, realHeight);
            }
            catch (ReflectiveOperationException e)
            {
                broken = true;
            }
        }
    }

    private static boolean resolve()
    {
        if (broken)
        {
            return false;
        }
        if (renderTooltips != null)
        {
            return true;
        }
        try
        {
            managerField = Members.field(TooltipOverlay.class, "tooltipManager");
            widthField = Members.field(TooltipOverlay.class, "prevWidth");
            heightField = Members.field(TooltipOverlay.class, "prevHeight");
            renderTooltips = Members.method(TooltipOverlay.class, "renderTooltips", Graphics2D.class, List.class);
            return true;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            broken = true;
            return false;
        }
    }

    private static final class Mark
    {
        private final boolean stock;
        private final MirrorOverlays.Side side;

        private Mark(boolean stock, MirrorOverlays.Side side)
        {
            this.stock = stock;
            this.side = side;
        }
    }
}
