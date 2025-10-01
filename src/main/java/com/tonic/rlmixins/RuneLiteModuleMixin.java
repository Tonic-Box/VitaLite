package com.tonic.rlmixins;

import com.tonic.injector.annotations.*;
import com.tonic.injector.util.GuiceBindingInjector;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

@Mixin("net/runelite/client/RuneLiteModule")
public class RuneLiteModuleMixin
{
    @ClassMod
    public static void init(ClassNode node)
    {
        GuiceBindingInjector.addCastBinding(
                "com/tonic/api/TClient",
                "provideTClient"
        );

        // Add TPacketWriter binding - cast to TClient then call getPacketWriter()
        GuiceBindingInjector.addGetterBinding(
                "com/tonic/api/TPacketWriter",
                "provideTPacketWriter",
                "com/tonic/api/TClient",
                "getPacketWriter",
                "()Lcom/tonic/api/TPacketWriter;"
        );

        // Apply all the bindings to the class
        GuiceBindingInjector.patch(node);
    }

    @Insert(
            method = "configure",
            at = @At(value = AtTarget.RETURN),
            raw = true
    )
    public static void configure(MethodNode method, AbstractInsnNode insertionPoint)
    {
        AbstractInsnNode target = null;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof Type) {
                    Type type = (Type) ldc.cst;
                    if (type.getInternalName().equals("net/runelite/client/account/SessionManager")) {
                        target = insn.getPrevious();
                        break;
                    }
                }
            }
        }

        if(target == null)
        {
            return;
        }

        List<AbstractInsnNode> toRemove = new ArrayList<>();
        toRemove.add(target);
        toRemove.add(target.getNext());
        toRemove.add(target.getNext().getNext());
        toRemove.add(target.getNext().getNext().getNext());

        for (AbstractInsnNode insn : toRemove) {
            method.instructions.remove(insn);
        }

        System.out.println("Removed " + toRemove.size() + " instructions for SessionManager binding");
    }

    @MethodOverride("provideTelemetry")
    public Object provideTelemetry() {
        return null;
    }
}
