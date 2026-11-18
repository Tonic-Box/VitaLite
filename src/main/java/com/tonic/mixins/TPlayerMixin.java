package com.tonic.mixins;

import com.tonic.injector.annotations.ClassMod;
import com.tonic.injector.annotations.Mixin;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Makes a player whose name is not decoded yet, such as one reported dead in the same update that brings their
 * appearance, answer false to the friend, friends chat and clan checks instead of throwing.
 */
@Mixin("Player")
public class TPlayerMixin
{
    @ClassMod
    public static void guardUnnamed(ClassNode player)
    {
        for (MethodNode method : player.methods)
        {
            boolean check = method.name.equals("isFriend") || method.name.equals("isFriendsChatMember") || method.name.equals("isClanMember");
            if (!check || !method.desc.equals("()Z"))
            {
                continue;
            }

            LabelNode named = new LabelNode();
            InsnList guard = new InsnList();
            guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
            guard.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, player.name, "getName", "()Ljava/lang/String;", false));
            guard.add(new JumpInsnNode(Opcodes.IFNONNULL, named));
            guard.add(new InsnNode(Opcodes.ICONST_0));
            guard.add(new InsnNode(Opcodes.IRETURN));
            guard.add(named);
            method.instructions.insert(guard);
        }
    }
}