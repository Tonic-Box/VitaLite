package com.tonic.injector.util.expreditor.impls;

import com.tonic.Static;
import com.tonic.util.asm.BytecodeBuilder;
import com.tonic.injector.util.expreditor.ExprEditor;
import com.tonic.injector.util.expreditor.LiteralValue;
import org.objectweb.asm.tree.*;
import java.util.ArrayList;
import java.util.List;

public class ModifyResourceLoading extends ExprEditor
{
    private final List<AbstractInsnNode> toRemove = new ArrayList<>();
    private final ExprEditor subEditor = new ExprEditor() {
        private MethodNode method;

        @Override
        public void edit(LiteralValue literal) {
            if (method == null)
                method = literal.getMethod();

            if (literal.isString()) {
                switch (literal.getStringValue()) {
                    case "Sound FX":
                    case "Music Tracks":
                    case "Music Samples":
                    case "Music Patches":
                        patchSoundLoad(literal);
                        break;
                }
            }
        }

        private void patchSoundLoad(LiteralValue literal)
        {
            InsnList insns = BytecodeBuilder.create()
                    .pop2()
                    .pop()
                    .build();

            AbstractInsnNode target = literal.getLiteralInstruction().getNext();
            AbstractInsnNode methodCall = target.getNext();
            toRemove.add(methodCall);
            method.instructions.insert(target, insns);
        }
    };

    @Override
    public void edit(LiteralValue literal) {
        if (!literal.isString() || !literal.getStringValue().equals("Sound FX"))
            return;

        if(!Static.getCliArgs().isNoMusic())
            return;

        subEditor.instrument(literal.getClassNode(), literal.getMethod());
        for(AbstractInsnNode insn : toRemove)
        {
            literal.getMethod().instructions.remove(insn);
        }
        toRemove.clear();
    }
}
