package com.tonic.injector;

import com.tonic.injector.util.AnnotationUtil;
import com.tonic.injector.util.*;
import com.tonic.vitalite.Main;
import com.tonic.util.dto.JClass;
import com.tonic.injector.annotations.*;
import com.tonic.injector.pipeline.*;
import com.tonic.util.JarDumper;
import com.tonic.util.PackageUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Injector {
    private static final String MIXINS = "com.tonic.mixins";
    // Pre-sized HashMap for ~17K classes - saves 20-30MB during resize churn
    public static HashMap<String, ClassNode> gamepack = new HashMap<>(17500, 0.75f);

    public static void patch() throws Exception {
        int totalClasses = Main.LIBS.getGamepack().classes.size();
        System.out.println("Loading " + totalClasses + " classes with memory optimizations...");

        // Phase 1: Identify mixin target classes
        HashMap<ClassNode, ClassNode> pairs = PackageUtil.getPairs(MIXINS);
        Set<String> mixinTargets = identifyMixinTargets(pairs);

        System.out.println("Identified " + mixinTargets.size() + " mixin target classes (full frame expansion)");
        System.out.println("Remaining " + (totalClasses - mixinTargets.size()) + " classes use optimized loading (skip frames)");

        // Phase 2: Load all gamepack classes with selective frame expansion
        for (var entry : Main.LIBS.getGamepack().classes.entrySet()) {
            String name = entry.getKey();
            byte[] bytes = entry.getValue();

            // Mixin targets need full frame expansion, others can skip frames (saves 200-300MB)
            boolean needsFrames = mixinTargets.contains(name);
            gamepack.put(name, ClassNodeUtil.toNode(bytes, needsFrames));
        }

        System.out.println("Classes loaded. Applying mixins...");

        applyInterfaces(pairs);
        applyMixins(pairs);

        System.out.println("Mixins applied. Processing output...");

        // Optimized output phase: Process and clear classes one-by-one to reduce memory pressure
        ArrayList<String> classNames = new ArrayList<>(gamepack.keySet());
        for (String name : classNames) {
            if(SignerMapper.shouldIgnore(name))
            {
                System.out.println("Skipping cert-checked class: " + name);
                gamepack.remove(name);
                continue;
            }

            // Store original bytecode for patch generation
            byte[] original = Main.LIBS.getGamepack().classes.get(name);
            PatchGenerator.storeOriginalGamepack(name, original);

            ClassNode classNode = gamepack.remove(name); // Remove from map immediately
            FieldHookTransformer.instrument(classNode);
            OSGlobalMixin.patch(classNode);

            byte[] modified = ClassNodeUtil.toBytes(classNode);
            Main.LIBS.getGamepack().classes.put(name, modified);

            // Capture diff if patch generation is enabled
            PatchGenerator.captureGamepackDiff(name, modified);

            StripAnnotationsTransformer.stripAnnotations(classNode);
            Main.LIBS.getGamepackClean().classes.put(name, ClassNodeUtil.toBytes(classNode));

            // Help GC by clearing reference immediately
            classNode = null;
        }
        gamepack.clear();

        // Clear string/type pools to release memory
        ClassNodeUtil.clearPools();
        System.out.println("Memory pools cleared. Injection complete.");

        JarDumper.dump(Main.LIBS.getGamepackClean().classes);
    }

    /**
     * Scans mixin annotations to identify which gamepack classes will be transformation targets.
     * These classes need full frame expansion. All others can use SKIP_FRAMES for memory savings.
     */
    private static Set<String> identifyMixinTargets(HashMap<ClassNode, ClassNode> pairs) {
        Set<String> targets = new HashSet<>();

        for (ClassNode mixin : pairs.keySet()) {
            try {
                String gamepackName = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
                JClass jClass = MappingProvider.getClass(gamepackName);
                if (jClass != null) {
                    targets.add(jClass.getObfuscatedName());
                }
            } catch (Exception e) {
                // Continue processing other mixins
                System.err.println("Warning: Failed to identify target for mixin " + mixin.name);
            }
        }

        return targets;
    }

    private static void applyMixins(HashMap<ClassNode, ClassNode> pairs) throws ClassNotFoundException {
        for (ClassNode mixin : pairs.keySet()) {
            StripLvtInfo.run(mixin);
            String gamepackName = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
            Boolean isCheatIdentifier = AnnotationUtil.getAnnotation(mixin, Mixin.class, "isInterface");
            isCheatIdentifier = isCheatIdentifier != null && isCheatIdentifier;
            ClassNode gamepackClass = null;
            if(isCheatIdentifier)
            {
                gamepackClass = gamepack.values().stream()
                        .filter(cn -> cn.interfaces != null && cn.interfaces.contains(gamepackName))
                        .findFirst()
                        .orElse(null);
            }
            else
            {
                JClass clazz = MappingProvider.getClass(gamepackName);
                if(clazz == null)
                {
                    throw new ClassNotFoundException("Could not find mapping for mixin target class: " + gamepackName);
                }
                gamepackClass = gamepack.get(clazz.getObfuscatedName());
            }
            if(gamepackClass == null)
            {
                throw new ClassNotFoundException("Could not find target class for mixin: " + gamepackName);
            }
            for(FieldNode field : mixin.fields)
            {
                if(AnnotationUtil.hasAnnotation(field, Inject.class))
                {
                    InjectTransformer.patch(gamepackClass, field);
                }
                if(AnnotationUtil.hasAnnotation(field, Shadow.class))
                {
                    ShadowTransformer.patch(mixin, field);
                }
            }

            BootstrapAttributeCopier.copyBootstrapAttributesAndCallsites(mixin, gamepackClass);

            for(MethodNode method : mixin.methods)
            {
                if(AnnotationUtil.hasAnnotation(method, Inject.class) || !AnnotationUtil.hasAnyAnnotation(method))
                {
                    InjectTransformer.patch(gamepackClass, mixin, method);
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
                if(AnnotationUtil.hasAnnotation(method, FieldHook.class))
                {
                    FieldHookTransformer.patch(mixin, method);
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
    }

    private static void applyInterfaces(HashMap<ClassNode, ClassNode> pairs) {
        for (var entry : pairs.entrySet()) {
            try
            {
                ClassNode api = entry.getValue();
                if(api == null)
                {
                    continue;
                }
                ClassNode mixin = entry.getKey();
                String gamepackName = AnnotationUtil.getAnnotation(mixin, Mixin.class, "value");
                Boolean isCheatIdentifier = AnnotationUtil.getAnnotation(api, Mixin.class, "isInterface");
                isCheatIdentifier = isCheatIdentifier != null && isCheatIdentifier;
                ClassNode gamepackClass = null;
                if(isCheatIdentifier)
                {
                    gamepackClass = gamepack.values().stream()
                            .filter(cn -> cn.interfaces != null && cn.interfaces.contains(gamepackName))
                            .findFirst()
                            .orElse(null);
                }
                else
                {
                    JClass clazz = MappingProvider.getClass(gamepackName);
                    if(clazz == null)
                    {
                        System.err.println("Warning: Could not find mapping for mixin target class: " + gamepackName);
                        continue;
                    }
                    gamepackClass = gamepack.get(clazz.getObfuscatedName());
                }

                if(gamepackClass == null)
                {
                    System.err.println("Warning: Could not find target class for interface mixin: " + gamepackName);
                    continue;
                }

                if(gamepackClass.interfaces == null)
                {
                    gamepackClass.interfaces = new ArrayList<>();
                }
                if(gamepackClass.interfaces.contains(api.name))
                {
                    continue;
                }
                gamepackClass.interfaces.add(api.name);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
    }
}
