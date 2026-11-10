package com.tonic.mixins;

import com.tonic.api.TBuffer;
import com.tonic.api.TObjectComposition;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.util.MappingProvider;
import com.tonic.util.asm.BytecodeBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

/**
 * Keeps each object definition's approach-side mask (opcode 69), which the client reads and discards
 */
@Mixin("ObjectComposition")
public abstract class TObjectCompositionMixin implements TObjectComposition
{
    @Inject
    private int blockAccessFlags;

    /**
     * Inserts a call to decodeNext where the decoder handles opcode 69, before the client reads its byte
     * @param method the definition's decoding constructor
     * @param insertionPoint unused, the switch case is found instead
     */
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
                    .pushThis()
                    .loadLocal(1, Opcodes.ALOAD)
                    .invokeVirtual(owner, "decodeNext", "(Lcom/tonic/api/TBuffer;)V")
                    .build();
            method.instructions.insert(opcodeSwitch.labels.get(69 - opcodeSwitch.min), code);
            break;
        }
    }

    /**
     * Stores the opcode 69 byte the buffer is about to read
     * @param buffer the definition buffer, positioned at the byte
     */
    @Inject
    public void decodeNext(TBuffer buffer)
    {
        blockAccessFlags = buffer.getArray()[buffer.getOffset()] & 0xFF;
    }

    @Override
    public int getBlockAccessFlags()
    {
        return blockAccessFlags;
    }
}
