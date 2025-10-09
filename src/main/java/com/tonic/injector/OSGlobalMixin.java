package com.tonic.injector;

import com.tonic.injector.util.BytecodeBuilder;
import com.tonic.injector.util.MappingProvider;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JField;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class OSGlobalMixin
{
    public static void patch(ClassNode classNode)
    {
        for(MethodNode method : classNode.methods)
        {
            randomDat(classNode, method);
            mouseFlag(method);
        }
    }

    public static void mouseFlag(MethodNode method)
    {
        JClass client = MappingProvider.getClass("Client");
        JField mouseFlag = MappingProvider.getField(client, "mouseFlag");

        List<FieldInsnNode> toReplace = new ArrayList<>();

        for(AbstractInsnNode insn : method.instructions)
        {
            if(insn.getOpcode() != Opcodes.GETSTATIC)
                continue;

            FieldInsnNode fin = (FieldInsnNode) insn;
            if(!fin.owner.equals(mouseFlag.getOwnerObfuscatedName()) || !fin.name.equals(mouseFlag.getObfuscatedName()))
                continue;

            toReplace.add(fin);
        }

        if(toReplace.isEmpty())
            return;

        for (FieldInsnNode insn : toReplace) {
            InsnNode iconst0 = new InsnNode(Opcodes.ICONST_0);
            method.instructions.set(insn, iconst0);
        }
    }

    public static void randomDat(ClassNode clazz, MethodNode method)
    {
        JClass client = MappingProvider.getClass("Client");
        JField randomDat = MappingProvider.getField(client, "randomDat");
        AbstractInsnNode target = null;
        for(AbstractInsnNode insn : method.instructions)
        {
            if (!(insn instanceof FieldInsnNode))
                continue;
            FieldInsnNode fin = (FieldInsnNode) insn;
            if(!fin.owner.equals(randomDat.getOwnerObfuscatedName()) || !fin.name.equals(randomDat.getObfuscatedName()))
                continue;

            if(insn.getNext().getOpcode() != Opcodes.IFNULL)
            {
                if(insn.getPrevious().getOpcode() != Opcodes.ACONST_NULL || !(insn.getNext() instanceof JumpInsnNode))
                    continue;
                if(insn.getNext().getOpcode() == Opcodes.GOTO)
                    continue;
                target = insn;
                break;
            }
            target = insn;
            break;
        }

        if(target == null)
        {
            return;
        }

        InsnList code = BytecodeBuilder.create()
                .pushString(clazz.name + "." + method.name + method.desc)
                .invokeStatic("client", "setRandomDat", "(Ljava/lang/String;)V")
                .build();

        method.instructions.insertBefore(target, code);
    }
}
