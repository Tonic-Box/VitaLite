package com.tonic.services.streamer;

import com.tonic.Logger;
import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.client.ui.DrawManager;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * Reads 117 HD's scene for the mirror from RuneLite's every-frame listener, registered only while streamer mode is on: 117 HD runs it after each frame it shows, with its GL context current and the frame's scene still in its framebuffer.
 */
final class HdMirror
{
    private static final String HD_PACKAGE = "rs117.hd.";
    private static final Runnable LISTENER = HdMirror::drawComplete;
    private static final float[] PLACE = new float[4];
    private static final ClassValue<Optional<Access>> ACCESS = new ClassValue<>()
    {
        @Override
        protected Optional<Access> computeValue(Class<?> renderer)
        {
            return Optional.ofNullable(Access.of(renderer));
        }
    };

    private static boolean reported;

    private HdMirror()
    {
    }

    /**
     * Starts reading 117 HD's scene each frame. Called once streamer mode is on; the listener takes itself off once it is off again, after letting go of the readback buffers.
     */
    static void start()
    {
        drawManager().registerEveryFrameListener(LISTENER);
    }

    private static void drawComplete()
    {
        Client client = Static.getClient();
        Object renderer = client.getDrawCallbacks();
        boolean hd = renderer != null && renderer.getClass().getName().startsWith(HD_PACKAGE);
        if (!Streamer.active)
        {
            drawManager().unregisterEveryFrameListener(LISTENER);
            if (Streamer.active)
            {
                start();
            }
            else if (hd)
            {
                GpuMirror.frameDone();
            }
            return;
        }
        if (!hd)
        {
            return;
        }

        Access access = ACCESS.get(renderer.getClass()).orElse(null);
        if (access != null)
        {
            try
            {
                readScene(access, renderer);
            }
            catch (IllegalAccessException | RuntimeException e)
            {
                report("117 HD's scene could not be read, the mirror shows its interface only: " + e);
            }
        }
        else
        {
            report("This 117 HD is not one the mirror can read, the mirror shows its interface only");
        }
        GpuMirror.frameDone();
    }

    private static void readScene(Access access, Object renderer) throws IllegalAccessException
    {
        Object plugin = access.plugin.get(renderer);
        if (plugin == null || !access.sceneShown(renderer, plugin))
        {
            return;
        }
        int[] resolution = (int[]) access.sceneResolution.get(plugin);
        int[] viewport = (int[]) access.sceneViewport.get(plugin);
        int[] canvas = (int[]) access.actualUiResolution.get(plugin);
        if (resolution == null || viewport == null || canvas == null || canvas[0] <= 0 || canvas[1] <= 0)
        {
            return;
        }
        int resolve = access.fboSceneResolve.getInt(plugin);
        int framebuffer = resolve != 0 ? resolve : access.fboScene.getInt(plugin);
        if (framebuffer == 0)
        {
            return;
        }

        PLACE[0] = viewport[0] / (float) canvas[0];
        PLACE[1] = (canvas[1] - viewport[1] - viewport[3]) / (float) canvas[1];
        PLACE[2] = viewport[2] / (float) canvas[0];
        PLACE[3] = viewport[3] / (float) canvas[1];
        GpuMirror.read(framebuffer, resolution[0], resolution[1], viewport[2], viewport[3], PLACE);
    }

    private static void report(String message)
    {
        if (!reported)
        {
            reported = true;
            Logger.error(message);
        }
    }

    private static DrawManager drawManager()
    {
        return Static.getInjector().getInstance(DrawManager.class);
    }

    /**
     * The 117 HD fields the mirror reads, for one of its renderers: the zone renderer, which says whether its scene is valid, or the legacy renderer, whose scene is shown once there is a scene context after login.
     */
    private static final class Access
    {
        private final Field plugin;
        private final Field fboScene;
        private final Field fboSceneResolve;
        private final Field sceneResolution;
        private final Field sceneViewport;
        private final Field actualUiResolution;
        private final Field hasLoggedIn;
        private final Field sceneFboValid;
        private final Field sceneContext;

        private Access(Field plugin, Field sceneFboValid, Field sceneContext)
        {
            Class<?> pluginClass = plugin.getType();
            this.plugin = plugin;
            this.fboScene = Members.inherited(pluginClass, "fboScene");
            this.fboSceneResolve = Members.inherited(pluginClass, "fboSceneResolve");
            this.sceneResolution = Members.inherited(pluginClass, "sceneResolution");
            this.sceneViewport = Members.inherited(pluginClass, "sceneViewport");
            this.actualUiResolution = Members.inherited(pluginClass, "actualUiResolution");
            this.hasLoggedIn = Members.inherited(pluginClass, "hasLoggedIn");
            this.sceneFboValid = sceneFboValid;
            this.sceneContext = sceneContext;
        }

        private static Access of(Class<?> renderer)
        {
            Field plugin = Members.inherited(renderer, "plugin");
            if (plugin == null || Members.inherited(plugin.getType(), "fboScene") == null)
            {
                return null;
            }
            Field sceneFboValid = Members.inherited(renderer, "sceneFboValid");
            Field sceneContext = sceneFboValid == null ? Members.inherited(renderer, "sceneContext") : null;
            if (sceneFboValid == null && sceneContext == null)
            {
                return null;
            }
            Access access = new Access(plugin, sceneFboValid, sceneContext);
            return access.complete() ? access : null;
        }

        private boolean complete()
        {
            return fboSceneResolve != null && sceneResolution != null && sceneViewport != null && actualUiResolution != null && hasLoggedIn != null;
        }

        private boolean sceneShown(Object renderer, Object pluginInstance) throws IllegalAccessException
        {
            if (sceneFboValid != null)
            {
                return sceneFboValid.getBoolean(renderer);
            }
            return hasLoggedIn.getBoolean(pluginInstance) && sceneContext.get(renderer) != null;
        }
    }
}
