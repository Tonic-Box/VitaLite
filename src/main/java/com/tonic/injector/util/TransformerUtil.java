package com.tonic.injector.util;

import com.tonic.util.MappingProvider;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JMethod;
import com.tonic.injector.Injector;
import com.tonic.injector.RLInjector;
import com.tonic.injector.annotations.Mixin;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class TransformerUtil
{
    public static ClassNode getBaseClass(ClassNode mixin)
    {
        String className = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
        if(className == null)
        {
            throw new RuntimeException("Could not find Mixin annotation on class: " + mixin.name);
        }
        return getBaseClass(className);
    }
    public static ClassNode getBaseClass(String className)
    {
        if(className.contains("/"))
        {
            return RLInjector.runelite.get(className);
        }

        JClass jClass = MappingProvider.getClass(className);
        if(jClass != null)
        {
            return Injector.gamepack.get(jClass.getObfuscatedName());
        }

        return null;
    }

    public static ClassNode getMethodClass(ClassNode mixin, String targetMethodName)
    {
        String className = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
        if(className.contains("/"))
        {
            return getBaseClass(className);
        }
        JClass jClass = MappingProvider.getClass(className);
        if (jClass == null) {
            throw new RuntimeException("Could not find class mapping for: " + className);
        }

        JMethod jMethod = MappingProvider.getMethod(jClass, targetMethodName);
        if (jMethod == null) {
            throw new RuntimeException("Could not find method mapping for: " + targetMethodName + " in class: " + className);
        }

        ClassNode gamepackClass = Injector.gamepack.get(jMethod.getOwnerObfuscatedName());
        if (gamepackClass == null) {
            throw new RuntimeException("Could not find gamepack class for: " + jMethod.getOwnerObfuscatedName());
        }

        return gamepackClass;
    }

    public static MethodNode getTargetMethod(ClassNode mixin, String targetMethodName)
    {
        String className = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
        if(className != null && className.contains("/"))
        {
            ClassNode rlClass = RLInjector.runelite.get(className);
            if(rlClass == null)
            {
                throw new RuntimeException("Could not find runelite class for: " + className);
            }
            return rlClass.methods.stream()
                    .filter(m -> m.name.equals(targetMethodName))
                    .findFirst()
                    .orElse(null);
        }
        JClass jClass = MappingProvider.getClass(className);
        if (jClass == null) {
            throw new RuntimeException("Could not find class mapping for: " + className);
        }

        JMethod jMethod = MappingProvider.getMethod(jClass, targetMethodName);
        if (jMethod == null) {
            throw new RuntimeException("Could not find method mapping for: " + targetMethodName + " in class: " + className);
        }

        ClassNode gamepackClass = Injector.gamepack.get(jMethod.getOwnerObfuscatedName());
        if (gamepackClass == null) {
            throw new RuntimeException("Could not find gamepack class for: " + jMethod.getOwnerObfuscatedName());
        }

        return gamepackClass.methods.stream()
                .filter(m -> m.name.equals(jMethod.getObfuscatedName()) && m.desc.equals(jMethod.getDescriptor()))
                .findFirst()
                .orElse(null);
    }

    public static MethodNode getTargetMethod(ClassNode mixin, String targetMethodName, String descriptor)
    {
        if(descriptor == null || descriptor.isEmpty())
        {
            return getTargetMethod(mixin, targetMethodName);
        }
        String className = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
        if(className != null && className.contains("/"))
        {
            ClassNode rlClass = RLInjector.runelite.get(className);
            if(rlClass == null)
            {
                throw new RuntimeException("Could not find runelite class for: " + className);
            }
            return rlClass.methods.stream()
                    .filter(m -> m.name.equals(targetMethodName) && m.desc.equals(descriptor))
                    .findFirst()
                    .orElse(null);
        }
        throw new IllegalArgumentException("desc " + descriptor + " given for gamepack method " + targetMethodName + ": gamepack methods are found by their mapping, desc is for RuneLite targets only");
    }

    public static MethodNode requireTargetMethod(String hook, ClassNode mixin, String targetMethodName, String descriptor)
    {
        MethodNode target = getTargetMethod(mixin, targetMethodName, descriptor);
        InjectionFailures.require(target != null, hook + ": target method " + targetMethodName + (descriptor == null ? "" : descriptor) + " not found");
        return target;
    }

    public static <T> List<T> select(String hook, List<T> matches, int expected, boolean all, int ordinal)
    {
        if (expected >= 0 && matches.size() != expected)
        {
            InjectionFailures.record(hook + ": " + matches.size() + " matches where " + expected + " were expected");
            return null;
        }
        if (matches.isEmpty())
        {
            InjectionFailures.record(hook + ": nothing matched");
            return null;
        }
        if (all)
        {
            return matches;
        }
        int index = ordinal == -1 ? matches.size() - 1 : ordinal;
        if (index < 0 || index >= matches.size())
        {
            InjectionFailures.record(hook + ": ordinal " + ordinal + " but only " + matches.size() + " matches");
            return null;
        }
        return List.of(matches.get(index));
    }
}
