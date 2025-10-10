package com.tonic.classloader;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.*;

import static com.tonic.classloader.ProxyClassProvider.PROXY_CLASS_PACKAGE_SLASHED;
import static org.objectweb.asm.Opcodes.*;

public class ProxyClassBuilder
{
    public static ProxyClassBuilder builder()
    {
        return new ProxyClassBuilder();
    }
    private final Set<String> constructorDescriptors = new HashSet<>();
    private String className;
    private String superClassName;
    private final Set<String> interfaces = new HashSet<>();

    public ProxyClassBuilder withName(String className)
    {
        if (className.contains("."))
        {
            String[] parts = className.split("\\.");
            className = parts[parts.length - 1];
        }
        this.className = className;
        return this;
    }

    public ProxyClassBuilder withSuper(String superClassName)
    {
        this.superClassName = superClassName;
        return this;
    }

    public ProxyClassBuilder withInterface(String interfaceName)
    {
        this.interfaces.add(interfaceName);
        return this;
    }

    public ProxyClassBuilder withInterfaces(String... interfaceNames)
    {
        this.interfaces.addAll(Arrays.asList(interfaceNames));
        return this;
    }

    public ProxyClassBuilder withConstructor(String descriptor)
    {
        this.constructorDescriptors.add(descriptor);
        return this;
    }

    public ProxyClassBuilder withConstructors(String... descriptors)
    {
        this.constructorDescriptors.addAll(Arrays.asList(descriptors));
        return this;
    }

    public byte[] build()
    {
        if (className == null || className.isEmpty())
        {
            throw new IllegalStateException("Class name must be set");
        }
        if (superClassName == null || superClassName.isEmpty())
        {
            superClassName = "java/lang/Object";
        }

        className = className.replace(".", "/");
        superClassName = superClassName.replace(".", "/");

        String[] ifaces = null;
        if (!interfaces.isEmpty())
        {
            ifaces = interfaces.stream()
                    .map(iface -> iface.replace('.', '/'))
                    .toArray(String[]::new);
        }

        className = className.replace('.', '/');
        superClassName = superClassName.replace('.', '/');
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(
                V1_8,
                ACC_PUBLIC | ACC_SUPER,
                PROXY_CLASS_PACKAGE_SLASHED + className,
                null,
                superClassName,
                ifaces
        );

        for(String constructorDescriptor : constructorDescriptors)
        {
            MethodVisitor mv = cw.visitMethod(
                    ACC_PUBLIC,
                    "<init>",
                    constructorDescriptor,
                    null,
                    null
            );

            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);

            Type[] argTypes = Type.getArgumentTypes(constructorDescriptor);

            int varIndex = 1;
            for (Type argType : argTypes) {
                int opcode = argType.getOpcode(ILOAD);
                int size = argType.getSize();
                mv.visitVarInsn(opcode, varIndex);
                varIndex += size;
            }

            mv.visitMethodInsn(
                    INVOKESPECIAL,
                    superClassName,
                    "<init>",
                    constructorDescriptor,
                    false
            );

            mv.visitInsn(RETURN);

            int maxStack = 1;
            for (Type argType : argTypes) {
                maxStack += argType.getSize();
            }

            mv.visitMaxs(maxStack, varIndex);
            mv.visitEnd();
        }

        cw.visitEnd();

        return cw.toByteArray();
    }
}
