package com.tonic.injector.pipeline;

import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.util.AnnotationUtil;
import com.tonic.injector.util.ClientClasses;
import com.tonic.injector.util.FieldUtil;
import com.tonic.injector.util.MethodUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Injects methods and fields from mixin classes into gamepack classes.
 */
public class InjectTransformer
{
    private static final String[] INJECTION_TIME_TYPES = {"Lorg/objectweb/asm/", "Lcom/tonic/util/asm/", "Lcom/tonic/injector/"};

    /**
     * @param method a mixin method
     * @return whether it takes or returns an injector type, as the lambdas and helpers of raw hooks do, so it only ever runs while injecting
     */
    private static boolean isInjectionTime(MethodNode method)
    {
        for (String type : INJECTION_TIME_TYPES)
        {
            if (method.desc.contains(type))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Injects a method from mixin to target class if it doesn't exist.
     *
     * @param toClass target class
     * @param fromClass source mixin class
     * @param method method to inject
     */
    public static MethodNode patch(ClassNode toClass, ClassNode fromClass, MethodNode method)
    {
        if(method.name.equals("<init>") && method.desc.equals("()V"))
            return null;

        if(isInjectionTime(method))
            return null;

        MethodNode methodExists = toClass.methods.stream()
                .filter(m -> m.name.equals(method.name) && m.desc.equals(method.desc)).findFirst().orElse(null);

        if(methodExists != null)
            return methodExists;

        MethodNode copyMethod = MethodUtil.copyMethod(method, method.name, fromClass, toClass);
        String delegate = AnnotationUtil.getAnnotation(method, Delegate.class, "value");
        if (delegate != null)
        {
            writeDelegate(copyMethod, toClass, delegate);
        }
        if(copyMethod.visibleAnnotations != null)
            copyMethod.visibleAnnotations.removeIf(a -> a.desc.contains("com/tonic"));
        toClass.methods.add(copyMethod);
        return copyMethod;
    }

    private static void writeDelegate(MethodNode hook, ClassNode owner, String target)
    {
        int dot = target.lastIndexOf('.');
        if (dot <= 0 || dot == target.length() - 1)
        {
            throw new IllegalArgumentException("@Delegate must be \"owner/Class.method\": " + target);
        }
        String calledOwner = target.substring(0, dot);
        ClassNode calledClass = ClientClasses.get(calledOwner);
        MethodNode called = staticMethod(calledClass, calledOwner, target.substring(dot + 1));

        List<Type> passed = new ArrayList<>();
        if ((hook.access & Opcodes.ACC_STATIC) == 0)
        {
            passed.add(Type.getObjectType(owner.name));
        }
        passed.addAll(List.of(Type.getArgumentTypes(hook.desc)));
        Type[] parameters = Type.getArgumentTypes(called.desc);
        if (parameters.length != passed.size())
        {
            throw new IllegalArgumentException("@Delegate " + target + called.desc + " takes " + parameters.length + " arguments but the hook passes " + passed.size());
        }

        InsnList body = new InsnList();
        int local = 0;
        for (int i = 0; i < parameters.length; i++)
        {
            body.add(new VarInsnNode(passed.get(i).getOpcode(Opcodes.ILOAD), local));
            cast(body, passed.get(i), parameters[i], "argument " + i + " of @Delegate " + target);
            local += passed.get(i).getSize();
        }
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, calledOwner, called.name, called.desc, (calledClass.access & Opcodes.ACC_INTERFACE) != 0));
        Type returned = Type.getReturnType(called.desc);
        Type hookReturn = Type.getReturnType(hook.desc);
        if (hookReturn.getSort() == Type.VOID)
        {
            if (returned.getSort() != Type.VOID)
            {
                body.add(new InsnNode(returned.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
            }
        }
        else
        {
            cast(body, returned, hookReturn, "the result of @Delegate " + target);
        }
        body.add(new InsnNode(hookReturn.getOpcode(Opcodes.IRETURN)));

        hook.access &= ~Opcodes.ACC_NATIVE;
        hook.instructions = body;
    }

    private static MethodNode staticMethod(ClassNode type, String owner, String name)
    {
        if (type == null)
        {
            throw new IllegalArgumentException("@Delegate class " + owner + " not found");
        }
        MethodNode found = null;
        for (MethodNode candidate : type.methods)
        {
            if (candidate.name.equals(name) && (candidate.access & Opcodes.ACC_STATIC) != 0)
            {
                if (found != null)
                {
                    throw new IllegalArgumentException("@Delegate " + owner + "." + name + " is overloaded");
                }
                found = candidate;
            }
        }
        if (found == null)
        {
            throw new IllegalArgumentException("@Delegate " + owner + " has no static method " + name);
        }
        if ((found.access & Opcodes.ACC_PUBLIC) == 0 || (type.access & Opcodes.ACC_PUBLIC) == 0)
        {
            throw new IllegalArgumentException("@Delegate " + owner + "." + name + " is not public");
        }
        return found;
    }

    private static void cast(InsnList body, Type from, Type to, String what)
    {
        boolean reference = isReference(to);
        if (reference != isReference(from) || !reference && !from.equals(to))
        {
            throw new IllegalArgumentException(what + " is " + from + " but " + to + " is needed");
        }
        if (reference && !to.equals(from) && !to.getInternalName().equals("java/lang/Object"))
        {
            body.add(new TypeInsnNode(Opcodes.CHECKCAST, to.getInternalName()));
        }
    }

    private static boolean isReference(Type type)
    {
        return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
    }

    /**
     * Injects a field into gamepack class if it doesn't exist.
     *
     * @param gamepack target class
     * @param field field to inject
     */
    public static void patch(ClassNode gamepack, FieldNode field)
    {
        boolean fieldExists = gamepack.fields.stream()
                .anyMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc));
        if(fieldExists)
            return;
        FieldNode copyField = FieldUtil.copyField(field);
        gamepack.fields.add(copyField);
    }
}
