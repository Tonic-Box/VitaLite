package com.tonic.mixins;

import com.tonic.api.TMenu;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.injector.util.Gates;
import com.tonic.injector.util.StreamerBridges;
import com.tonic.services.streamer.StreamerMenu;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The client's right-click menu, reporting its draws and layouts to StreamerMenu while streamer mode is on, so the streamer mirror can show it with only stock entries.
 */
@Mixin("Menu")
public abstract class TMenuMixin implements TMenu
{
    @Override
    @Shadow("draw")
    public abstract void drawMenu();

    @Override
    @Shadow("layout")
    public abstract void layoutMenu(int x, int y);

    /**
     * Reports that the menu is about to draw.
     */
    @Insert(method = "draw", at = @At(value = AtTarget.HEAD), gate = StreamerBridges.STREAMER_MIRRORING)
    public void streamerMenuDrawing()
    {
        StreamerMenu.drawing(this);
    }

    /**
     * Reports that the menu has drawn.
     */
    @Insert(method = "draw", at = @At(value = AtTarget.RETURN), all = true, gate = StreamerBridges.STREAMER_MIRRORING)
    public void streamerMenuDrawn()
    {
        StreamerMenu.drawn(this);
    }

    /**
     * Reports the menu being laid out.
     *
     * @param x where it is opened, across the canvas
     * @param y where it is opened, down the canvas
     */
    @Insert(method = "layout", at = @At(value = AtTarget.HEAD), gate = StreamerBridges.STREAMER_ACTIVE)
    public void streamerMenuLaidOut(int x, int y)
    {
        StreamerMenu.laidOut(this, x, y);
    }

    /**
     * Reports each entry created through the client's or a menu's createMenuEntry as it is returned.
     *
     * @param method Menu.createEntry(int)
     * @param insertionPoint a return of the entry
     */
    @Insert(method = "createEntry", at = @At(value = AtTarget.RETURN), all = true, raw = true)
    public static void streamerEntryCreated(MethodNode method, AbstractInsnNode insertionPoint)
    {
        InsnList report = new InsnList();
        report.add(new InsnNode(Opcodes.DUP));
        report.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/tonic/services/streamer/StreamerMenu", "created", "(Ljava/lang/Object;)V", false));
        method.instructions.insertBefore(insertionPoint, Gates.around(StreamerBridges.STREAMER_ACTIVE, report));
    }
}
