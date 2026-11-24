package com.tonic.mixins;

import com.tonic.api.TBuffer;
import com.tonic.api.TObjectComposition;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.InjectionFailures;
import com.tonic.util.MappingProvider;
import com.tonic.util.asm.BytecodeBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

/**
 * Keeps what the client reads from an object definition and does not hold on to: the approach-side mask (opcode 69), and the interact type (opcodes 17 and 27, default 2) and hollow flag (opcode 74) that decide whether it collides
 */
@Mixin("ObjectComposition")
public abstract class TObjectCompositionMixin implements TObjectComposition
{
    private static final int OPCODE_NOT_SOLID = 17;
    private static final int OPCODE_SOLID_ONE = 27;
    private static final int OPCODE_ACCESS_FLAGS = 69;
    private static final int OPCODE_HOLLOW = 74;

    @Inject
    private int blockAccessFlags;

    @Inject
    private int decodedInteractType;

    @Inject
    private boolean hollow;

    /**
     * Inserts a call at the decoder's cases for opcodes 17, 27, 69 and 74, before each runs
     * @param method the definition's decoding constructor
     * @param insertionPoint unused, the switch cases are found instead
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
            if (opcodeSwitch.min > OPCODE_NOT_SOLID || opcodeSwitch.max < OPCODE_HOLLOW)
                continue;

            InsnList accessFlags = BytecodeBuilder.create()
                    .pushThis()
                    .loadLocal(1, Opcodes.ALOAD)
                    .invokeVirtual(owner, "decodeNext", "(Lcom/tonic/api/TBuffer;)V")
                    .build();
            insertAt(opcodeSwitch, OPCODE_ACCESS_FLAGS, method, accessFlags);
            insertAt(opcodeSwitch, OPCODE_NOT_SOLID, method, BytecodeBuilder.create().pushThis().invokeVirtual(owner, "decodeNotSolid", "()V").build());
            insertAt(opcodeSwitch, OPCODE_SOLID_ONE, method, BytecodeBuilder.create().pushThis().invokeVirtual(owner, "decodeSolidOne", "()V").build());
            insertAt(opcodeSwitch, OPCODE_HOLLOW, method, BytecodeBuilder.create().pushThis().invokeVirtual(owner, "decodeHollow", "()V").build());
            return;
        }
        InjectionFailures.require(false, "ObjectComposition decoder: no opcode switch covering " + OPCODE_NOT_SOLID + ".." + OPCODE_HOLLOW);
    }

    private static void insertAt(TableSwitchInsnNode opcodeSwitch, int opcode, MethodNode method, InsnList code)
    {
        LabelNode label = opcodeSwitch.labels.get(opcode - opcodeSwitch.min);
        int shared = 0;
        for (LabelNode other : opcodeSwitch.labels)
        {
            if (other == label)
                shared++;
        }
        if (!InjectionFailures.require(label != opcodeSwitch.dflt && shared == 1, "ObjectComposition decoder: opcode " + opcode + " has no case of its own"))
            return;
        method.instructions.insert(label, code);
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

    /**
     * Notes opcode 17, which gives the definition interact type 0
     */
    @Inject
    public void decodeNotSolid()
    {
        decodedInteractType = 1;
    }

    /**
     * Notes opcode 27, which gives the definition interact type 1
     */
    @Inject
    public void decodeSolidOne()
    {
        decodedInteractType = 2;
    }

    /**
     * Notes opcode 74, which makes the definition hollow
     */
    @Inject
    public void decodeHollow()
    {
        hollow = true;
    }

    @Override
    public int getBlockAccessFlags()
    {
        return blockAccessFlags;
    }

    @Override
    public int getInteractType()
    {
        if (hollow)
            return 0;
        return decodedInteractType == 0 ? 2 : decodedInteractType - 1;
    }
}
