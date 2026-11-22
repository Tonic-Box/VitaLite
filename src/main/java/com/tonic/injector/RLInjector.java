package com.tonic.injector;

import com.tonic.injector.annotations.*;
import com.tonic.injector.pipeline.*;
import com.tonic.injector.util.*;
import com.tonic.patch.PatchGenerator;
import com.tonic.util.PackageUtil;
import com.tonic.util.asm.ClassNodeUtil;
import com.tonic.util.asm.SignerMapper;
import com.tonic.vitalite.Main;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RLInjector
{
    private static final String MIXINS = "com.tonic.rlmixins";
    public static final Map<String,ClassNode> runelite = new HashMap<>();

    public static void patch() throws Exception
    {
        for (var entry : Main.LIBS.getRunelite().classes.entrySet()) {
            String name = entry.getKey();
            if(SignerMapper.shouldIgnore(name))
            {
                System.out.println("Ignoring class: " + name);
                continue;
            }
            byte[] bytes = entry.getValue();

            // Store original bytecode for patch generation
            PatchGenerator.storeOriginalRunelite(name, bytes);

            runelite.put(name.replace(".", "/"), ClassNodeUtil.toNode(bytes));
        }

        List<ClassNode> mixins = PackageUtil.getClasses(MIXINS, null);
        Set<String> targets = new HashSet<>();

        for(ClassNode mixin : mixins)
        {
            String target = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
            if (target == null)
            {
                continue;
            }
            targets.add(target);
            shadowFields(mixin, runelite.get(target));
            for(MethodNode method : mixin.methods)
            {
                if(AnnotationUtil.hasAnnotation(method, Inject.class) || !AnnotationUtil.hasAnyAnnotation(method))
                {
                    InjectTransformer.patch(runelite.get(target), mixin, method);
                }
                if(AnnotationUtil.hasAnnotation(method, MethodHook.class))
                {
                    MethodHookTransformer.patch(mixin, method);
                }
                if(AnnotationUtil.hasAnnotation(method, Replace.class))
                {
                    ReplaceTransformer.patch(mixin, method);
                }
                if(AnnotationUtil.hasAnnotation(method, MethodOverride.class))
                {
                    MethodOverrideTransformer.patch(mixin, method);
                }
                if(AnnotationUtil.hasAnnotation(method, Shadow.class))
                {
                    ShadowTransformer.patch(mixin, method);
                }
                if(AnnotationUtil.hasAnnotation(method, Construct.class))
                {
                    ConstructTransformer.patch(mixin, method);
                }
                if(AnnotationUtil.hasAnnotation(method, Disable.class))
                {
                    DisableTransformer.patch(mixin, method);
                }
                if(AnnotationUtil.hasAnnotation(method, Insert.class))
                {
                    InsertTransformer.patch(mixin, method);
                }
                if(AnnotationUtil.hasAnnotation(method, Redirect.class))
                {
                    RedirectTransformer.patch(mixin, method);
                }
                if(AnnotationUtil.hasAnnotation(method, ClassMod.class))
                {
                    ClassModTransformer.patch(mixin, method);
                }
            }
        }

        for (String target : targets)
        {
            ClassNode patched = runelite.get(target);
            if (InjectionFailures.require(patched != null, "RuneLite mixin target " + target + " not found"))
            {
                MemberCheck.verify(patched, runelite);
            }
        }

        for (var entry : runelite.entrySet()) {
            String name = entry.getKey().replace("/", ".");
            if(SignerMapper.shouldIgnore(name))
            {
                continue;
            }
            RLGlobalMixin.patch(entry.getValue());
            MemberCheck.verifyVitaLite(entry.getValue());
            byte[] bytes = ClassNodeUtil.toBytes(entry.getValue());
            Main.LIBS.getRunelite().classes.put(
                    name,
                    bytes
            );

            // Capture diff if patch generation is enabled
            PatchGenerator.captureRuneliteDiff(name, bytes);

//            List<String> toDump = List.of(
//                    "net.runelite.client.RuneLite",
//                    "net.runelite.client.RuneLiteModule",
//                    "net.runelite.client.plugins.PluginManager",
//                    "net.runelite.client.ui.ClientUI",
//                    "net.runelite.client.ui.SplashScreen",
//                    "net.runelite.client.plugins.config.TopLevelConfigPanel"
//            );
//            if(toDump.contains(name))
//            {
//                ClassFileUtil.writeClass(
//                        name,
//                        bytes,
//                        Path.of("C:/test/dumper/")
//                );
//            }
        }
        runelite.clear();
    }

    private static void shadowFields(ClassNode mixin, ClassNode target)
    {
        for (FieldNode field : mixin.fields)
        {
            String name = AnnotationUtil.getAnnotation(field, Shadow.class, "value");
            if (name == null)
            {
                continue;
            }
            boolean present = target != null && target.fields.stream().anyMatch(candidate -> candidate.name.equals(name) && candidate.desc.equals(field.desc));
            if (!InjectionFailures.require(present, "@Shadow " + mixin.name + "." + field.name + ": no field " + name + " " + field.desc + " in " + (target == null ? "a missing class" : target.name)))
            {
                continue;
            }
            for (MethodNode method : mixin.methods)
            {
                for (AbstractInsnNode insn : method.instructions)
                {
                    if (insn instanceof FieldInsnNode && ((FieldInsnNode) insn).owner.equals(mixin.name) && ((FieldInsnNode) insn).name.equals(field.name))
                    {
                        ((FieldInsnNode) insn).name = name;
                    }
                }
            }
        }
    }
}
