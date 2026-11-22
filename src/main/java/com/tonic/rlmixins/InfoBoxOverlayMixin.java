package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.Gates;
import com.tonic.injector.util.StreamerBridges;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Passes the boxes an InfoBox panel draws through MirrorInfoBoxes while streamer mode is on, so the panel can be drawn for the mirror with only its stock boxes.
 */
@Mixin("net/runelite/client/ui/overlay/infobox/InfoBoxOverlay")
public class InfoBoxOverlayMixin
{
    private static final String INFO_BOX_OVERLAY = "net/runelite/client/ui/overlay/infobox/InfoBoxOverlay";

    /**
     * Passes each read of the panel's boxes through MirrorInfoBoxes while streamer mode is on.
     *
     * @param method InfoBoxOverlay.render(Graphics2D)
     * @param insertionPoint a read of the boxes
     */
    @Insert(
            method = "render",
            desc = "(Ljava/awt/Graphics2D;)Ljava/awt/Dimension;",
            at = @At(value = AtTarget.GETFIELD, owner = INFO_BOX_OVERLAY, target = "infoBoxes"),
            all = true,
            raw = true
    )
    public static void filterBoxes(MethodNode method, AbstractInsnNode insertionPoint)
    {
        InsnList filter = new InsnList();
        filter.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/tonic/services/streamer/MirrorInfoBoxes", "boxes", "(Ljava/util/List;)Ljava/util/List;", false));
        method.instructions.insert(insertionPoint, Gates.around(StreamerBridges.STREAMER_ACTIVE, filter));
    }
}
