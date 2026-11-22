package com.tonic.injector.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Checks that a patched class only refers to fields and methods that exist and are reached as static only when they are, so a mixin naming a member its target lost, or injected code naming VitaLite code that changed, fails the injector run rather than the client.
 */
public final class MemberCheck
{
    private static final String VITALITE = "com/tonic/";
    private static final int MISSING = -1;
    private static final int ASSUMED = -2;

    private MemberCheck()
    {
    }

    /**
     * Records every reference in a class to a field or method of that same class that neither it nor the superclasses and interfaces known here declare, or that it reaches as static when it is not or the other way round. A member that may come from a class not known here is taken to exist.
     *
     * @param node the patched class
     * @param classes the classes known here, by internal name
     */
    public static void verify(ClassNode node, Map<String, ClassNode> classes)
    {
        check(node, owner -> owner.equals(node.name), classes::get);
    }

    /**
     * Records every reference in a class to a class, field or method of VitaLite that the client's class loader does not have, or that it reaches as static when it is not or the other way round.
     *
     * @param node the patched class
     */
    public static void verifyVitaLite(ClassNode node)
    {
        check(node, owner -> owner.startsWith(VITALITE), ClientClasses::get);
    }

    private static void check(ClassNode node, Predicate<String> checked, Function<String, ClassNode> classes)
    {
        for (MethodNode method : node.methods)
        {
            for (AbstractInsnNode insn : method.instructions)
            {
                String owner;
                String name;
                String desc;
                boolean isMethod = insn instanceof MethodInsnNode;
                if (isMethod)
                {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    owner = call.owner;
                    name = call.name;
                    desc = call.desc;
                }
                else if (insn instanceof FieldInsnNode)
                {
                    FieldInsnNode field = (FieldInsnNode) insn;
                    owner = field.owner;
                    name = field.name;
                    desc = field.desc;
                }
                else
                {
                    continue;
                }
                if (!checked.test(owner))
                {
                    continue;
                }

                String where = node.name + "." + method.name + method.desc;
                ClassNode type = owner.equals(node.name) ? node : classes.apply(owner);
                if (type == null)
                {
                    InjectionFailures.record(where + " refers to missing class " + owner);
                    continue;
                }
                String member = (isMethod ? "method " : "field ") + owner + "." + name + (isMethod ? "" : " ") + desc;
                int access = access(type, name, desc, isMethod, classes);
                boolean asStatic = insn.getOpcode() == Opcodes.INVOKESTATIC || insn.getOpcode() == Opcodes.GETSTATIC || insn.getOpcode() == Opcodes.PUTSTATIC;
                if (access == MISSING)
                {
                    InjectionFailures.record(where + " refers to missing " + member);
                }
                else if (access != ASSUMED && ((access & Opcodes.ACC_STATIC) != 0) != asStatic)
                {
                    InjectionFailures.record(where + " reaches " + member + (asStatic ? " as static, but it is not" : " as an instance member, but it is static"));
                }
            }
        }
    }

    private static int access(ClassNode type, String name, String desc, boolean method, Function<String, ClassNode> classes)
    {
        if (method)
        {
            for (MethodNode candidate : type.methods)
            {
                if (candidate.name.equals(name) && candidate.desc.equals(desc))
                {
                    return candidate.access;
                }
            }
        }
        else
        {
            for (FieldNode candidate : type.fields)
            {
                if (candidate.name.equals(name) && candidate.desc.equals(desc))
                {
                    return candidate.access;
                }
            }
        }

        if (type.superName != null)
        {
            int inherited = inherited(type.superName, name, desc, method, classes);
            if (inherited != MISSING)
            {
                return inherited;
            }
        }
        for (String face : type.interfaces)
        {
            int inherited = inherited(face, name, desc, method, classes);
            if (inherited != MISSING)
            {
                return inherited;
            }
        }
        return MISSING;
    }

    private static int inherited(String owner, String name, String desc, boolean method, Function<String, ClassNode> classes)
    {
        ClassNode known = classes.apply(owner);
        if (known != null)
        {
            return access(known, name, desc, method, classes);
        }
        if (!owner.equals("java/lang/Object"))
        {
            return ASSUMED;
        }
        if (!method)
        {
            return MISSING;
        }
        for (Method candidate : Object.class.getDeclaredMethods())
        {
            if (candidate.getName().equals(name) && Type.getMethodDescriptor(candidate).equals(desc))
            {
                return candidate.getModifiers();
            }
        }
        return MISSING;
    }
}
