package com.tonic.injector.pipeline;

import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JField;
import com.tonic.util.dto.JMethod;
import com.tonic.util.MappingProvider;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.injector.util.AnnotationUtil;
import com.tonic.injector.util.TransformerUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * Transforms shadow annotations to create proxy methods and fields.
 */
public class ShadowTransformer
{
    /**
     * Creates shadow method proxy for accessing target method.
     * @param mixin class node containing shadow method
     * @param method shadow method to patch
     */
    public static void patch(ClassNode mixin, MethodNode method) {
        String gamepackName = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
        String name = AnnotationUtil.getAnnotation(method, Shadow.class, "value");
        Boolean isRuneliteExport = AnnotationUtil.getAnnotation(method, Shadow.class, "isRuneLites");

        ClassNode injectionSite = TransformerUtil.getBaseClass(mixin);

        if(isRuneliteExport != null && isRuneliteExport)
        {
            createGenericBridgeMethod(injectionSite, method, name);
            return;
        }

        ClassNode gamepack = TransformerUtil.getMethodClass(mixin, name);

        MethodNode toShadow = TransformerUtil.getTargetMethod(mixin, name);

        Number multiplier = null;
        if(!gamepackName.contains("/"))
        {
            JClass jClass = MappingProvider.getClass(gamepackName);
            JMethod jMethod = MappingProvider.getMethod(jClass, name);
            multiplier = jMethod.getGarbageValue();
        }

        InsnList instructions = new InsnList();

        Type shadowReturnType = Type.getReturnType(method.desc);
        Type[] shadowParams = Type.getArgumentTypes(method.desc);
        Type[] toShadowParams = Type.getArgumentTypes(toShadow.desc);

        boolean isTargetStatic = (toShadow.access & Opcodes.ACC_STATIC) != 0;
        boolean isShadowStatic = (method.access & Opcodes.ACC_STATIC) != 0;

        int localVarIndex = isShadowStatic ? 0 : 1;
        if (!isTargetStatic) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }

        for (int i = 0; i < shadowParams.length; i++) {
            Type paramType = shadowParams[i];
            instructions.add(new VarInsnNode(paramType.getOpcode(Opcodes.ILOAD), localVarIndex));

            if (i < toShadowParams.length) {
                Type expectedType = toShadowParams[i];
                if (!paramType.equals(expectedType)) {
                    if (expectedType.getSort() == Type.OBJECT || expectedType.getSort() == Type.ARRAY) {
                        instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, expectedType.getInternalName()));
                    }
                }
            }

            localVarIndex += paramType.getSize();
        }

        if (multiplier != null && toShadowParams.length > shadowParams.length) {
            Type garbageType = toShadowParams[toShadowParams.length - 1];

            if (multiplier instanceof Integer) {
                instructions.add(new LdcInsnNode(multiplier.intValue()));
                if (garbageType.getSort() == Type.BYTE) {
                    instructions.add(new InsnNode(Opcodes.I2B));
                } else if (garbageType.getSort() == Type.SHORT) {
                    instructions.add(new InsnNode(Opcodes.I2S));
                } else if (garbageType.getSort() == Type.CHAR) {
                    instructions.add(new InsnNode(Opcodes.I2C));
                }
            } else if (multiplier instanceof Long) {
                instructions.add(new LdcInsnNode(multiplier.longValue()));
            } else if (multiplier instanceof Float) {
                instructions.add(new LdcInsnNode(multiplier.floatValue()));
            } else if (multiplier instanceof Double) {
                instructions.add(new LdcInsnNode(multiplier.doubleValue()));
            } else {
                instructions.add(new LdcInsnNode(multiplier.intValue()));
            }
        }

        int invokeOpcode = isTargetStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
        instructions.add(new MethodInsnNode(
                invokeOpcode,
                gamepack.name,
                toShadow.name,
                toShadow.desc,
                false
        ));

        Type toShadowReturnType = Type.getReturnType(toShadow.desc);
        if (shadowReturnType.getSort() == Type.VOID) {
            instructions.add(new InsnNode(Opcodes.RETURN));
        } else {
            if (!toShadowReturnType.equals(shadowReturnType)) {
                if (shadowReturnType.getSort() == Type.OBJECT || shadowReturnType.getSort() == Type.ARRAY) {
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, shadowReturnType.getInternalName()));
                }
            }
            instructions.add(new InsnNode(shadowReturnType.getOpcode(Opcodes.IRETURN)));
        }

        method.access &= ~Opcodes.ACC_ABSTRACT;
        method.instructions = instructions;

        MethodNode injectedMethod = new MethodNode(
                method.access,
                method.name,
                method.desc,
                method.signature,
                method.exceptions != null ? method.exceptions.toArray(new String[0]) : null
        );
        injectedMethod.instructions = instructions;

        injectionSite.methods.add(injectedMethod);
    }

    /**
     * Creates shadow field proxy for accessing target field.
     * @param mixin mixin class containing shadow field
     * @param field shadow field to transform
     */
    public static void patch(ClassNode mixin, FieldNode field) {
        try
        {
            String gamepackName = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
            String name = AnnotationUtil.getAnnotation(field, Shadow.class, "value");
            JClass jClass = MappingProvider.getClass(gamepackName);
            JField jField = MappingProvider.getField(jClass, name);

            String oldOwner = mixin.name;
            String oldName = field.name;
            String newOwner = jField.getOwnerObfuscatedName();
            String newName = jField.getObfuscatedName();

            Type oldType = Type.getType(field.desc);
            Type newType = Type.getType(jField.getDescriptor());

            for(MethodNode mn : mixin.methods)
            {
                transformProxyField(jField, mn, oldType, newType, oldName, oldOwner, newName, newOwner);
            }
        }
        catch (Exception e)
        {
            System.out.println("Error transforming shadow field: " + mixin.name + "." + field.name);
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Transforms field references in method instructions from old to new field.
     * @param field field metadata
     * @param mn method to scan
     * @param oldType mixin field type
     * @param newType target field type
     * @param oldName old field name
     * @param oldOwner old field owner
     * @param newName new field name
     * @param newOwner new field owner
     */
    private static void transformProxyField(JField field, MethodNode mn, Type oldType, Type newType, String oldName, String oldOwner, String newName, String newOwner)
    {
        InsnList instructions = mn.instructions;
        if (instructions == null) return;

        for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof FieldInsnNode)) continue;

            FieldInsnNode fin = (FieldInsnNode) insn;
            if (!fin.owner.equals(oldOwner) || !fin.name.equals(oldName)) continue;

            fin.owner = newOwner;
            fin.name  = newName;
            fin.desc = newType.getDescriptor();

            int op = fin.getOpcode();
            if ((op == Opcodes.GETFIELD || op == Opcodes.GETSTATIC))
            {
                if((oldType.getSort() == Type.OBJECT || oldType.getSort() == Type.ARRAY))
                {
                    TypeInsnNode checkcast = new TypeInsnNode(
                            Opcodes.CHECKCAST,
                            oldType.getInternalName()
                    );
                    instructions.insert(fin, checkcast);
                    continue;
                }
                if(field.getGetter() == null)
                    continue;

                Number multiplier = field.getGetter();

                if(field.getDescriptor().equals("I"))
                {
                    instructions.insert(fin, new LdcInsnNode(multiplier.intValue()));
                    instructions.insert(fin.getNext(), new InsnNode(Opcodes.IMUL));
                }
                else if(field.getDescriptor().equals("J"))
                {
                    instructions.insert(fin, new LdcInsnNode(multiplier.longValue()));
                    instructions.insert(fin.getNext(), new InsnNode(Opcodes.LMUL));
                }
            }
            else if (op == Opcodes.PUTFIELD || op == Opcodes.PUTSTATIC) {
                if((oldType.getSort() == Type.OBJECT || oldType.getSort() == Type.ARRAY))
                {
                    TypeInsnNode checkcast = new TypeInsnNode(
                            Opcodes.CHECKCAST,
                            newType.getInternalName()
                    );
                    instructions.insertBefore(fin, checkcast);
                    continue;
                }
                if(field.getSetter() == null)
                    continue;

                Number multiplier = field.getSetter();

                if(field.getDescriptor().equals("I"))
                {
                    instructions.insertBefore(fin, new LdcInsnNode(multiplier.intValue()));
                    instructions.insertBefore(fin, new InsnNode(Opcodes.IMUL));
                }
                else if(field.getDescriptor().equals("J"))
                {
                    instructions.insertBefore(fin, new LdcInsnNode(multiplier.longValue()));
                    instructions.insertBefore(fin, new InsnNode(Opcodes.LMUL));
                }
            }
        }
    }

    public static void createGenericBridgeMethod(ClassNode classNode,
                                                 MethodNode interfaceMethod,
                                                 String targetMethodName) {
        // Create new method with interface signature
        MethodNode bridgeMethod = new MethodNode(
                Opcodes.ACC_PUBLIC,
                interfaceMethod.name,
                interfaceMethod.desc,
                interfaceMethod.signature,  // Preserve generic signature
                interfaceMethod.exceptions != null ?
                        interfaceMethod.exceptions.toArray(new String[0]) : null
        );

        // Find the target method to get its return type
        MethodNode targetMethod = null;
        String targetDesc = null;
        for (MethodNode mn : classNode.methods) {
            if (mn.name.equals(targetMethodName)) {
                // Check if parameters match (ignoring return type)
                Type[] interfaceParams = Type.getArgumentTypes(interfaceMethod.desc);
                Type[] targetParams = Type.getArgumentTypes(mn.desc);
                if (java.util.Arrays.equals(interfaceParams, targetParams)) {
                    targetMethod = mn;
                    targetDesc = mn.desc;
                    break;
                }
            }
        }

        if (targetMethod == null) {
            throw new IllegalArgumentException("Target method " + targetMethodName + " not found");
        }

        InsnList instructions = bridgeMethod.instructions;

        // Load 'this'
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));

        // Load all parameters
        Type[] paramTypes = Type.getArgumentTypes(interfaceMethod.desc);
        int localVarIndex = 1; // Start at 1 (0 is 'this')

        for (Type paramType : paramTypes) {
            int opcode;
            switch (paramType.getSort()) {
                case Type.BOOLEAN:
                case Type.BYTE:
                case Type.CHAR:
                case Type.SHORT:
                case Type.INT:
                    opcode = Opcodes.ILOAD;
                    break;
                case Type.LONG:
                    opcode = Opcodes.LLOAD;
                    break;
                case Type.FLOAT:
                    opcode = Opcodes.FLOAD;
                    break;
                case Type.DOUBLE:
                    opcode = Opcodes.DLOAD;
                    break;
                default:
                    opcode = Opcodes.ALOAD;
                    break;
            }
            instructions.add(new VarInsnNode(opcode, localVarIndex));
            localVarIndex += paramType.getSize(); // Long/Double take 2 slots
        }

        // Call the target method
        boolean isStatic = (targetMethod.access & Opcodes.ACC_STATIC) != 0;
        boolean isInterface = (classNode.access & Opcodes.ACC_INTERFACE) != 0;

        instructions.add(new MethodInsnNode(
                isStatic ? Opcodes.INVOKESTATIC :
                        (isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL),
                classNode.name,
                targetMethodName,
                targetDesc,
                isInterface
        ));

        // Add checkcast if return types differ
        Type interfaceReturn = Type.getReturnType(interfaceMethod.desc);
        Type targetReturn = Type.getReturnType(targetDesc);

        if (!targetReturn.equals(interfaceReturn) &&
                interfaceReturn.getSort() == Type.OBJECT) {
            // Cast to the interface return type
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST,
                    interfaceReturn.getInternalName()));
        }

        // Return
        int returnOpcode;
        switch (interfaceReturn.getSort()) {
            case Type.VOID:
                returnOpcode = Opcodes.RETURN;
                break;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                returnOpcode = Opcodes.IRETURN;
                break;
            case Type.LONG:
                returnOpcode = Opcodes.LRETURN;
                break;
            case Type.FLOAT:
                returnOpcode = Opcodes.FRETURN;
                break;
            case Type.DOUBLE:
                returnOpcode = Opcodes.DRETURN;
                break;
            default:
                returnOpcode = Opcodes.ARETURN;
                break;
        }
        instructions.add(new InsnNode(returnOpcode));

        // Set max stack and locals
        bridgeMethod.maxStack = Math.max(localVarIndex, paramTypes.length + 1);
        bridgeMethod.maxLocals = localVarIndex;

        // Add the method to the class
        classNode.methods.add(bridgeMethod);
    }
}
