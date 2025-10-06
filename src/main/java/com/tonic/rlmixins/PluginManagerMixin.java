package com.tonic.rlmixins;

import com.tonic.injector.annotations.*;
import com.tonic.injector.util.BytecodeBuilder;
import com.tonic.injector.util.LogCallRemover;
import com.tonic.model.ConditionType;
import com.tonic.vitalite.Main;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;

@Mixin("net/runelite/client/plugins/PluginManager")
public class PluginManagerMixin {
    private static final List<String> allowed = new ArrayList<>()
    {{
        add("net.runelite.client.plugins.config");
        add("net.runelite.client.plugins.lowmemory");
        add("net.runelite.client.plugins.loginscreen");
    }};

    private static final List<String> disallowed = new ArrayList<>()
    {{
        add("net.runelite.client.plugins.account");
        add("net.runelite.client.plugins.crowdsourcing");
        add("net.runelite.client.plugins.discord");
        add("net.runelite.client.plugins.twitch");
        add("net.runelite.client.plugins.xtea");
    }};

    @Insert(
            method = "loadSideLoadPlugins",
            at = @At(
                    value = AtTarget.GETFIELD,
                    owner = "net/runelite/client/plugins/PluginManager",
                    target = "developerMode"
            ),
            raw = true
    )
    public static void loadSideLoadPlugins(MethodNode method, AbstractInsnNode insertionPoint)
    {
        InsnList list = BytecodeBuilder.create()
                .pop()
                .pushInt(1)
                .build();
        method.instructions.insert(insertionPoint, list);
        LogCallRemover.removeLogInfoCall(method);
    }

    @Insert(
            method = "loadPlugins",
            at = @At(
                    value = AtTarget.GETFIELD,
                    owner = "net/runelite/client/plugins/PluginManager",
                    target = "developerMode"
            ),
            raw = true
    )
    public static void loadPlugins(MethodNode method, AbstractInsnNode insertionPoint)
    {
        if(Main.optionsParser.isIncognito())
            return;

        InsnList code = BytecodeBuilder.create()
                .pushInt(1)
                .build();

        method.instructions.insert(insertionPoint, code);
        method.instructions.remove(insertionPoint.getPrevious());
        method.instructions.remove(insertionPoint);

        // Replace "getSuperclass() == Plugin.class" with "Plugin.class.isAssignableFrom(getSuperclass())"
        InsnList instructions = method.instructions;
        AbstractInsnNode target = null;
        for(AbstractInsnNode insn : instructions.toArray())
        {
            if(insn.getOpcode() != Opcodes.INVOKEVIRTUAL)
                continue;
            MethodInsnNode methodInsn = (MethodInsnNode) insn;
            if(!methodInsn.owner.equals("java/lang/Class") || !methodInsn.name.equals("getSuperclass"))
                continue;

            if(insn.getNext().getNext().getOpcode() != Opcodes.IF_ACMPEQ)
                continue;

            target = insn;
        }

        if(target != null) {
            AbstractInsnNode ldc = target.getNext();
            AbstractInsnNode jump = ldc.getNext();
            LabelNode label = ((JumpInsnNode)jump).label;

            // Remove the LDC and IF_ACMPEQ
            instructions.remove(ldc);
            instructions.remove(jump);

            // Add: Plugin.class.isAssignableFrom(getSuperclass result)
            instructions.insert(target, new LdcInsnNode(Type.getType("Lnet/runelite/client/plugins/Plugin;")));
            instructions.insert(target.getNext(), new InsnNode(Opcodes.SWAP));
            instructions.insert(target.getNext().getNext(), new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Class",
                    "isAssignableFrom",
                    "(Ljava/lang/Class;)Z",
                    false
            ));
            instructions.insert(target.getNext().getNext().getNext(), new JumpInsnNode(Opcodes.IFNE, label));
        }
    }

    @Insert(
            method = "loadCorePlugins",
            at = @At(
                    value = AtTarget.STORE,
                    local = 2
            ),
            ordinal = -1,
            raw = true
    )
    public static void loadCorePlugins(MethodNode method, AbstractInsnNode insertionPoint)
    {
        InsnList code = BytecodeBuilder.create()
                // Static.set(new RuneLite(Main.CLASSLOADER.getMain()), "RL");
                .newInstance("com/tonic/model/RuneLite")
                .dup()
                .getStaticField(
                        "com/tonic/vitalite/Main",
                        "CLASSLOADER",
                        "Lcom/tonic/classloader/RLClassLoader;"
                )
                .invokeVirtual(
                        "com/tonic/classloader/RLClassLoader",
                        "getMain",
                        "()Ljava/lang/Class;"
                )
                .invokeSpecial(
                        "com/tonic/model/RuneLite",
                        "<init>",
                        "(Ljava/lang/Class;)V"
                )
                .pushString("RL")
                .invokeStatic(
                        "com/tonic/Static",
                        "set",
                        "(Ljava/lang/Object;Ljava/lang/String;)V"
                )

                .invokeStatic(
                        "com/tonic/services/GameManager",
                        "init",
                        "()V"
                )

                // RuneLite.injector.getInstance(Install.class).injectSideLoadPlugins(plugins);
                .invokeStatic("com/tonic/Static",
                        "getInjector",
                        "()Lcom/google/inject/Injector;")
                .pushClass("com/tonic/runelite/Install")
                .invokeInterface("com/google/inject/Injector",
                        "getInstance",
                        "(Ljava/lang/Class;)Ljava/lang/Object;")
                .castToType("com/tonic/runelite/Install")
                .loadLocal(2, Opcodes.ALOAD)
                .invokeVirtual("com/tonic/runelite/Install",
                        "injectBuiltInPlugins",
                        "(Ljava/util/List;)V")
                .build();


        method.instructions.insert(insertionPoint, code);

        boolean isMin = Main.optionsParser.isNoPlugins() || Main.optionsParser.isMin();

        InsnList code2 = BytecodeBuilder.create()
                .newInstance("java/util/ArrayList")
                .dup()
                .invokeSpecial("java/util/ArrayList", "<init>", "()V")
                .storeLocal(3, ASTORE)
                .loadLocal(2, ALOAD)
                .invokeInterface("java/util/List", "iterator", "()Ljava/util/Iterator;")
                .storeLocal(4, ASTORE)
                .whileBlock(
                        ConditionType.EQUALS,
                        condition -> condition.loadLocal(4, ALOAD)
                                .invokeInterface("java/util/Iterator", "hasNext", "()Z")
                                .pushInt(0),
                        body -> {
                            body.loadLocal(4, ALOAD)
                                    .invokeInterface("java/util/Iterator", "next", "()Ljava/lang/Object;")
                                    .castToType("java/lang/Class")
                                    .storeLocal(5, ASTORE)
                                    .loadLocal(5, ALOAD)
                                    .invokeVirtual("java/lang/Class", "getName", "()Ljava/lang/String;")
                                    .storeLocal(6, ASTORE);
                            LabelNode skipAdd = body.createLabel("skipAdd");
                            body.pushInt(0);
                            if(isMin)
                            {
                                for(String prefix : allowed)
                                {
                                    body.loadLocal(6, ALOAD)
                                            .pushString(prefix)
                                            .invokeVirtual("java/lang/String", "startsWith", "(Ljava/lang/String;)Z")
                                            .or();
                                }
                            }
                            else
                            {
                                for(String prefix : disallowed)
                                {
                                    body.loadLocal(6, ALOAD)
                                            .pushString(prefix)
                                            .invokeVirtual("java/lang/String", "startsWith", "(Ljava/lang/String;)Z")
                                            .or();
                                }
                            }
                            ConditionType cond = isMin ? ConditionType.EQUALS : ConditionType.NOT_EQUALS;
                            body.pushInt(0)
                                    .jumpIf(cond, skipAdd)
                                    .loadLocal(3, ALOAD)
                                    .loadLocal(5, ALOAD)
                                    .invokeInterface("java/util/List", "add", "(Ljava/lang/Object;)Z")
                                    .pop()
                                    .placeLabel(skipAdd);
                        }
                )
                .loadLocal(3, ALOAD)
                .storeLocal(2, ASTORE)
                .build();

        method.instructions.insert(insertionPoint, code2);
    }
}
