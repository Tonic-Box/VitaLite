package com.tonic.services.streamer;

import com.tonic.Static;
import com.tonic.services.profiler.frames.FrameProfiler;
import net.runelite.api.BufferProvider;
import net.runelite.api.Client;

import java.awt.Dimension;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The game frames handed from the client thread to the mirror, the newest finished one always waiting between the two. Only some frames are handed over: at most the streamer frame rate a second, none while the mirror has still not painted a frame handed over before the last decision, and with a GPU renderer none while every readback buffer is busy; the others are drawn as with streamer mode off. With a GPU renderer, each frame's interface waits for its scene, which is read back from the graphics card once the card has finished it. Runs on the client thread, apart from taking frames.
 */
public final class MirrorFrames
{
    private static final int[] RGB_MASKS = {0xFF0000, 0xFF00, 0xFF};
    private static final DirectColorModel RGB = new DirectColorModel(24, RGB_MASKS[0], RGB_MASKS[1], RGB_MASKS[2]);
    private static final DirectColorModel ARGB_PRE = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 32, RGB_MASKS[0], RGB_MASKS[1], RGB_MASKS[2], 0xFF000000, true, DataBuffer.TYPE_INT);

    private static final int PUBLISH = FrameProfiler.timer("mirror.publish");

    private static volatile MirrorFrames current;

    private final Runnable onFrame;
    private final AtomicReference<Frame> middle = new AtomicReference<>(new Frame());
    private final ArrayDeque<Frame> spare = new ArrayDeque<>();
    private final ArrayDeque<Frame> held = new ArrayDeque<>();
    private Frame front = new Frame();
    private Frame waiting;
    private boolean sceneSeen;
    private boolean publishedSinceDue;
    private long nextFrameAt;

    private MirrorFrames(Runnable onFrame)
    {
        this.onFrame = onFrame;
    }

    /**
     * Starts handing frames over.
     *
     * @param onFrame run on the client thread after each frame is handed over
     */
    static void open(Runnable onFrame)
    {
        current = new MirrorFrames(onFrame);
    }

    /**
     * Stops handing frames over and lets the buffers go.
     */
    static void close()
    {
        current = null;
    }

    /**
     * @return the frames being handed over, or null while streamer mode is off
     */
    static MirrorFrames get()
    {
        return current;
    }

    /**
     * Copies the finished frame for the mirror, when it is one handed over, once RuneLite has drawn the last of it: the overlays, the notification flash and the client's own overlays. Then decides whether the next frame is handed over. Called when the client finishes painting its overlays while streamer mode is on.
     */
    public static void frameDrawn()
    {
        MirrorFrames frames = current;
        if (frames == null)
        {
            return;
        }
        if (Streamer.mirroring)
        {
            MirrorLayout.nextFrame();
            Client client = Static.getClient();
            BufferProvider provider = client.getBufferProvider();
            if (provider != null)
            {
                long began = FrameProfiler.begin();
                frames.write(client, provider);
                FrameProfiler.end(PUBLISH, began);
            }
        }
        Streamer.mirroring = frames.due(System.nanoTime());
    }

    private boolean due(long now)
    {
        Frame newest = middle.get();
        boolean behind = newest != null && newest.fresh && !publishedSinceDue;
        publishedSinceDue = false;
        if (behind || held.size() + (waiting != null ? 1 : 0) >= GpuMirror.SLOTS || now - nextFrameAt < 0)
        {
            return false;
        }
        long interval = 1_000_000_000L / Streamer.frameRate;
        nextFrameAt = now - nextFrameAt >= interval ? now + interval : nextFrameAt + interval;
        return true;
    }

    private void write(Client client, BufferProvider provider)
    {
        int width = provider.getWidth();
        int height = provider.getHeight();
        int[] pixels = provider.getPixels();
        if (width <= 0 || height <= 0 || pixels == null || pixels.length < width * height)
        {
            return;
        }

        boolean gpu = client.isGpu();
        Frame frame = fresh();
        frame.size(width, height, gpu);
        System.arraycopy(pixels, 0, frame.pixels, 0, width * height);
        MirrorHiding.apply(frame.pixels, width, height);
        boolean stretched = client.isStretchedEnabled();
        frame.fast = client.isStretchedFast();
        frame.stretchedSize = stretched ? client.getStretchedDimensions() : null;
        frame.hasScene = false;
        if (gpu)
        {
            if (waiting != null)
            {
                spare.push(waiting);
            }
            waiting = frame;
        }
        else
        {
            publish(frame);
        }
    }

    /**
     * Notes that the renderer drew a scene this frame. Called by the GPU readback.
     */
    void sceneSeen()
    {
        sceneSeen = true;
    }

    /**
     * @return whether this frame's interface is handed over and waits for its scene to be read
     */
    boolean awaitsScene()
    {
        return waiting != null;
    }

    /**
     * Keeps this frame's interface until the scene the readback just started reading is done.
     *
     * @param place where the scene sits on the canvas, as left, top, width and height fractions of it
     */
    void sceneStarted(float[] place)
    {
        waiting.sceneLeft = place[0];
        waiting.sceneTop = place[1];
        waiting.sceneWidth = place[2];
        waiting.sceneHeight = place[3];
        held.addLast(waiting);
        waiting = null;
    }

    /**
     * Gives the buffer the oldest scene being read is copied into, sized for it.
     *
     * @param width the scene's width in pixels
     * @param height the scene's height in pixels
     * @return the buffer, or null when no interface waits for a scene
     */
    int[] sceneBuffer(int width, int height)
    {
        Frame oldest = held.peekFirst();
        if (oldest == null)
        {
            return null;
        }
        oldest.sizeScene(width, height);
        return oldest.scenePixels;
    }

    /**
     * Hands over the interface the oldest scene being read belongs to, with the scene copied in, or lets it go when the scene could not be read.
     *
     * @param filled whether the scene was copied in
     */
    void sceneDone(boolean filled)
    {
        Frame oldest = held.pollFirst();
        if (oldest == null)
        {
            return;
        }
        if (filled)
        {
            oldest.hasScene = true;
            publish(oldest);
        }
        else
        {
            spare.push(oldest);
        }
    }

    /**
     * Lets go of every interface waiting for a scene, as when the readback starts over.
     */
    void dropScenes()
    {
        while (!held.isEmpty())
        {
            spare.push(held.pollFirst());
        }
    }

    /**
     * Ends a GPU frame. Without a scene this frame, as on the login screen, the interface is handed over alone and the interfaces waiting for scenes are let go; with one, an interface whose scene read could not start is let go.
     *
     * @return whether the renderer drew a scene this frame
     */
    boolean gpuFrameDone()
    {
        boolean seen = sceneSeen;
        sceneSeen = false;
        if (!seen)
        {
            dropScenes();
        }
        if (waiting != null)
        {
            if (seen)
            {
                spare.push(waiting);
            }
            else
            {
                publish(waiting);
            }
            waiting = null;
        }
        return seen;
    }

    private Frame fresh()
    {
        Frame frame = spare.poll();
        return frame != null ? frame : new Frame();
    }

    private void publish(Frame frame)
    {
        publishedSinceDue = true;
        frame.fresh = true;
        Frame previous = middle.getAndSet(frame);
        if (previous != null)
        {
            previous.fresh = false;
            spare.push(previous);
        }
        onFrame.run();
    }

    /**
     * Takes the newest finished frame for painting. Called on the event dispatch thread only.
     *
     * @return the frame to paint, or null before the first one
     */
    Frame take()
    {
        Frame newest = middle.get();
        if (newest != null && newest.fresh)
        {
            Frame taken = middle.getAndSet(null);
            if (taken != null)
            {
                taken.fresh = false;
                Frame old = front;
                front = taken;
                if (old != null)
                {
                    middle.compareAndSet(null, old);
                }
            }
        }
        return front == null || front.image == null ? null : front;
    }

    /**
     * One copied game frame: the interface, and with a GPU renderer the scene under it and where it sits.
     */
    static final class Frame
    {
        int[] pixels;
        BufferedImage image;
        int[] scenePixels;
        BufferedImage scene;
        float sceneLeft;
        float sceneTop;
        float sceneWidth;
        float sceneHeight;
        boolean hasScene;
        boolean fast;
        Dimension stretchedSize;
        volatile boolean fresh;
        private boolean alpha;

        private void size(int width, int height, boolean withAlpha)
        {
            if (image != null && image.getWidth() == width && image.getHeight() == height && alpha == withAlpha)
            {
                return;
            }
            alpha = withAlpha;
            pixels = new int[width * height];
            if (withAlpha)
            {
                WritableRaster raster = Raster.createPackedRaster(new DataBufferInt(pixels, pixels.length), width, height, width, new int[]{RGB_MASKS[0], RGB_MASKS[1], RGB_MASKS[2], 0xFF000000}, null);
                image = new BufferedImage(ARGB_PRE, raster, true, null);
            }
            else
            {
                image = rgbImage(pixels, width, height);
            }
        }

        private void sizeScene(int width, int height)
        {
            if (scene != null && scene.getWidth() == width && scene.getHeight() == height)
            {
                return;
            }
            scenePixels = new int[width * height];
            scene = rgbImage(scenePixels, width, height);
        }

        private static BufferedImage rgbImage(int[] pixels, int width, int height)
        {
            return new BufferedImage(RGB, Raster.createPackedRaster(new DataBufferInt(pixels, pixels.length), width, height, width, RGB_MASKS, null), false, null);
        }
    }
}
