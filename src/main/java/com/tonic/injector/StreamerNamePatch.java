package com.tonic.injector;

import com.tonic.injector.util.Gates;
import com.tonic.injector.util.InjectionFailures;
import com.tonic.injector.util.StreamerBridges;
import com.tonic.util.MappingProvider;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JField;
import com.tonic.util.dto.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends the client's text draws that can hold the player's name through bridges that report to StreamerName, so the streamer mirror can show the streamer name in its place: interface text, overhead text and the login screen, where VitaLite's welcome line is drawn too and the mirror shows the stock one.
 */
public final class StreamerNamePatch
{
    private static final String STREAMER_NAME = "com/tonic/services/streamer/StreamerName";
    private static final String BRIDGE = "streamer$text$";
    private static final String STRING = "Ljava/lang/String;";
    private static final int TEXT = 1;
    private static final int X = 2;
    private static final int Y = 3;
    private static final int WIDTH = 4;
    private static final int HEIGHT = 5;

    private static boolean mappingsReported;

    private enum Mode
    {
        BAND,
        BOX,
        LOGIN
    }

    private StreamerNamePatch()
    {
    }

    /**
     * Patches the widget drawer, the overhead text drawer and the login screen, leaving every other class alone.
     *
     * @param classNode a gamepack class
     */
    public static void patch(ClassNode classNode)
    {
        JClass font = MappingProvider.getClass("Font");
        JClass abstractFont = MappingProvider.getClass("AbstractFont");
        JClass drawer = MappingProvider.getClass("WidgetDrawer");
        JClass strings = MappingProvider.getClass("Strings");
        JMethod drawLines = abstractFont == null ? null : MappingProvider.getMethod(abstractFont, "drawLines");
        JMethod drawWidgets = drawer == null ? null : MappingProvider.getMethod(drawer, "drawWidgets");
        JMethod drawOverheadText = MappingProvider.getStaticMethod("drawOverheadText");
        JMethod drawLoginScreen = MappingProvider.getStaticMethod("drawLoginScreen");
        JField welcome = strings == null ? null : MappingProvider.getField(strings, "welcomeToRuneScape");
        if (font == null || drawLines == null || drawWidgets == null || drawOverheadText == null || drawLoginScreen == null || welcome == null)
        {
            if (!mappingsReported)
            {
                mappingsReported = true;
                InjectionFailures.record("streamer name patch: Font, AbstractFont.drawLines, WidgetDrawer.drawWidgets, drawOverheadText, drawLoginScreen or Strings.welcomeToRuneScape is not mapped");
            }
            return;
        }
        Fonts fonts = new Fonts(font.getObfuscatedName(), abstractFont.getObfuscatedName(), drawLines);

        Map<String, MethodNode> bridges = new LinkedHashMap<>();
        patchMethod(classNode, drawWidgets, Mode.BAND, fonts, bridges);
        patchMethod(classNode, drawOverheadText, Mode.BAND, fonts, bridges);
        MethodNode loginScreen = patchMethod(classNode, drawLoginScreen, Mode.LOGIN, fonts, bridges);
        if (loginScreen != null)
        {
            passWelcome(loginScreen, welcome);
        }
        classNode.methods.addAll(bridges.values());
    }

    private static MethodNode patchMethod(ClassNode classNode, JMethod target, Mode mode, Fonts fonts, Map<String, MethodNode> bridges)
    {
        if (!classNode.name.equals(target.getOwnerObfuscatedName()))
        {
            return null;
        }
        MethodNode method = StreamerBridges.find(classNode, target);
        if (!InjectionFailures.require(method != null, "streamer name patch: " + target.getName() + " not found in " + classNode.name))
        {
            return null;
        }

        List<MethodInsnNode> draws = StreamerBridges.callsTo(method, fonts::isTextDraw);
        InjectionFailures.require(!draws.isEmpty(), "streamer name patch: no text draws in " + target.getName());
        for (MethodInsnNode draw : draws)
        {
            Mode drawMode = fonts.isLines(draw) ? Mode.BOX : mode;
            String key = draw.getOpcode() + " " + draw.owner + "." + draw.name + draw.desc + " " + drawMode;
            MethodNode bridge = bridges.get(key);
            if (bridge == null)
            {
                bridge = bridge(BRIDGE + bridges.size(), draw, drawMode);
                if (bridge == null)
                {
                    continue;
                }
                bridges.put(key, bridge);
            }
            method.instructions.set(draw, new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, bridge.name, bridge.desc, false));
        }
        return method;
    }

    private static void passWelcome(MethodNode loginScreen, JField welcome)
    {
        int passed = 0;
        for (AbstractInsnNode insn : loginScreen.instructions.toArray())
        {
            if (insn.getOpcode() == Opcodes.GETSTATIC && ((FieldInsnNode) insn).owner.equals(welcome.getOwnerObfuscatedName()) && ((FieldInsnNode) insn).name.equals(welcome.getObfuscatedName()))
            {
                loginScreen.instructions.insert(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, StreamerBridges.STREAMER, "welcomeText", "(Ljava/lang/String;)Ljava/lang/String;", false));
                passed++;
            }
        }
        InjectionFailures.require(passed > 0, "streamer name patch: the login screen does not read Strings.welcomeToRuneScape");
    }

    private static MethodNode bridge(String name, MethodInsnNode draw, Mode mode)
    {
        String desc = StreamerBridges.receiverFirst(draw);
        Type[] arguments = Type.getArgumentTypes(desc);
        if (!InjectionFailures.require(fits(arguments, mode), "streamer name patch: " + draw.owner + "." + draw.name + draw.desc + " does not take its text and place where " + mode + " draws expect"))
        {
            return null;
        }
        MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, desc, null, null);
        Type returned = Type.getReturnType(desc);
        int how = StreamerBridges.argumentSlots(desc);
        int result = how + 1;

        LabelNode plain = new LabelNode();
        LabelNode replacedOnly = new LabelNode();
        LabelNode guarded = new LabelNode();
        LabelNode guardEnd = new LabelNode();
        InsnList code = bridge.instructions;

        code.add(Gates.read(StreamerBridges.STREAMER_MIRRORING));
        code.add(new JumpInsnNode(Opcodes.IFEQ, plain));
        code.add(new VarInsnNode(Opcodes.ALOAD, TEXT));
        switch (mode)
        {
            case BOX:
                code.add(new VarInsnNode(Opcodes.ILOAD, X));
                code.add(new VarInsnNode(Opcodes.ILOAD, Y));
                code.add(new VarInsnNode(Opcodes.ILOAD, WIDTH));
                code.add(new VarInsnNode(Opcodes.ILOAD, HEIGHT));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STREAMER_NAME, "beginBox", "(Ljava/lang/String;IIII)I", false));
                break;
            case LOGIN:
                code.add(new VarInsnNode(Opcodes.ILOAD, Y));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STREAMER_NAME, "beginLogin", "(Ljava/lang/String;I)I", false));
                break;
            default:
                code.add(new VarInsnNode(Opcodes.ILOAD, Y));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STREAMER_NAME, "begin", "(Ljava/lang/String;I)I", false));
                break;
        }
        code.add(new VarInsnNode(Opcodes.ISTORE, how));
        code.add(new VarInsnNode(Opcodes.ILOAD, how));
        code.add(new JumpInsnNode(Opcodes.IFEQ, plain));
        code.add(new VarInsnNode(Opcodes.ILOAD, how));
        code.add(new InsnNode(Opcodes.ICONST_2));
        code.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, replacedOnly));

        code.add(guarded);
        code.add(call(draw, arguments, false));
        if (returned.getSort() != Type.VOID)
        {
            code.add(new VarInsnNode(returned.getOpcode(Opcodes.ISTORE), result));
        }
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STREAMER_NAME, "mirror", "()V", false));
        code.add(call(draw, arguments, true));
        if (returned.getSort() != Type.VOID)
        {
            code.add(new InsnNode(returned.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
        }
        code.add(guardEnd);
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STREAMER_NAME, "done", "()V", false));
        if (returned.getSort() != Type.VOID)
        {
            code.add(new VarInsnNode(returned.getOpcode(Opcodes.ILOAD), result));
        }
        code.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));

        code.add(replacedOnly);
        code.add(call(draw, arguments, true));
        code.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));

        code.add(plain);
        code.add(call(draw, arguments, false));
        code.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));

        StreamerBridges.guard(bridge, guarded, guardEnd, STREAMER_NAME, "done");
        return bridge;
    }

    private static boolean fits(Type[] arguments, Mode mode)
    {
        int last = mode == Mode.BOX ? HEIGHT : Y;
        if (arguments.length <= last || !arguments[TEXT].getDescriptor().equals(STRING))
        {
            return false;
        }
        for (int i = X; i <= last; i++)
        {
            if (arguments[i] != Type.INT_TYPE)
            {
                return false;
            }
        }
        return true;
    }

    private static InsnList call(MethodInsnNode draw, Type[] arguments, boolean replaced)
    {
        return StreamerBridges.call(draw, arguments, i -> replaced && i == TEXT ? text() : null);
    }

    private static InsnList text()
    {
        InsnList text = new InsnList();
        text.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STREAMER_NAME, "text", "()Ljava/lang/String;", false));
        return text;
    }

    /**
     * The client's font classes and which of their calls draw text.
     */
    private static final class Fonts
    {
        private final String font;
        private final String abstractFont;
        private final JMethod drawLines;

        private Fonts(String font, String abstractFont, JMethod drawLines)
        {
            this.font = font;
            this.abstractFont = abstractFont;
            this.drawLines = drawLines;
        }

        private boolean isTextDraw(MethodInsnNode call)
        {
            if (!call.owner.equals(font) && !call.owner.equals(abstractFont))
            {
                return false;
            }
            Type[] arguments = Type.getArgumentTypes(call.desc);
            int first = 0;
            if (call.getOpcode() == Opcodes.INVOKESTATIC)
            {
                if (arguments.length == 0 || !isFont(arguments[0]))
                {
                    return false;
                }
                first = 1;
            }
            else if (call.getOpcode() != Opcodes.INVOKEVIRTUAL)
            {
                return false;
            }
            if (arguments.length < first + 3 || !arguments[first].getDescriptor().equals(STRING) || arguments[first + 1] != Type.INT_TYPE || arguments[first + 2] != Type.INT_TYPE)
            {
                return false;
            }
            return Type.getReturnType(call.desc) == Type.VOID_TYPE || isLines(call);
        }

        private boolean isLines(MethodInsnNode call)
        {
            return call.getOpcode() == Opcodes.INVOKEVIRTUAL && call.name.equals(drawLines.getObfuscatedName()) && call.desc.equals(drawLines.getDescriptor());
        }

        private boolean isFont(Type type)
        {
            return type.getSort() == Type.OBJECT && (type.getInternalName().equals(font) || type.getInternalName().equals(abstractFont));
        }
    }
}
