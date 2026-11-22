package com.tonic.mixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Reports each chat message added through the client's addChatMessage to StreamerChat, so the streamer mirror can tell who added it.
 */
@Mixin("Client")
public class TChatMessageMixin
{
    private static final String STREAMER_CHAT = "com/tonic/services/streamer/StreamerChat";

    /**
     * Hands the added message to StreamerChat as it is returned.
     *
     * @param method Client.addChatMessage(ChatMessageType, String, String, String, boolean)
     * @param insertionPoint a return of the message
     */
    @Insert(method = "addChatMessage", at = @At(value = AtTarget.RETURN), all = true, raw = true)
    public static void reportAdded(MethodNode method, AbstractInsnNode insertionPoint)
    {
        InsnList report = new InsnList();
        report.add(new InsnNode(Opcodes.DUP));
        report.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STREAMER_CHAT, "added", "(Ljava/lang/Object;)V", false));
        method.instructions.insertBefore(insertionPoint, report);
    }
}
