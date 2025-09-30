package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.util.BytecodeBuilder;
import com.tonic.vitalite.Main;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

@Mixin("net/runelite/client/RuneLite")
public class RuneLiteMixin {
    @Insert(
            method = "main",
            at = @At(
                    value = AtTarget.CHECKCAST,
                    owner = "net/runelite/client/RuneLite"
            ),
            raw = true
    )
    public static void main(ClassNode classNode, MethodNode method, AbstractInsnNode insertionPoint) {
        FieldNode rlInstanceField = new FieldNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "rlInstance",
                "Lnet/runelite/client/RuneLite;",
                null,
                null
        );
        classNode.fields.add(rlInstanceField);

        InsnList toInject = BytecodeBuilder.create()
                .dup()
                .putStaticField("net/runelite/client/RuneLite", "rlInstance", "Lnet/runelite/client/RuneLite;")
                .build();

        method.instructions.insert(insertionPoint, toInject);
    }

    @Insert(
            method = "start",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void start(ClassNode classNode, MethodNode method, AbstractInsnNode insertionPoint)
    {
        if(Main.optionsParser.isIncognito())
            return;
        InsnList code = BytecodeBuilder.create()
                .invokeStatic(
                        "com/tonic/services/codeeval/CodeEvalFrame",
                        "install",
                        "()V"
                ).build();

        method.instructions.insertBefore(
                insertionPoint,
                code
        );
    }
}
