package com.tonic.mixins;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.TPacketBuffer;
import com.tonic.api.TPacketWriter;
import com.tonic.api.TServerPacket;
import com.tonic.events.PacketReceived;
import com.tonic.injector.annotations.*;
import com.tonic.injector.util.BytecodeBuilder;
import com.tonic.injector.util.MappingProvider;
import com.tonic.model.ui.VitaLiteOptionsPanel;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JField;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
@Mixin("Client")
public class TServerPacketLoggerMixin
{
    @Insert(
            method = "processServerPacket",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void process(MethodNode method, AbstractInsnNode insertionPoint)
    {
        JClass clazz = MappingProvider.getClass("PacketWriter");
        if(clazz == null)
            return;
        JField field = MappingProvider.getField(clazz, "serverPacket");

        AbstractInsnNode target = null;
        for(AbstractInsnNode node : method.instructions)
        {
            if(node.getOpcode() != Opcodes.GETFIELD)
                continue;

            FieldInsnNode fin = (FieldInsnNode) node;
            if(!fin.name.equals(field.getObfuscatedName()) || !fin.owner.equals(clazz.getObfuscatedName()))
                continue;

            if(fin.getNext().getOpcode() != Opcodes.GETSTATIC)
                continue;

            if(fin.getNext().getNext().getOpcode() != Opcodes.IF_ACMPNE)
                continue;

            target = fin;
            break;
        }

        if(target == null)
            return;

        InsnList code = BytecodeBuilder.create()
                .invokeStatic(
                        "client",
                        "process",
                        "()V"
                ).build();
        method.instructions.insertBefore(target, code);
    }

    @Inject
    public static void process()
    {
        TClient client = Static.getClient();
        TPacketWriter writer = client.getPacketWriter();
        TServerPacket packet = writer.getServerPacket();
        TPacketBuffer buffer = writer.getServerPacketBuffer();
        byte[] bytes = new byte[writer.getServerPacketLength()];
        System.arraycopy(buffer.getArray(), 0, bytes, 0, writer.getServerPacketLength());
        int id = packet.getId();
        int length = writer.getServerPacketLength();
        PacketReceived packetReceived = PacketReceived.of(id, length, bytes);
        Static.post(packetReceived);
        VitaLiteOptionsPanel.getInstance().onPacketReceived(packetReceived);
    }
}
