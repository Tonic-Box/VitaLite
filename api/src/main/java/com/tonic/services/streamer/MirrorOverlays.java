package com.tonic.services.streamer;

import com.tonic.Logger;
import com.tonic.services.ground.GroundHooks;
import com.tonic.services.profiler.frames.FrameProfiler;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Decides how each overlay RuneLite draws reaches the mirror: drawn for both views as it is, left out of the mirror when it is not stock, drawn again from a log where the mirror places it elsewhere, or drawn a second time with only its stock content. Runs on the client thread only.
 */
public final class MirrorOverlays
{
    /**
     * Which views content drawn now is for.
     */
    enum Side
    {
        BOTH,
        REAL,
        MIRROR
    }

    private enum Kind
    {
        SHARED,
        HIDDEN,
        MOVED,
        INFO_BOXES,
        MENU,
        TOOLTIPS
    }

    private static final Set<String> MENU_OVERLAYS = Set.of("net.runelite.client.plugins.mousehighlight.MouseHighlightOverlay");
    private static final int STOCK = FrameProfiler.timer("mirror.overlay.stock");
    private static final int OTHER = FrameProfiler.timer("mirror.overlay.nonStock");

    private static Overlay overlay;
    private static Kind kind = Kind.SHARED;
    private static Side side = Side.BOTH;
    private static Point real;
    private static Point mirror;
    private static Graphics2D start;
    private static DrawingLog log;
    private static List<Tooltip> mirrorTooltips;
    private static Object renderer;
    private static boolean outlineHidden;
    private static Rectangle outlineMirror;
    private static Field fontField;
    private static Field tooltipFontField;
    private static Field interfaceFontField;
    private static boolean fontBroken;
    private static boolean drawFailed;
    private static long overlayBegan;

    private MirrorOverlays()
    {
    }

    /**
     * Starts an overlay's draw. Called by the overlay renderer before each overlay draws while streamer mode is on.
     *
     * @param overlayRenderer RuneLite's overlay renderer
     * @param drawn the overlay about to draw
     * @param graphics the graphics it draws with
     * @param location where RuneLite draws it
     * @return the graphics to draw with
     */
    public static Graphics2D begin(Object overlayRenderer, Overlay drawn, Graphics2D graphics, Point location)
    {
        overlayBegan = FrameProfiler.begin();
        reset();
        outlineHidden = false;
        outlineMirror = null;
        renderer = overlayRenderer;
        overlay = drawn;
        real = new Point(location);
        if (!StockContent.isStock(drawn))
        {
            kind = Kind.HIDDEN;
            return MirrorHiding.hide(graphics);
        }

        boolean infoBoxes = MirrorInfoBoxes.hidesAny(drawn);
        Dimension size = infoBoxes ? MirrorInfoBoxes.mirrorSize(drawn) : drawn.getBounds().getSize();
        mirror = MirrorLayout.place(overlayRenderer, drawn, location, size, !infoBoxes);
        if (infoBoxes)
        {
            return split(Kind.INFO_BOXES, graphics);
        }
        if (MENU_OVERLAYS.contains(drawn.getClass().getName()) && MirrorMenu.menuDiffers())
        {
            return split(Kind.MENU, graphics);
        }
        mirrorTooltips = MirrorTooltips.split(drawn);
        if (mirrorTooltips != null)
        {
            return split(Kind.TOOLTIPS, graphics);
        }
        if (!mirror.equals(real))
        {
            kind = Kind.MOVED;
            log = new DrawingLog((Graphics2D) GroundHooks.target(graphics).create());
            return MirrorHiding.hide(graphics, log);
        }
        return NameGraphics.shared(graphics);
    }

    /**
     * Ends the overlay's draw, drawing what only the mirror shows of it. Called by the overlay renderer after each overlay draws while streamer mode is on.
     *
     * @param drawn the overlay that drew
     */
    public static void done(Overlay drawn)
    {
        try
        {
            finish(drawn);
        }
        finally
        {
            FrameProfiler.end(drawn != null && drawn.getClass().getName().startsWith("net.runelite.") ? STOCK : OTHER, overlayBegan);
        }
    }

    private static void finish(Overlay drawn)
    {
        MirrorHiding.end();
        if (drawn != overlay)
        {
            MirrorLayout.skipped();
            clear();
            return;
        }
        try
        {
            switch (kind)
            {
                case HIDDEN:
                    outlineHidden = true;
                    MirrorLayout.skipped();
                    break;
                case MOVED:
                    replay();
                    outlineMirror = new Rectangle(mirror, drawn.getBounds().getSize());
                    MirrorLayout.placed(outlineMirror);
                    break;
                case MENU:
                    drawAgain(Kind.MENU);
                    MirrorLayout.placed(new Rectangle(mirror, drawn.getBounds().getSize()));
                    break;
                case INFO_BOXES:
                    Rectangle shown = new Rectangle(mirror, drawAgain(Kind.INFO_BOXES));
                    outlineMirror = shown.equals(drawn.getBounds()) ? null : shown;
                    MirrorLayout.placed(shown);
                    break;
                case TOOLTIPS:
                    drawAgain(Kind.TOOLTIPS);
                    MirrorLayout.skipped();
                    break;
                default:
                    MirrorLayout.placed(new Rectangle(mirror, drawn.getBounds().getSize()));
                    break;
            }
        }
        finally
        {
            clear();
        }
    }

    /**
     * Draws the outline RuneLite puts around the overlay drawn last while overlays are being moved: for the real view only when the mirror leaves the overlay out, and around the mirror's copy when the mirror shows it elsewhere or at another size. Called by the overlay renderer in place of its own outline draw while streamer mode is on.
     *
     * @param graphics the renderer's graphics, set up for the outline
     * @param outline the overlay's bounds
     */
    public static void outline(Graphics2D graphics, Shape outline)
    {
        if (!outlineHidden && outlineMirror == null)
        {
            graphics.draw(outline);
            return;
        }
        MirrorHiding.drawHidden(graphics, hidden -> hidden.draw(outline));
        Rectangle shown = outlineMirror;
        if (shown != null && !shown.isEmpty())
        {
            MirrorHiding.drawMirrorOnly(graphics, onto -> onto.draw(shown));
        }
    }

    /**
     * Draws a shape the ground pass took out of an overlay's draw flat for the mirror, as stock RuneLite would, when the overlay drawing is one the mirror shows where it is. Called by the ground capture after each shape it takes while streamer mode is on.
     *
     * @param target the graphics the overlay draws through
     * @param draw the overlay's draw call
     */
    public static void captured(Graphics2D target, Consumer<Graphics2D> draw)
    {
        if (overlay != null && kind == Kind.SHARED)
        {
            MirrorHiding.drawMirrorOnly(target, draw);
        }
    }

    /**
     * Forgets any draw in progress, as when streamer mode starts.
     */
    static void reset()
    {
        MirrorHiding.end();
        MirrorLayout.skipped();
        clear();
    }

    private static Graphics2D split(Kind drawn, Graphics2D graphics)
    {
        kind = drawn;
        side = Side.REAL;
        start = (Graphics2D) GroundHooks.target(graphics).create();
        return MirrorHiding.hide(graphics);
    }

    private static void clear()
    {
        if (start != null)
        {
            start.dispose();
        }
        if (log != null)
        {
            log.start().dispose();
        }
        overlay = null;
        kind = Kind.SHARED;
        side = Side.BOTH;
        start = null;
        log = null;
        mirrorTooltips = null;
        real = null;
        mirror = null;
    }

    /**
     * @return the overlay drawing now, or null between overlays
     */
    static Overlay drawing()
    {
        return overlay;
    }

    /**
     * @return which views content drawn now is for
     */
    static Side side()
    {
        return side;
    }

    private static void replay()
    {
        Graphics2D onto = MirrorHiding.mirror(log.start());
        if (onto == null)
        {
            return;
        }
        try
        {
            log.replay(NameGraphics.mirrorOnly(onto), mirror.x - real.x, mirror.y - real.y);
        }
        catch (RuntimeException e)
        {
            failed(e);
        }
        finally
        {
            MirrorHiding.end();
        }
    }

    private static Dimension drawAgain(Kind again)
    {
        Graphics2D onto = NameGraphics.mirrorOnly(MirrorHiding.mirror(start));
        if (onto == null)
        {
            return new Dimension();
        }
        side = Side.MIRROR;
        try
        {
            Font font = font(overlay.getPosition());
            if (font != null)
            {
                onto.setFont(font);
            }
            onto.translate(mirror.x, mirror.y);
            if (again == Kind.INFO_BOXES)
            {
                return MirrorInfoBoxes.render(overlay, onto, mirror);
            }
            if (again == Kind.MENU)
            {
                Overlay drawn = overlay;
                MirrorMenu.withStockMenu(() -> drawn.render(onto));
                return new Dimension();
            }
            MirrorTooltips.render(overlay, onto, mirrorTooltips);
            return new Dimension();
        }
        catch (RuntimeException e)
        {
            failed(e);
            return new Dimension();
        }
        finally
        {
            MirrorHiding.end();
        }
    }

    private static void failed(RuntimeException e)
    {
        if (!drawFailed)
        {
            drawFailed = true;
            Logger.error("Streamer mirror could not draw an overlay for the mirror: " + e);
        }
    }

    private static Font font(OverlayPosition position)
    {
        if (fontBroken)
        {
            return null;
        }
        try
        {
            if (interfaceFontField == null)
            {
                Class<?> type = renderer.getClass();
                fontField = Members.field(type, "font");
                tooltipFontField = Members.field(type, "tooltipFont");
                interfaceFontField = Members.field(type, "interfaceFont");
            }
            if (position == OverlayPosition.DYNAMIC || position == OverlayPosition.DETACHED)
            {
                return (Font) fontField.get(renderer);
            }
            if (position == OverlayPosition.TOOLTIP)
            {
                return (Font) tooltipFontField.get(renderer);
            }
            return (Font) interfaceFontField.get(renderer);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            fontBroken = true;
            return null;
        }
    }
}
