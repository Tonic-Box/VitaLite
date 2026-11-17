package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Stops the loot tracker's chat handler throwing when a game message arrives while the local player is not loaded,
 * such as while the world view of a boat being boarded or left is not there yet.
 */
@Mixin("net/runelite/client/plugins/loottracker/LootTrackerPlugin")
public class LootTrackerPluginMixin
{
    private static final String CLIENT = "net/runelite/api/Client";

    @Insert(
            method = "onChatMessage",
            at = @At(
                    value = AtTarget.INVOKE,
                    owner = CLIENT,
                    target = "getLocalPlayer()Lnet/runelite/api/Player;"
            ),
            raw = true
    )
    public static void skipWithoutLocalPlayer(MethodNode method, AbstractInsnNode insertionPoint)
    {
        for (AbstractInsnNode instruction : method.instructions.toArray())
        {
            if (!(instruction instanceof MethodInsnNode))
            {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!call.owner.equals(CLIENT) || !call.name.equals("getLocalPlayer"))
            {
                continue;
            }

            LabelNode present = new LabelNode();
            InsnList guard = new InsnList();
            guard.add(new InsnNode(Opcodes.DUP));
            guard.add(new JumpInsnNode(Opcodes.IFNONNULL, present));
            guard.add(new InsnNode(Opcodes.POP));
            guard.add(new InsnNode(Opcodes.RETURN));
            guard.add(present);
            method.instructions.insert(call, guard);
        }
    }
}
