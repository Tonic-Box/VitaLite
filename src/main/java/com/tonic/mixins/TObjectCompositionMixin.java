package com.tonic.mixins;

import com.tonic.api.TObjectComposition;
import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import lombok.Getter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

@Getter
@Mixin("ObjectComposition")
public class TObjectCompositionMixin implements TObjectComposition
{
    @Inject
    private int blockAccessFlags;

    @Insert(method = "decode", at = @At(value = AtTarget.LOAD), raw = true)
    public static void captureBlockAccessFlags(ClassNode owner, MethodNode method, AbstractInsnNode ignored)
    {
        for (AbstractInsnNode instruction : method.instructions)
        {
            if (!(instruction instanceof TableSwitchInsnNode))
            {
                continue;
            }

            TableSwitchInsnNode tableSwitch = (TableSwitchInsnNode) instruction;
            if (69 < tableSwitch.min || 69 > tableSwitch.max)
            {
                continue;
            }

            AbstractInsnNode current = tableSwitch.labels.get(69 - tableSwitch.min).getNext();
            while (current != null)
            {
                if (current instanceof MethodInsnNode && current.getOpcode() == Opcodes.INVOKEVIRTUAL)
                {
                    MethodInsnNode invocation = (MethodInsnNode) current;
                    AbstractInsnNode next = current.getNext();
                    while (next != null && next.getOpcode() == -1)
                    {
                        next = next.getNext();
                    }
                    if (invocation.desc.endsWith(")I") && next != null && next.getOpcode() == Opcodes.POP)
                    {
                        InsnList capture = new InsnList();
                        capture.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        capture.add(new InsnNode(Opcodes.SWAP));
                        capture.add(new FieldInsnNode(Opcodes.PUTFIELD, owner.name, "blockAccessFlags", "I"));
                        method.instructions.insertBefore(next, capture);
                        method.instructions.remove(next);
                        return;
                    }
                }

                if (current.getOpcode() == Opcodes.GOTO)
                {
                    break;
                }
                current = current.getNext();
            }
        }

        throw new IllegalStateException("Unable to locate object access-flags decoder");
    }

    @Override
    public int getBlockAccessFlags() {
        return blockAccessFlags;
    }
}
