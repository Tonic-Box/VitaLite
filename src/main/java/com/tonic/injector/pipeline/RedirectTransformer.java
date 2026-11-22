package com.tonic.injector.pipeline;

import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Redirect;
import com.tonic.injector.types.InstructionMatcher;
import com.tonic.injector.util.AnnotationUtil;
import com.tonic.injector.util.Gates;
import com.tonic.injector.util.InjectionFailures;
import com.tonic.injector.util.TransformerUtil;
import com.tonic.util.MappingProvider;
import com.tonic.util.dto.JClass;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Replaces the calls a Redirect matches with calls to its static hook, copied into the mixin's target class.
 */
public final class RedirectTransformer
{
    private static final String EVERY_METHOD = "*";

    private RedirectTransformer()
    {
    }

    /**
     * Applies a Redirect, recording a failure when its target or calls are missing or its hook does not fit them.
     *
     * @param mixin the mixin class
     * @param method the hook, annotated with Redirect
     */
    public static void patch(ClassNode mixin, MethodNode method)
    {
        String hook = "@Redirect " + mixin.name + "." + method.name;
        try
        {
            Redirect redirect = AnnotationUtil.annotation(method, Redirect.class);
            if ((method.access & Opcodes.ACC_STATIC) == 0)
            {
                InjectionFailures.record(hook + ": the hook must be static");
                return;
            }
            String targetName = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
            boolean gamepack = !targetName.contains("/");
            JClass mapping = gamepack ? MappingProvider.getClass(targetName) : null;
            ClassNode target = TransformerUtil.getBaseClass(mixin);
            MethodNode copied = InjectTransformer.patch(target, mixin, method);

            List<MethodNode> searched = new ArrayList<>();
            if (redirect.method().equals(EVERY_METHOD))
            {
                for (MethodNode candidate : target.methods)
                {
                    if (candidate != copied && !declaredIn(mixin, candidate))
                    {
                        searched.add(candidate);
                    }
                }
            }
            else
            {
                MethodNode single = TransformerUtil.requireTargetMethod(hook, mixin, redirect.method(), redirect.desc());
                if (single == null)
                {
                    return;
                }
                searched.add(single);
            }

            List<Map.Entry<MethodNode, MethodInsnNode>> calls = new ArrayList<>();
            for (MethodNode searchedMethod : searched)
            {
                for (InstructionMatcher.MatchResult match : InstructionMatcher.findMatches(searchedMethod, redirect.at(), redirect.slice(), mapping, gamepack))
                {
                    if (!(match.instruction instanceof MethodInsnNode))
                    {
                        InjectionFailures.record(hook + ": matched " + match.instruction + ", which is not a call");
                        return;
                    }
                    calls.add(Map.entry(searchedMethod, (MethodInsnNode) match.instruction));
                }
            }

            List<Map.Entry<MethodNode, MethodInsnNode>> chosen = TransformerUtil.select(hook, calls, redirect.expectedMatches(), redirect.all(), redirect.ordinal());
            if (chosen == null)
            {
                return;
            }
            for (Map.Entry<MethodNode, MethodInsnNode> call : chosen)
            {
                redirect(call.getKey(), call.getValue(), target.name, method, redirect.gate());
            }
            System.out.println("Successfully redirected " + chosen.size() + " call(s) to " + method.name);
        }
        catch (Exception | LinkageError e)
        {
            InjectionFailures.record(hook + ": " + e);
            e.printStackTrace();
        }
    }

    private static boolean declaredIn(ClassNode mixin, MethodNode method)
    {
        for (MethodNode declared : mixin.methods)
        {
            if (declared.name.equals(method.name) && declared.desc.equals(method.desc))
            {
                return true;
            }
        }
        return false;
    }

    private static String misfit(MethodInsnNode call, String hookDesc)
    {
        List<Type> passed = new ArrayList<>();
        if (call.getOpcode() != Opcodes.INVOKESTATIC)
        {
            passed.add(Type.getObjectType(call.owner));
        }
        passed.addAll(List.of(Type.getArgumentTypes(call.desc)));
        Type[] taken = Type.getArgumentTypes(hookDesc);
        if (taken.length != passed.size())
        {
            return "the hook takes " + taken.length + " arguments but " + call.owner + "." + call.name + call.desc + " passes " + passed.size();
        }
        for (int i = 0; i < taken.length; i++)
        {
            if (!fits(passed.get(i), taken[i]))
            {
                return "argument " + i + " of " + call.owner + "." + call.name + call.desc + " is " + passed.get(i) + " but the hook takes " + taken[i];
            }
        }
        Type returned = Type.getReturnType(call.desc);
        if (!fits(returned, Type.getReturnType(hookDesc)))
        {
            return "the hook returns " + Type.getReturnType(hookDesc) + " but " + call.name + call.desc + " returns " + returned;
        }
        return null;
    }

    private static boolean fits(Type a, Type b)
    {
        return reference(a) ? reference(b) : a.equals(b);
    }

    private static boolean reference(Type type)
    {
        return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
    }

    /**
     * Replaces a call with a call to a static hook that takes the call's receiver first for an instance call.
     *
     * @param owner the method holding the call
     * @param call the call
     * @param hookOwner the hook's class
     * @param hook the hook
     * @param gate a static boolean field, as "owner/Class.field", that must be true for the hook to run, the original call running otherwise; empty for none
     * @throws IllegalArgumentException if the hook does not take what the call passes or return what it returns
     */
    public static void redirect(MethodNode owner, MethodInsnNode call, String hookOwner, MethodNode hook, String gate)
    {
        String misfit = misfit(call, hook.desc);
        if (misfit != null)
        {
            throw new IllegalArgumentException(misfit);
        }
        replace(owner, call, hookOwner, hook, gate);
    }

    private static void replace(MethodNode owner, MethodInsnNode call, String targetName, MethodNode hook, String gate)
    {
        InsnList redirected = new InsnList();
        redirected.add(new MethodInsnNode(Opcodes.INVOKESTATIC, targetName, hook.name, hook.desc, false));
        Type returned = Type.getReturnType(call.desc);
        if (reference(returned) && !returned.equals(Type.getReturnType(hook.desc)))
        {
            redirected.add(new TypeInsnNode(Opcodes.CHECKCAST, returned.getInternalName()));
        }
        if (gate.isEmpty())
        {
            owner.instructions.insertBefore(call, redirected);
            owner.instructions.remove(call);
            return;
        }

        LabelNode original = new LabelNode();
        LabelNode end = new LabelNode();
        InsnList before = new InsnList();
        before.add(Gates.read(gate));
        before.add(new JumpInsnNode(Opcodes.IFEQ, original));
        before.add(redirected);
        before.add(new JumpInsnNode(Opcodes.GOTO, end));
        before.add(original);
        owner.instructions.insertBefore(call, before);
        owner.instructions.insert(call, end);
    }
}
