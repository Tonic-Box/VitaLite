package com.tonic.mixins;

import com.tonic.Static;
import com.tonic.api.TAngle;
import com.tonic.api.TRasterState;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.MethodHook;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.injector.annotations.Shift;
import com.tonic.services.PickCamera;
import com.tonic.services.SceneScale;
import net.runelite.api.BufferProvider;
import net.runelite.api.Client;

/**
 * Draws the scene from the PickCamera while one is set, so everything the game picks under the mouse is
 * picked down its ray. The game's camera is only borrowed: the draw's caller puts the camera back once
 * the scene is drawn, as it does after shaking it, and the exact camera is worked out afresh each frame.
 * In CPU mode it also draws the scene at the SceneScale size and stretches it over the viewport before
 * anything is drawn on it, with picking scaled to match.
 */
@Mixin("Scene")
public abstract class TSceneMixin
{
    @Shadow("cameraX")
    private static int cameraX;
    @Shadow("cameraY")
    private static int cameraY;
    @Shadow("cameraZ")
    private static int cameraZ;
    @Shadow("cameraExactX")
    private static float cameraExactX;
    @Shadow("cameraExactY")
    private static float cameraExactY;
    @Shadow("cameraExactZ")
    private static float cameraExactZ;
    @Shadow("cameraPitchRadians")
    private static float cameraPitchRadians;
    @Shadow("cameraYawRadians")
    private static float cameraYawRadians;
    @Shadow("cameraPitch")
    private static TAngle cameraPitch;
    @Shadow("cameraYaw")
    private static TAngle cameraYaw;
    @Shadow("mouseCrossState")
    private static int mouseCrossState;
    @Shadow("rasterState")
    private static TRasterState rasterState;
    @Shadow("viewportMouseX")
    private static int viewportMouseX;
    @Shadow("viewportMouseY")
    private static int viewportMouseY;
    @Shadow("pickX")
    private int pickX;
    @Shadow("pickY")
    private int pickY;

    @Inject
    private static boolean sceneScaled;
    @Inject
    private static int sceneZoom;
    @Inject
    private static int sceneMouseX;
    @Inject
    private static int sceneMouseY;

    @Shadow("setRasterArea")
    public abstract void setRasterArea(int left, int top, int right, int bottom);

    @Insert(method = "setPick", at = @At(value = AtTarget.RETURN))
    public void scalePick()
    {
        if (SceneScale.percent() < 100)
        {
            Client client = Static.getClient();
            pickX = SceneScale.scale(pickX, client.getViewportWidth());
            pickY = SceneScale.scale(pickY, client.getViewportHeight());
        }
    }

    @MethodHook("drawScene")
    public void beginScaledScene()
    {
        SceneScale.discardDepth();
        Client client = Static.getClient();
        int width = client.getViewportWidth();
        int height = client.getViewportHeight();
        sceneScaled = SceneScale.percent() < 100 && width > 0 && height > 0;
        if (!sceneScaled)
        {
            return;
        }

        int x = client.getViewportXOffset();
        int y = client.getViewportYOffset();
        sceneZoom = rasterState.getZoom();
        sceneMouseX = viewportMouseX;
        sceneMouseY = viewportMouseY;
        rasterState.setZoom(SceneScale.scale(sceneZoom, width));
        viewportMouseX = SceneScale.scale(sceneMouseX, width);
        viewportMouseY = SceneScale.scale(sceneMouseY, height);
        setRasterArea(x, y, x + SceneScale.scaled(width), y + SceneScale.scaled(height));
    }

    @Insert(method = "drawScene", at = @At(value = AtTarget.INVOKE, owner = "Callbacks", target = "drawScene", shift = Shift.HEAD), all = true)
    public void endScaledScene()
    {
        Client client = Static.getClient();
        if (sceneScaled)
        {
            sceneScaled = false;
            int x = client.getViewportXOffset();
            int y = client.getViewportYOffset();
            int width = client.getViewportWidth();
            int height = client.getViewportHeight();
            setRasterArea(x, y, x + width, y + height);
            rasterState.setZoom(sceneZoom);
            viewportMouseX = sceneMouseX;
            viewportMouseY = sceneMouseY;
            BufferProvider buffer = client.getBufferProvider();
            SceneScale.stretch(buffer.getPixels(), buffer.getWidth(), x, y, width, height);
        }
        SceneScale.latch(client.isGpu());
    }

    @Insert(method = "drawScene", at = @At(value = AtTarget.INVOKE, owner = "Client", target = "updateCamera"))
    public static void pickCameraHook()
    {
        float[] ray = PickCamera.ray();
        if (ray == null)
        {
            return;
        }

        // The game's camera at yaw a looks along (-sin a, 0, cos a), its pitch counting down as y does; 16384 units to a turn
        double unitsPerRadian = 16384 / (2 * Math.PI);
        double yaw = Math.atan2(-ray[3], ray[5]);
        double pitch = Math.atan2(ray[4], Math.hypot(ray[3], ray[5]));
        cameraX = (int) ray[0];
        cameraY = (int) ray[1];
        cameraZ = (int) ray[2];
        cameraExactX = ray[0];
        cameraExactY = ray[1];
        cameraExactZ = ray[2];
        cameraPitch.setUnits((int) Math.round(pitch * unitsPerRadian));
        cameraYaw.setUnits((int) Math.round(yaw * unitsPerRadian));
        cameraPitchRadians = (float) pitch;
        cameraYawRadians = (float) yaw;

        // The pointer shows its own click cross on the world; the game's would sit at the viewport's middle
        mouseCrossState = 0;
    }
}
