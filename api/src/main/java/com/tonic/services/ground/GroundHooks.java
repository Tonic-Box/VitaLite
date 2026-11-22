package com.tonic.services.ground;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.WorldOverlays;
import com.tonic.services.profiler.frames.FrameProfiler;
import com.tonic.services.streamer.MirrorHiding;
import com.tonic.services.streamer.Streamer;
import net.runelite.api.BufferProvider;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.OverlayLayer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Entry points the injected client calls to run the ground pass each frame; plugins use GroundOverlays instead.
 */
public final class GroundHooks
{
    private static final int GL_FRAMES = 3;

    private static final List<GroundLayer> frame = new ArrayList<>();
    private static final GroundRaster raster = new GroundRaster();
    private static final GroundGl gl = new GroundGl();
    private static final GroundMeshes meshes = new GroundMeshes();
    private static final Set<Mode> failed = EnumSet.noneOf(Mode.class);

    private static Mode mode = Mode.FLAT;
    private static long frames;
    private static long glSeenFrame = Long.MIN_VALUE / 2;
    private static boolean hadGround;
    private static BufferedImage deferredImage;
    private static Graphics2D deferredGraphics;
    private static BufferedImage mirrorDeferredImage;
    private static final PaintedTiles painted = new PaintedTiles();
    private static final PaintedTiles mirrorPainted = new PaintedTiles();
    private static Graphics2D sceneGraphics;
    private static final int FINISH = FrameProfiler.timer("ground.finish");
    private static final int FINISH_MIRRORED = FrameProfiler.timer("ground.finishMirrored");

    private GroundHooks()
    {
    }

    /**
     * Wraps an overlay layer's graphics so its ground shapes are captured. Called before the layer draws.
     *
     * @param graphics the layer's graphics
     * @param layer the overlay layer
     * @return the graphics to draw the layer with
     */
    public static Graphics2D wrap(Graphics2D graphics, OverlayLayer layer)
    {
        if (layer == OverlayLayer.ABOVE_SCENE)
        {
            sceneGraphics = graphics;
            beginFrame();
        }
        if (!mode.drawsGround())
        {
            return graphics;
        }

        if (layer == OverlayLayer.ABOVE_SCENE)
        {
            CapturedShapes.beginAbove();
            if (mode == Mode.SOFTWARE && hadGround)
            {
                Graphics2D deferred = deferTo(graphics);
                if (deferred != null)
                {
                    if (Streamer.mirroring)
                    {
                        MirrorHiding.deferTo(deferredImage);
                    }
                    return new GroundCapture(deferred);
                }
            }
            return new GroundCapture(graphics);
        }
        if (layer == OverlayLayer.UNDER_WIDGETS)
        {
            CapturedShapes.beginUnder();
            return new GroundCapture(graphics);
        }
        return graphics;
    }

    /**
     * Wraps the graphics overlays draw with over the interfaces so their ground shapes are captured.
     *
     * @param graphics the graphics
     * @return the graphics to draw the overlays with
     */
    public static Graphics2D wrapWidgets(Graphics2D graphics)
    {
        if (!mode.drawsGround())
        {
            return graphics;
        }
        CapturedShapes.beginWidgets(frames);
        return new GroundCapture(graphics);
    }

    /**
     * Ends an overlay layer, drawing the ground after the scene layer. Called after the layer draws.
     *
     * @param graphics the graphics the layer was drawn with
     * @param layer the overlay layer
     */
    public static void finish(Graphics2D graphics, OverlayLayer layer)
    {
        long began = FrameProfiler.begin();
        finishLayer(graphics, layer);
        FrameProfiler.end(FINISH, began);
    }

    private static void finishLayer(Graphics2D graphics, OverlayLayer layer)
    {
        CapturedShapes.end();
        if (layer != OverlayLayer.ABOVE_SCENE)
        {
            return;
        }
        Graphics2D target = sceneGraphics != null ? sceneGraphics : graphics;
        sceneGraphics = null;
        if (!mode.drawsGround())
        {
            hadGround = false;
            if (Streamer.mirroring)
            {
                MirrorHiding.drawHidden(target, GroundHooks::drawFlat);
            }
            else
            {
                drawFlat(target);
            }
            return;
        }

        hadGround = hasGround();
        if (Streamer.mirroring && mode == Mode.SOFTWARE)
        {
            long began = FrameProfiler.begin();
            finishMirrored(target);
            FrameProfiler.end(FINISH_MIRRORED, began);
            return;
        }
        if (mode == Mode.SOFTWARE && hadGround)
        {
            raster.draw(Static.getClient(), frame, false);
        }
        if (mode == Mode.MESH)
        {
            updateMeshes();
        }
        if (takeDeferredGraphics())
        {
            composite(target, deferredImage);
        }
    }

    /**
     * @param graphics an overlay layer's graphics
     * @return the graphics it draws through, past the ground capture
     */
    public static Graphics2D target(Graphics2D graphics)
    {
        return graphics instanceof GroundCapture ? ((GroundCapture) graphics).target() : graphics;
    }

    private static void finishMirrored(Graphics2D target)
    {
        boolean deferredLayer = false;
        if (deferredGraphics != null)
        {
            if (mirrorDeferredImage == null || mirrorDeferredImage.getWidth() != deferredImage.getWidth() || mirrorDeferredImage.getHeight() != deferredImage.getHeight())
            {
                mirrorDeferredImage = new BufferedImage(deferredImage.getWidth(), deferredImage.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
            }
            mirrorPainted.clear(mirrorDeferredImage.getWidth(), mirrorDeferredImage.getHeight());
            deferredLayer = MirrorHiding.takeDeferred(mirrorDeferredImage, mirrorPainted::mark);
        }
        if (!hadGround && deferredGraphics == null)
        {
            return;
        }

        MirrorHiding.hideFrame();
        try
        {
            if (hadGround)
            {
                raster.draw(Static.getClient(), frame, true);
            }
            if (takeDeferredGraphics())
            {
                painted.scan(deferredImage);
                composite(target, deferredImage, painted);
            }
        }
        finally
        {
            MirrorHiding.end();
        }

        if (deferredLayer)
        {
            mirrorPainted.include(painted);
            MirrorHiding.mirrorFrame();
            try
            {
                composite(target, mirrorDeferredImage, mirrorPainted);
            }
            finally
            {
                MirrorHiding.end();
            }
        }
    }

    private static void composite(Graphics2D target, BufferedImage image, PaintedTiles tiles)
    {
        if (tiles.isEmpty())
        {
            return;
        }
        tiles.touch();
        Graphics2D out = compositeGraphics(target);
        tiles.draw(out, image);
        out.dispose();
    }

    /**
     * Draws the frame's ground into the GPU plugin's scene before it is shown. Called by the GPU plugin.
     *
     * @param framebuffer the scene framebuffer
     * @param program the scene's shader program
     * @param worldProjection the program's world projection uniform
     */
    public static void drawGl(int framebuffer, int program, int worldProjection)
    {
        glSeenFrame = frames;
        if (mode != Mode.GL)
        {
            gl.release();
            return;
        }
        try
        {
            gl.draw(Static.getClient(), frame, framebuffer, program, worldProjection);
        }
        catch (RuntimeException e)
        {
            disable(Mode.GL, e);
        }
    }

    /**
     * Records a projected point when it lies on the ground. Called by Perspective for every local point it projects.
     *
     * @param point the canvas point, or null when the local point did not project
     * @param client the client
     * @param worldViewId the world view the point is local to
     * @param x the local x
     * @param y the local y
     * @param z the height the point was projected at
     */
    public static void projected(Point point, Client client, int worldViewId, int x, int y, int z)
    {
        GroundPoints.projected(point, client, worldViewId, x, y, z);
    }

    /**
     * Marks the start of a tile polygon, whose corners are ground points whatever height offset they are drawn at.
     *
     * @param plane the level the polygon was asked for, or -1 for the world view's level
     */
    public static void enterArea(int plane)
    {
        GroundPoints.enterArea(plane);
    }

    /**
     * Marks the end of a tile polygon.
     */
    public static void exitArea()
    {
        GroundPoints.exitArea();
    }

    private static void beginFrame()
    {
        frames++;
        Mode next = choose(Static.getClient());
        if (mode == Mode.MESH && next != Mode.MESH)
        {
            meshes.clear();
        }
        mode = next;

        GroundPoints.beginFrame(mode.drawsGround());
        CapturedShapes.beginFrame(mode.drawsGround());
        frame.clear();
        CapturedShapes.addTo(frame);
        GroundOverlays.paintInto(frame);
    }

    private static Mode choose(Client client)
    {
        if (client == null || WorldOverlays.isHidden() || !Static.getVitaConfig().shouldDrawGroundUnderObjects())
        {
            return Mode.FLAT;
        }
        if (!client.isGpu())
        {
            return Mode.SOFTWARE;
        }
        if (!failed.contains(Mode.GL) && glSeenFrame >= frames - GL_FRAMES)
        {
            return Mode.GL;
        }
        if (!failed.contains(Mode.MESH) && client.getDrawCallbacks() != null && !Streamer.active)
        {
            return Mode.MESH;
        }
        return Mode.FLAT;
    }

    private static void updateMeshes()
    {
        try
        {
            meshes.update(Static.getClient(), frame);
        }
        catch (RuntimeException e)
        {
            meshes.clear();
            disable(Mode.MESH, e);
        }
    }

    private static void disable(Mode broken, RuntimeException cause)
    {
        failed.add(broken);
        mode = Mode.FLAT;
        Logger.error("Ground overlay " + broken.name().toLowerCase() + " pass failed, disabled: " + cause);
    }

    private static boolean hasGround()
    {
        for (GroundLayer layer : frame)
        {
            if (!layer.isEmpty())
            {
                return true;
            }
        }
        return false;
    }

    private static void drawFlat(Graphics2D graphics)
    {
        Client client = Static.getClient();
        if (client == null)
        {
            return;
        }

        int[] xs = new int[3];
        int[] ys = new int[3];
        Color previous = graphics.getColor();
        for (GroundLayer layer : frame)
        {
            for (GroundTriangles triangles : layer.views())
            {
                for (int triangle = 0; triangle < triangles.count(); triangle++)
                {
                    if (!project(client, triangles, triangle, xs, ys))
                    {
                        continue;
                    }
                    graphics.setColor(new Color(triangles.colour(triangle), true));
                    graphics.fillPolygon(xs, ys, 3);
                }
            }
        }
        graphics.setColor(previous);
    }

    private static boolean project(Client client, GroundTriangles triangles, int triangle, int[] xs, int[] ys)
    {
        for (int corner = 0; corner < 3; corner++)
        {
            Point point = Perspective.localToCanvas(client, triangles.worldViewId(), Math.round(triangles.x(triangle, corner)), Math.round(triangles.y(triangle, corner)), Math.round(triangles.height(triangle, corner)));
            if (point == null)
            {
                return false;
            }
            xs[corner] = point.getX();
            ys[corner] = point.getY();
        }
        return true;
    }

    private static Graphics2D deferTo(Graphics2D graphics)
    {
        Client client = Static.getClient();
        BufferProvider buffer = client.getBufferProvider();
        if (buffer == null || buffer.getWidth() <= 0 || buffer.getHeight() <= 0)
        {
            return null;
        }
        if (deferredImage == null || deferredImage.getWidth() != buffer.getWidth() || deferredImage.getHeight() != buffer.getHeight())
        {
            deferredImage = new BufferedImage(buffer.getWidth(), buffer.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        }

        Graphics2D target = deferredImage.createGraphics();
        target.setComposite(AlphaComposite.Clear);
        target.fillRect(0, 0, deferredImage.getWidth(), deferredImage.getHeight());
        target.setTransform(graphics.getTransform());
        target.setClip(graphics.getClip());
        target.setRenderingHints(graphics.getRenderingHints());
        target.setFont(graphics.getFont());
        target.setBackground(graphics.getBackground());
        target.setStroke(graphics.getStroke());
        target.setPaint(graphics.getPaint());
        target.setComposite(graphics.getComposite());
        deferredGraphics = target;
        return target;
    }

    private static boolean takeDeferredGraphics()
    {
        if (deferredGraphics == null)
        {
            return false;
        }
        deferredGraphics.dispose();
        deferredGraphics = null;
        return true;
    }

    private static void composite(Graphics2D target, BufferedImage image)
    {
        Graphics2D out = compositeGraphics(target);
        out.drawImage(image, 0, 0, null);
        out.dispose();
    }

    private static Graphics2D compositeGraphics(Graphics2D target)
    {
        Graphics2D out = (Graphics2D) target.create();
        out.setTransform(new AffineTransform());
        out.setClip(null);
        out.setComposite(AlphaComposite.SrcOver);
        return out;
    }

    private enum Mode
    {
        FLAT,
        SOFTWARE,
        GL,
        MESH;

        boolean drawsGround()
        {
            return this != FLAT;
        }
    }
}
