package com.tonic.injector.util;

import com.tonic.util.asm.InsnUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Makes RuneLite code that uses the local player without checking it return early instead, for the moments the client draws or handles events while the local player is not loaded, such as while a boat's world view is being boarded or left.
 */
public final class LocalPlayerGuard
{
    private static final String CLIENT = "net/runelite/api/Client";
    private static final String GET_LOCAL_PLAYER = "getLocalPlayer";

    private LocalPlayerGuard()
    {
    }

    /**
     * Has the method return right after any read of the local player that gives null: with nothing from a void method, null from one returning an object, and zero from one returning a number.
     *
     * @param method the method
     */
    public static void returnWhenMissing(MethodNode method)
    {
        int guarded = 0;
        int monitors = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray())
        {
            if (instruction.getOpcode() == Opcodes.MONITORENTER)
            {
                monitors++;
            }
            else if (instruction.getOpcode() == Opcodes.MONITOREXIT)
            {
                monitors--;
            }
            if (!(instruction instanceof MethodInsnNode))
            {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!call.owner.equals(CLIENT) || !call.name.equals(GET_LOCAL_PLAYER))
            {
                continue;
            }
            if (!InjectionFailures.require(monitors <= 0, "local player guard in " + method.name + method.desc + ": a read inside a synchronized block, where returning would skip its monitor exit"))
            {
                continue;
            }
            guarded++;

            LabelNode present = new LabelNode();
            InsnList guard = new InsnList();
            guard.add(new InsnNode(Opcodes.DUP));
            guard.add(new JumpInsnNode(Opcodes.IFNONNULL, present));
            guard.add(new InsnNode(Opcodes.POP));
            guard.add(InsnUtil.generateDefaultReturn(method));
            guard.add(present);
            method.instructions.insert(call, guard);
        }
        InjectionFailures.require(guarded > 0, "local player guard in " + method.name + method.desc + ": no read of the local player found");
    }
}
