package com.tonic.rlmixins;

import com.tonic.injector.annotations.ClassMod;
import com.tonic.injector.annotations.Mixin;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Sizes the GPU plugin's zone buffers by the faces its scene uploader writes, leaving out the hidden tiles and faces it skips, so no buffer keeps an unused tail on the card and in the driver's copy.
 */
@Mixin("net/runelite/client/plugins/gpu/SceneUploader")
public class SceneUploaderMixin
{
    /**
     * Counts each tile's paint and model, and each scenery model, through ZoneFaces.
     *
     * @param uploader the scene uploader class
     */
    @ClassMod
    public static void exactZoneSizes(ClassNode uploader)
    {
        String zone = "net/runelite/client/plugins/gpu/Zone";
        String faces = "com/tonic/services/gpu/ZoneFaces";
        for (MethodNode method : uploader.methods)
        {
            if (method.name.equals("zoneSize") && method.desc.equals("(L" + zone + ";Lnet/runelite/api/Tile;)V"))
            {
                int paint = -1;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext())
                {
                    if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).name.equals("getSceneTilePaint") && insn.getNext().getOpcode() == Opcodes.ASTORE)
                    {
                        paint = ((VarInsnNode) insn.getNext()).var;
                    }
                    else if (paint >= 0 && insn.getOpcode() == Opcodes.ICONST_2 && insn.getPrevious() instanceof FieldInsnNode && ((FieldInsnNode) insn.getPrevious()).name.equals("sizeO") && insn.getNext().getOpcode() == Opcodes.IADD)
                    {
                        InsnList count = new InsnList();
                        count.add(new VarInsnNode(Opcodes.ALOAD, paint));
                        count.add(new MethodInsnNode(Opcodes.INVOKESTATIC, faces, "paint", "(Lnet/runelite/api/SceneTilePaint;)I", false));
                        AbstractInsnNode next = count.getLast();
                        method.instructions.insertBefore(insn, count);
                        method.instructions.remove(insn);
                        insn = next;
                        paint = -1;
                    }
                    else if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).name.equals("getFaceX") && insn.getNext().getOpcode() == Opcodes.ARRAYLENGTH)
                    {
                        AbstractInsnNode length = insn.getNext();
                        MethodInsnNode count = new MethodInsnNode(Opcodes.INVOKESTATIC, faces, "tileModel", "(Lnet/runelite/api/SceneTileModel;)I", false);
                        method.instructions.set(insn, count);
                        method.instructions.remove(length);
                        insn = count;
                    }
                }
            }
            else if (method.name.equals("zoneRenderableSize") && method.desc.equals("(L" + zone + ";Lnet/runelite/api/Renderable;)V"))
            {
                InsnList body = new InsnList();
                body.add(new VarInsnNode(Opcodes.ALOAD, 2));
                body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, faces, "renderable", "(Lnet/runelite/api/Renderable;)J", false));
                body.add(new VarInsnNode(Opcodes.LSTORE, 3));
                body.add(new VarInsnNode(Opcodes.ALOAD, 1));
                body.add(new InsnNode(Opcodes.DUP));
                body.add(new FieldInsnNode(Opcodes.GETFIELD, zone, "sizeO", "I"));
                body.add(new VarInsnNode(Opcodes.LLOAD, 3));
                body.add(new InsnNode(Opcodes.L2I));
                body.add(new InsnNode(Opcodes.IADD));
                body.add(new FieldInsnNode(Opcodes.PUTFIELD, zone, "sizeO", "I"));
                body.add(new VarInsnNode(Opcodes.ALOAD, 1));
                body.add(new InsnNode(Opcodes.DUP));
                body.add(new FieldInsnNode(Opcodes.GETFIELD, zone, "sizeA", "I"));
                body.add(new VarInsnNode(Opcodes.LLOAD, 3));
                body.add(new IntInsnNode(Opcodes.BIPUSH, 32));
                body.add(new InsnNode(Opcodes.LUSHR));
                body.add(new InsnNode(Opcodes.L2I));
                body.add(new InsnNode(Opcodes.IADD));
                body.add(new FieldInsnNode(Opcodes.PUTFIELD, zone, "sizeA", "I"));
                body.add(new InsnNode(Opcodes.RETURN));
                method.instructions = body;
                method.tryCatchBlocks.clear();
                if (method.localVariables != null)
                {
                    method.localVariables.clear();
                }
                method.maxLocals = Math.max(method.maxLocals, 5);
                method.maxStack = Math.max(method.maxStack, 5);
            }
        }
    }
}
