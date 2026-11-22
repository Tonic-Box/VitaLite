package com.tonic.injector.pipeline;

import com.tonic.injector.annotations.Replace;
import com.tonic.injector.util.AnnotationUtil;
import com.tonic.injector.util.TransformerUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * Replaces methods by injecting static replacement and redirecting calls.
 */
public class ReplaceTransformer
{
    /**
     * Replaces target method with call to injected static replacement method.
     * @param mixin mixin class containing replacement method
     * @param method method annotated with Replace
     */
    public static void patch(ClassNode mixin, MethodNode method) {
        String name = AnnotationUtil.getAnnotation(method, Replace.class, "value");

        ClassNode gamepack = TransformerUtil.getBaseClass(mixin);
        InjectTransformer.patch(gamepack, mixin, method);

        MethodNode toReplace = TransformerUtil.requireTargetMethod("@Replace " + mixin.name + "." + method.name, mixin, name, null);
        if (toReplace == null) {
            return;
        }

        Type hookType   = Type.getMethodType(method.desc);
        Type targetType = Type.getMethodType(toReplace.desc);
        Type[] hookParams   = hookType.getArgumentTypes();
        Type[] targetParams = targetType.getArgumentTypes();
        Type returnType   = hookType.getReturnType();

        InsnList override = new InsnList();
        boolean isStatic = (toReplace.access & Opcodes.ACC_STATIC) != 0;
        int localIdx = isStatic ? 0 : 1;

        for (int i = 0; i < hookParams.length; i++) {
            Type t = targetParams[i];
            switch (t.getSort()) {
                case Type.BOOLEAN: case Type.CHAR: case Type.BYTE:
                case Type.SHORT:   case Type.INT:
                    override.add(new VarInsnNode(Opcodes.ILOAD,  localIdx));
                    break;
                case Type.FLOAT:
                    override.add(new VarInsnNode(Opcodes.FLOAD,  localIdx));
                    break;
                case Type.LONG:
                    override.add(new VarInsnNode(Opcodes.LLOAD,  localIdx));
                    break;
                case Type.DOUBLE:
                    override.add(new VarInsnNode(Opcodes.DLOAD,  localIdx));
                    break;
                default:
                    override.add(new VarInsnNode(Opcodes.ALOAD,  localIdx));
            }
            localIdx += t.getSize();
        }

        override.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                gamepack.name,
                method.name,
                method.desc,
                false
        ));

        int retOp;
        switch (returnType.getSort()) {
            case Type.VOID:
                retOp = Opcodes.RETURN;
                break;
            case Type.BOOLEAN: case Type.CHAR: case Type.BYTE:
            case Type.SHORT:   case Type.INT:
                retOp = Opcodes.IRETURN;
                break;
            case Type.FLOAT:
                retOp = Opcodes.FRETURN;
                break;
            case Type.LONG:
                retOp = Opcodes.LRETURN;
                break;
            case Type.DOUBLE:
                retOp = Opcodes.DRETURN;
                break;
                default:
                retOp = Opcodes.ARETURN;
        }
        override.add(new InsnNode(retOp));

        toReplace.instructions.insert(override);
    }
}
