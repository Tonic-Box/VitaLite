package com.tonic.mixins;

import com.tonic.api.TAngle;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.services.PickCamera;

/**
 * Draws the scene from the PickCamera while one is set, so everything the game picks under the mouse is
 * picked down its ray. The game's camera is only borrowed: the draw's caller puts the camera back once
 * the scene is drawn, as it does after shaking it, and the exact camera is worked out afresh each frame.
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
