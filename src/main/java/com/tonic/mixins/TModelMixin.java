package com.tonic.mixins;

import com.tonic.api.TModel;
import com.tonic.injector.annotations.ClassMod;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.util.MappingProvider;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

@Mixin("Model")
public abstract class TModelMixin implements TModel
{
    @Shadow("vertexLabels")
    public int[][] vertexLabels;

    @Shadow("faceTransparencies")
    public byte[] faceTransparencies;

    @Inject
    @Override
    public int[][] getVertexGroups()
    {
        return vertexLabels;
    }

    @Inject
    @Override
    public void setFaceTransparencies(byte[] transparencies)
    {
        faceTransparencies = transparencies;
    }

    /**
     * Has a model leave its face drawing, after it was picked, while the SceneCache has nothing for it to fill.
     *
     * @param model the model class
     */
    @ClassMod
    public static void skipCachedFaces(ClassNode model)
    {
        JClass mapped = MappingProvider.getClass("Model");
        for (String name : new String[]{"drawFaces", "drawSortedFaces"})
        {
            JMethod draw = MappingProvider.getMethod(mapped, name);
            for (MethodNode method : model.methods)
            {
                if (!method.name.equals(draw.getObfuscatedName()) || !method.desc.equals(draw.getDescriptor()))
                {
                    continue;
                }

                LabelNode fill = new LabelNode();
                InsnList skip = new InsnList();
                skip.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/tonic/services/SceneCache", "skipsFaces", "()Z", false));
                skip.add(new JumpInsnNode(Opcodes.IFEQ, fill));
                skip.add(new InsnNode(Opcodes.RETURN));
                skip.add(fill);
                method.instructions.insert(skip);
            }
        }
    }
}
