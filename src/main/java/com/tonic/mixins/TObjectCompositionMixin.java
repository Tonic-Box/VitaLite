package com.tonic.mixins;

import com.tonic.api.TBuffer;
import com.tonic.api.TObjectComposition;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;

import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import com.tonic.util.MappingProvider;
import com.tonic.util.asm.BytecodeBuilder;
import lombok.Getter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

@Getter
@Mixin("ObjectComposition")
public class TObjectCompositionMixin implements TObjectComposition
{
    @Inject
    private static int blockAccessFlags;

    @Insert(method = "<init>", at = @At(value = AtTarget.RETURN), raw = true)
    public static void decodeHook(MethodNode method, AbstractInsnNode insertionPoint)
    {
        String owner = MappingProvider.getClass("ObjectComposition").getObfuscatedName();
        for (AbstractInsnNode insn : method.instructions.toArray())
        {
            if (insn.getOpcode() != Opcodes.TABLESWITCH)
                continue;

            TableSwitchInsnNode opcodeSwitch = (TableSwitchInsnNode) insn;
            if (opcodeSwitch.min > 69 || opcodeSwitch.max < 69)
                continue;

            InsnList code = BytecodeBuilder.create()
                    .loadLocal(1, Opcodes.ALOAD)
                    .invokeStatic(owner, "decodeNext", "(Lcom/tonic/api/TBuffer;)V")
                    .build();
            method.instructions.insert(opcodeSwitch.labels.get(69 - opcodeSwitch.min), code);
            break;
        }
    }

    @Inject
    public static void decodeNext(TBuffer buffer)
    {
        byte[] array = buffer.getArray();
        int offset = buffer.getOffset();
        blockAccessFlags = array[offset] & 0xFF;
    }

    @Override
    public int getBlockAccessFlags() {
        return blockAccessFlags;
    }
}
