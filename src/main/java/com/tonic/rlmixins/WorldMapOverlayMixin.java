package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Redirect;
import com.tonic.injector.annotations.Slice;
import com.tonic.injector.util.Gates;
import com.tonic.injector.util.StreamerBridges;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;

/**
 * Draws the icons and tooltips of world map points that are not stock for the real view only while streamer mode is on, through MirrorWorldMap.
 */
@Mixin("net/runelite/client/ui/overlay/worldmap/WorldMapOverlay")
public class WorldMapOverlayMixin
{
    private static final String POINT = "net/runelite/client/ui/overlay/worldmap/WorldMapPoint";
    private static final String RENDER = "(Ljava/awt/Graphics2D;)Ljava/awt/Dimension;";
    private static final String TOOLTIP = "(Ljava/awt/Graphics2D;L" + POINT + ";)V";
    private static final String MIRROR_WORLD_MAP = "com/tonic/services/streamer/MirrorWorldMap";

    /**
     * Notes the point whose icon is read, so its draw knows which point it is.
     *
     * @param method WorldMapOverlay.render(Graphics2D)
     * @param insertionPoint the point's getImage call
     */
    @Insert(method = "render", desc = RENDER, at = @At(value = AtTarget.INVOKE, owner = POINT, target = "getImage"), expectedMatches = 1, raw = true)
    public static void streamerPointImage(MethodNode method, AbstractInsnNode insertionPoint)
    {
        InsnList note = new InsnList();
        note.add(new InsnNode(Opcodes.DUP));
        note.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MIRROR_WORLD_MAP, "point", "(L" + POINT + ";)V", false));
        method.instructions.insertBefore(insertionPoint, Gates.around(StreamerBridges.STREAMER_MIRRORING, note));
    }

    /**
     * Draws a point's icon through MirrorWorldMap, which keeps hidden points' icons out of the mirror.
     *
     * @param graphics the overlay's graphics
     * @param image the icon
     * @param x where to draw it
     * @param y where to draw it
     * @param observer the image observer
     * @return whether the image was drawn completely
     */
    @Redirect(method = "render", desc = RENDER, at = @At(value = AtTarget.INVOKE, owner = "java/awt/Graphics2D", target = "drawImage(Ljava/awt/Image;IILjava/awt/image/ImageObserver;)Z"), slice = @Slice(from = @At(value = AtTarget.INVOKE, owner = POINT, target = "getImage")), expectedMatches = 1, gate = StreamerBridges.STREAMER_MIRRORING)
    @Delegate(MIRROR_WORLD_MAP + ".drawImage")
    public static native boolean streamerPointIcon(Graphics2D graphics, Image image, int x, int y, ImageObserver observer);

    /**
     * Starts a point's tooltip with the graphics MirrorWorldMap gives.
     *
     * @param graphics the overlay's graphics
     * @param point the point
     * @return the graphics to draw the tooltip with
     */
    @Insert(method = "drawTooltip", desc = TOOLTIP, at = @At(value = AtTarget.HEAD), gate = StreamerBridges.STREAMER_MIRRORING, storeIntoParameter = 0)
    @Delegate(MIRROR_WORLD_MAP + ".tooltip")
    public static native Graphics2D streamerTooltipBegin(Graphics2D graphics, Object point);

    /**
     * Ends a point's tooltip at each return.
     */
    @Insert(method = "drawTooltip", desc = TOOLTIP, at = @At(value = AtTarget.RETURN), all = true, gate = StreamerBridges.STREAMER_MIRRORING)
    @Delegate("com/tonic/services/streamer/MirrorHiding.end")
    public static native void streamerTooltipEnd();
}
