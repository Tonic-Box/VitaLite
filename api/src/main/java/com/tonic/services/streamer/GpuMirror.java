package com.tonic.services.streamer;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.profiler.frames.FrameProfiler;
import net.runelite.api.Client;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.awt.Canvas;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Reads a GPU renderer's scene back for the mirror without waiting on the graphics card: the scene of each frame handed to the mirror is copied into a free pixel buffer of a small ring with a fence behind it, and a buffer is read only once its fence says the card has filled it. Runs on the client thread, where the renderer's GL context is current.
 */
public final class GpuMirror
{
    static final int SLOTS = 3;
    private static final int MAX_WAIT_FRAMES = 30;
    private static final float[] WHOLE_CANVAS = {0, 0, 1, 1};
    private static final int READ = FrameProfiler.timer("mirror.gpuRead");

    private static final int[] buffers = new int[SLOTS];
    private static final int[] bufferBytes = new int[SLOTS];
    private static final long[] fences = new long[SLOTS];
    private static final int[] waited = new int[SLOTS];
    private static final int[] readWidths = new int[SLOTS];
    private static final int[] readHeights = new int[SLOTS];

    private static GLCapabilities context;
    private static int resolveFramebuffer;
    private static int resolveRenderbuffer;
    private static int resolveWidth;
    private static int resolveHeight;
    private static int resolveFormat;
    private static int sourceFormat;
    private static int sourceSamples;
    private static int oldest;
    private static int reading;
    private static boolean failed;

    private GpuMirror()
    {
    }

    /**
     * Collects the scenes the card has finished reading and starts reading this frame's when the frame is handed to the mirror. Called by the GPU plugin before the scene is drawn to the screen, while streamer mode is on.
     *
     * @param sceneFramebuffer the plugin's scene framebuffer
     * @param stretchedWidth the scene's width before display scaling
     * @param stretchedHeight the scene's height before display scaling
     */
    public static void scene(int sceneFramebuffer, int stretchedWidth, int stretchedHeight)
    {
        AffineTransform scale = displayScale();
        int width = (int) (stretchedWidth * scale.getScaleX());
        int height = (int) (stretchedHeight * scale.getScaleY());
        read(sceneFramebuffer, width, height, width, height, WHOLE_CANVAS);
    }

    /**
     * Collects the scenes the card has finished reading and starts reading this frame's when the frame is handed to the mirror.
     *
     * @param framebuffer the framebuffer holding the scene
     * @param sourceWidth the scene's width in the framebuffer
     * @param sourceHeight the scene's height in the framebuffer
     * @param fitWidth the most pixels across worth reading, the scene's width on screen
     * @param fitHeight the most pixels down worth reading, the scene's height on screen
     * @param place where the scene sits on the canvas, as left, top, width and height fractions of it
     */
    static void read(int framebuffer, int sourceWidth, int sourceHeight, int fitWidth, int fitHeight, float[] place)
    {
        MirrorFrames frames = MirrorFrames.get();
        if (frames == null || failed)
        {
            return;
        }
        frames.sceneSeen();
        boolean start = frames.awaitsScene() && reading < SLOTS && sourceWidth > 0 && sourceHeight > 0;
        if (reading == 0 && !start)
        {
            return;
        }

        long began = FrameProfiler.begin();
        int readFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int drawFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int packBuffer = glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING);
        int boundRenderbuffer = glGetInteger(GL_RENDERBUFFER_BINDING);
        try
        {
            prepareContext(frames);
            collect(frames);
            if (start)
            {
                start(frames, framebuffer, sourceWidth, sourceHeight, fitWidth, fitHeight, place);
            }
        }
        catch (RuntimeException e)
        {
            failed = true;
            Logger.error("Streamer mirror could not read the GPU scene: " + e);
        }
        finally
        {
            glBindRenderbuffer(GL_RENDERBUFFER, boundRenderbuffer);
            glBindBuffer(GL_PIXEL_PACK_BUFFER, packBuffer);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, readFramebuffer);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            FrameProfiler.end(READ, began);
        }
    }

    /**
     * Ends the renderer's frame, and lets go of the readback buffers once streamer mode is off. Called at the end of each frame a GPU renderer draws.
     */
    public static void frameDone()
    {
        MirrorFrames frames = MirrorFrames.get();
        if (frames == null || !Streamer.active)
        {
            if (resolveFramebuffer != 0)
            {
                release();
            }
            return;
        }
        if (!frames.gpuFrameDone())
        {
            forgetReads();
        }
    }

    private static void collect(MirrorFrames frames)
    {
        while (reading > 0)
        {
            int slot = oldest;
            int status = glClientWaitSync(fences[slot], GL_SYNC_FLUSH_COMMANDS_BIT, 0);
            if (status == GL_TIMEOUT_EXPIRED && ++waited[slot] < MAX_WAIT_FRAMES)
            {
                return;
            }
            glDeleteSync(fences[slot]);
            fences[slot] = 0;
            oldest = (oldest + 1) % SLOTS;
            reading--;
            boolean ready = status == GL_ALREADY_SIGNALED || status == GL_CONDITION_SATISFIED;
            int[] target = ready ? frames.sceneBuffer(readWidths[slot], readHeights[slot]) : null;
            frames.sceneDone(target != null && copy(slot, target));
        }
    }

    private static void start(MirrorFrames frames, int framebuffer, int sourceWidth, int sourceHeight, int fitWidth, int fitHeight, float[] place)
    {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
        describeSource();
        boolean multisampled = sourceSamples > 0;
        int width = multisampled ? sourceWidth : Math.max(1, Math.min(sourceWidth, fitWidth));
        int height = multisampled ? sourceHeight : Math.max(1, Math.min(sourceHeight, fitHeight));
        prepareResolve(width, height, multisampled ? sourceFormat : GL_RGBA8);

        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, resolveFramebuffer);
        int filter = width == sourceWidth && height == sourceHeight ? GL_NEAREST : GL_LINEAR;
        glBlitFramebuffer(0, 0, sourceWidth, sourceHeight, 0, 0, width, height, GL_COLOR_BUFFER_BIT, filter);

        int slot = (oldest + reading) % SLOTS;
        glBindFramebuffer(GL_READ_FRAMEBUFFER, resolveFramebuffer);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, buffers[slot]);
        int bytes = width * height * 4;
        if (bufferBytes[slot] != bytes)
        {
            glBufferData(GL_PIXEL_PACK_BUFFER, bytes, GL_STREAM_READ);
            bufferBytes[slot] = bytes;
        }
        glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, 0L);
        fences[slot] = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        waited[slot] = 0;
        readWidths[slot] = width;
        readHeights[slot] = height;
        reading++;
        frames.sceneStarted(place);
    }

    private static boolean copy(int slot, int[] target)
    {
        int width = readWidths[slot];
        int height = readHeights[slot];
        glBindBuffer(GL_PIXEL_PACK_BUFFER, buffers[slot]);
        ByteBuffer mapped = glMapBufferRange(GL_PIXEL_PACK_BUFFER, 0, (long) width * height * 4, GL_MAP_READ_BIT);
        if (mapped == null)
        {
            return false;
        }
        try
        {
            IntBuffer rows = mapped.order(ByteOrder.nativeOrder()).asIntBuffer();
            for (int row = 0; row < height; row++)
            {
                rows.position(row * width);
                rows.get(target, (height - 1 - row) * width, width);
            }
            return true;
        }
        finally
        {
            glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
        }
    }

    private static void forgetReads()
    {
        if (GL.getCapabilities() == context)
        {
            for (int slot = 0; slot < SLOTS; slot++)
            {
                if (fences[slot] != 0)
                {
                    glDeleteSync(fences[slot]);
                }
            }
        }
        Arrays.fill(fences, 0);
        oldest = 0;
        reading = 0;
    }

    private static void prepareContext(MirrorFrames frames)
    {
        GLCapabilities capabilities = GL.getCapabilities();
        if (capabilities != context)
        {
            forgetReads();
            frames.dropScenes();
            context = capabilities;
            resolveFramebuffer = 0;
        }
        if (resolveFramebuffer == 0)
        {
            resolveFramebuffer = glGenFramebuffers();
            resolveRenderbuffer = glGenRenderbuffers();
            resolveWidth = 0;
            resolveHeight = 0;
            resolveFormat = 0;
            for (int slot = 0; slot < SLOTS; slot++)
            {
                buffers[slot] = glGenBuffers();
                bufferBytes[slot] = 0;
            }
        }
    }

    private static void describeSource()
    {
        sourceFormat = GL_RGBA8;
        sourceSamples = 0;
        if (glGetFramebufferAttachmentParameteri(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE) != GL_RENDERBUFFER)
        {
            return;
        }
        glBindRenderbuffer(GL_RENDERBUFFER, glGetFramebufferAttachmentParameteri(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME));
        int format = glGetRenderbufferParameteri(GL_RENDERBUFFER, GL_RENDERBUFFER_INTERNAL_FORMAT);
        if (format != 0)
        {
            sourceFormat = format;
        }
        sourceSamples = glGetRenderbufferParameteri(GL_RENDERBUFFER, GL_RENDERBUFFER_SAMPLES);
    }

    private static void prepareResolve(int width, int height, int format)
    {
        if (resolveWidth == width && resolveHeight == height && resolveFormat == format)
        {
            return;
        }
        glBindRenderbuffer(GL_RENDERBUFFER, resolveRenderbuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, format, width, height);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, resolveFramebuffer);
        glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, resolveRenderbuffer);
        resolveWidth = width;
        resolveHeight = height;
        resolveFormat = format;
    }

    private static void release()
    {
        forgetReads();
        if (GL.getCapabilities() == context)
        {
            glDeleteFramebuffers(resolveFramebuffer);
            glDeleteRenderbuffers(resolveRenderbuffer);
            glDeleteBuffers(buffers);
        }
        resolveFramebuffer = 0;
        context = null;
        failed = false;
    }

    private static AffineTransform displayScale()
    {
        Client client = Static.getClient();
        Canvas canvas = client.getCanvas();
        GraphicsConfiguration configuration = canvas == null ? null : canvas.getGraphicsConfiguration();
        return configuration == null ? new AffineTransform() : configuration.getDefaultTransform();
    }
}
