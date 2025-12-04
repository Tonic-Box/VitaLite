package com.tonic.util.asm;

import com.tonic.util.asm.BytecodeBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

public class InsnUtil
{
    public static void insertStringCasts(InsnList insnList) {

        for (AbstractInsnNode node : insnList) {
            if (node instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) node;
                if (!(ldc.cst instanceof String)) {
                    MethodInsnNode toStringCall = new MethodInsnNode(
                            INVOKESTATIC,
                            "java/lang/String",
                            "valueOf",
                            "(Ljava/lang/Object;)Ljava/lang/String;",
                            false
                    );
                    insnList.insert(ldc, toStringCall);
                }
            } else if (node instanceof IntInsnNode || node instanceof InsnNode) {
                int opcode = node.getOpcode();
                if ((opcode >= ICONST_M1 && opcode <= ICONST_5) ||
                        opcode == BIPUSH || opcode == SIPUSH ||
                        opcode == LDC) {
                    MethodInsnNode toStringCall = new MethodInsnNode(
                            INVOKESTATIC,
                            "java/lang/Integer",
                            "toString",
                            "(I)Ljava/lang/String;",
                            false
                    );
                    insnList.insert(node, toStringCall);
                }
            } else if (node instanceof VarInsnNode) {
                VarInsnNode varInsn = (VarInsnNode) node;
                if (varInsn.getOpcode() == ILOAD ||
                        varInsn.getOpcode() == LLOAD ||
                        varInsn.getOpcode() == FLOAD ||
                        varInsn.getOpcode() == DLOAD ||
                        varInsn.getOpcode() == ALOAD) {
                    MethodInsnNode toStringCall = generateToStringCast(varInsn);

                    insnList.insert(varInsn, toStringCall);
                }
            }
        }
    }

    private static MethodInsnNode generateToStringCast(VarInsnNode varInsn) {
        String owner, method, descriptor;
        switch (varInsn.getOpcode()) {
            case ILOAD:
                owner = "java/lang/Integer";
                method = "toString";
                descriptor = "(I)Ljava/lang/String;";
                break;
            case LLOAD:
                owner = "java/lang/Long";
                method = "toString";
                descriptor = "(J)Ljava/lang/String;";
                break;
            case FLOAD:
                owner = "java/lang/Float";
                method = "toString";
                descriptor = "(F)Ljava/lang/String;";
                break;
            case DLOAD:
                owner = "java/lang/Double";
                method = "toString";
                descriptor = "(D)Ljava/lang/String;";
                break;
            case ALOAD:
            default:
                owner = "java/lang/String";
                method = "valueOf";
                descriptor = "(Ljava/lang/Object;)Ljava/lang/String;";
                break;
        }

        return new MethodInsnNode(
                INVOKESTATIC,
                owner,
                method,
                descriptor,
                false
        );
    }

    public static boolean isReturn(InsnNode node)
    {
        return node.getOpcode() >= IRETURN && node.getOpcode() <= RETURN;
    }

    public static InsnList generateDefaultReturn(MethodNode method) {
        Type returnType = Type.getReturnType(method.desc);
        BytecodeBuilder builder = BytecodeBuilder.create();

        switch (returnType.getSort()) {
            case Type.VOID:
                builder.returnVoid();
                break;

            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                builder.pushInt(0).returnValue(IRETURN);
                break;

            case Type.LONG:
                builder.pushLong(0L).returnValue(LRETURN);
                break;

            case Type.FLOAT:
                builder.pushFloat(0.0f).returnValue(FRETURN);
                break;

            case Type.DOUBLE:
                builder.pushDouble(0.0).returnValue(DRETURN);
                break;

            case Type.ARRAY:
            case Type.OBJECT:
                builder.pushNull().returnValue(ARETURN);
                break;

            default:
                throw new IllegalArgumentException("Unexpected return type: " + returnType);
        }

        return builder.build();
    }
}