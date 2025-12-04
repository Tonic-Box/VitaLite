package com.tonic.util.asm;

import com.tonic.vitalite.Main;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

/**
 * This class is responsible for mapping any cert checks. for us to respect.
 */
public class SignerMapper
{
    private static final Set<String> blacklist = new HashSet<>();

    public static boolean shouldIgnore(String className)
    {
        return blacklist.contains(className);
    }

    public static void map()
    {
        blacklist.add("net.runelite.api.hooks.Callbacks");
        blacklist.add("net.runelite.client.callback.Hooks");
        for(var entry : Main.LIBS.getRunelite().classes.entrySet())
        {
            ClassNode node = ClassNodeUtil.toNode(entry.getValue());
            for(MethodNode mn : node.methods)
            {
                scan(mn);
            }
        }

        for(var entry : Main.LIBS.getGamepack().classes.entrySet())
        {
            ClassNode node = ClassNodeUtil.toNode(entry.getValue());
            for(MethodNode mn : node.methods)
            {
                scan(mn);
            }
        }

        for(var entry : Main.LIBS.getOther().classes.entrySet())
        {
            ClassNode node = ClassNodeUtil.toNode(entry.getValue());
            for(MethodNode mn : node.methods)
            {
                scan(mn);
            }
        }
    }
    private static void scan(MethodNode mn)
    {
        AbstractInsnNode target;
        for(AbstractInsnNode insn : mn.instructions)
        {
            if(insn.getOpcode() != INVOKEVIRTUAL)
                continue;

            MethodInsnNode min = (MethodInsnNode) insn;
            if(min.owner.equals("java/lang/Class") && min.name.equals("getSigners"))
            {
                target = min.getPrevious().getPrevious();
                if(target instanceof FieldInsnNode)
                {
                    FieldInsnNode fin = (FieldInsnNode) target;
                    String clazz = fin.desc.replace("L", "").replace(";", "").replace("/", ".");
                    blacklist.add(clazz);
                }
            }
        }
    }
}
