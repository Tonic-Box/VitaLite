package com.tonic.injector.util;

import com.tonic.util.MappingProvider;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JMethod;
import com.tonic.injector.Injector;
import com.tonic.injector.RLInjector;
import com.tonic.injector.annotations.Mixin;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

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
        return null;
    }
}
