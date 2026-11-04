package com.tonic.mixins;

import com.tonic.api.TPlayerComposition;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.util.MappingProvider;
import com.tonic.util.asm.BytecodeBuilder;
import net.runelite.api.ModelData;
import net.runelite.api.PlayerComposition;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Arrays;

/**
 * Records where each equipment slot's faces start as the player's model is merged from its parts, a kit or a worn
 * item for each slot in slot order, so a slot's faces can be told apart in the model drawn.
 */
@Mixin("PlayerComposition")
public abstract class TPlayerCompositionMixin implements TPlayerComposition
{
    private static final int SLOTS = 12;

    @Inject
    private int[] partSlots;
    @Inject
    private int partCount;
    @Inject
    private int[] slotFaces;
    @Inject
    private int[] slotFacesEquipment;

    @Insert(method = "buildModelData", at = @At(value = AtTarget.RETURN), raw = true)
    public static void buildModelDataHook(MethodNode method, AbstractInsnNode insertionPoint)
    {
        String owner = MappingProvider.getClass("PlayerComposition").getObfuscatedName();
        int partsLocal = -1;
        int slotLocal = -1;
        MethodInsnNode merge = null;
        for (AbstractInsnNode insn : method.instructions.toArray())
        {
            if (insn.getOpcode() == Opcodes.ANEWARRAY && partsLocal == -1 && insn.getNext().getOpcode() == Opcodes.ASTORE)
            {
                partsLocal = ((VarInsnNode) insn.getNext()).var;
            }
            else if (insn.getOpcode() == Opcodes.IALOAD && slotLocal == -1 && isLoad(insn.getPrevious(), Opcodes.ILOAD, -1) && isLoad(insn.getPrevious().getPrevious(), Opcodes.ALOAD, 1))
            {
                slotLocal = ((VarInsnNode) insn.getPrevious()).var;
            }
            else if (insn.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) insn).name.equals("<init>") && ((MethodInsnNode) insn).desc.startsWith("([L"))
            {
                merge = (MethodInsnNode) insn;
            }
        }
        if (partsLocal == -1 || slotLocal == -1 || merge == null)
        {
            System.err.println("TPlayerCompositionMixin: buildModelData did not match, slot faces stay unknown");
            return;
        }

        // Each part stored is noted with the slot it came from, and the parts' faces are counted as they merge
        for (AbstractInsnNode insn : method.instructions.toArray())
        {
            if (insn.getOpcode() == Opcodes.AASTORE)
            {
                method.instructions.insert(insn, BytecodeBuilder.create()
                        .pushThis()
                        .loadLocal(slotLocal, Opcodes.ILOAD)
                        .invokeVirtual(owner, "partAdded", "(I)V")
                        .build());
            }
        }
        method.instructions.insertBefore(merge, BytecodeBuilder.create()
                .pushThis()
                .loadLocal(partsLocal, Opcodes.ALOAD)
                .loadLocal(1, Opcodes.ALOAD)
                .invokeVirtual(owner, "partsMerging", "([Ljava/lang/Object;[I)V")
                .build());
        method.instructions.insert(BytecodeBuilder.create()
                .pushThis()
                .invokeVirtual(owner, "partsStarting", "()V")
                .build());
    }

    private static boolean isLoad(AbstractInsnNode insn, int opcode, int local)
    {
        return insn instanceof VarInsnNode && insn.getOpcode() == opcode && (local == -1 || ((VarInsnNode) insn).var == local);
    }

    @Inject
    public void partsStarting()
    {
        if (partSlots == null)
        {
            partSlots = new int[SLOTS];
        }
        partCount = 0;
    }

    @Inject
    public void partAdded(int slot)
    {
        if (partCount < partSlots.length)
        {
            partSlots[partCount] = slot;
        }
        partCount++;
    }

    @Inject
    public void partsMerging(Object[] parts, int[] equipment)
    {
        if (partCount > SLOTS)
        {
            slotFaces = null;
            return;
        }
        int[] starts = new int[SLOTS + 1];
        int faces = 0;
        int part = 0;
        for (int slot = 0; slot < SLOTS; slot++)
        {
            starts[slot] = faces;
            while (part < partCount && partSlots[part] == slot)
            {
                faces += ((ModelData) parts[part]).getFaceCount();
                part++;
            }
        }
        starts[SLOTS] = faces;
        slotFaces = starts;
        slotFacesEquipment = equipment.clone();
    }

    @Inject
    @Override
    public int[] getSlotFaces()
    {
        return slotFaces != null && Arrays.equals(slotFacesEquipment, ((PlayerComposition) this).getEquipmentIds()) ? slotFaces.clone() : null;
    }
}
