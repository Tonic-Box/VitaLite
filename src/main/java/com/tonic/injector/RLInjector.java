package com.tonic.injector;

import com.tonic.injector.annotations.*;
import com.tonic.injector.pipeline.*;
import com.tonic.injector.util.*;
import com.tonic.patch.PatchGenerator;
import com.tonic.util.PackageUtil;
import com.tonic.util.asm.ClassNodeUtil;
import com.tonic.util.asm.SignerMapper;
import com.tonic.vitalite.Main;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        for(ClassNode mixin : mixins)
        {
            String target = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
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
                if(AnnotationUtil.hasAnnotation(method, ClassMod.class))
                {
                    ClassModTransformer.patch(mixin, method);
                }
            }
        }

        for (var entry : runelite.entrySet()) {
            String name = entry.getKey().replace("/", ".");
            if(SignerMapper.shouldIgnore(name))
            {
                continue;
            }
            RLGlobalMixin.patch(entry.getValue());
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
}
