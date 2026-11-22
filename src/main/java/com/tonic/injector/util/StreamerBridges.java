package com.tonic.injector.util;

import com.tonic.util.dto.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * What the streamer mode patches share: the gate that says streamer mode is on, finding calls and methods by their mapping, calls that pass a bridge's arguments on, and the cleanup that runs when the client's draw throws inside a bridge.
 */
public final class StreamerBridges
{
    /**
     * The class whose static fields say whether streamer mode is on and whether the frame being drawn is handed to the mirror.
     */
    public static final String STREAMER = "com/tonic/services/streamer/Streamer";

    /**
     * The gate injected hooks name to run only while streamer mode is on.
     */
    public static final String STREAMER_ACTIVE = STREAMER + ".active";

    /**
     * The gate injected hooks that draw for the mirror name to run only while the client draws a frame handed to the mirror.
     */
    public static final String STREAMER_MIRRORING = STREAMER + ".mirroring";

    private StreamerBridges()
    {
    }

    /**
     * @param call a call in the gamepack
     * @param target a mapped method
     * @return whether the call is to that method
     */
    public static boolean matches(MethodInsnNode call, JMethod target)
    {
        return call.owner.equals(target.getOwnerObfuscatedName()) && call.name.equals(target.getObfuscatedName()) && call.desc.equals(target.getDescriptor());
    }

    /**
     * @param method a method
     * @param target a mapped method
     * @return the method's calls to it, in order
     */
    public static List<MethodInsnNode> callsTo(MethodNode method, JMethod target)
    {
        return callsTo(method, call -> matches(call, target));
    }

    /**
     * @param method a method
     * @param test which calls to keep
     * @return the method's calls that pass the test, in order
     */
    public static List<MethodInsnNode> callsTo(MethodNode method, Predicate<MethodInsnNode> test)
    {
        List<MethodInsnNode> calls = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions)
        {
            if (insn instanceof MethodInsnNode && test.test((MethodInsnNode) insn))
            {
                calls.add((MethodInsnNode) insn);
            }
        }
        return calls;
    }

    /**
     * @param classNode a class
     * @param target a mapped method of that class
     * @return the class's method, or null when it has none
     */
    public static MethodNode find(ClassNode classNode, JMethod target)
    {
        for (MethodNode method : classNode.methods)
        {
            if (method.name.equals(target.getObfuscatedName()) && method.desc.equals(target.getDescriptor()))
            {
                return method;
            }
        }
        return null;
    }

    /**
     * @param classNode a class
     * @param name a method name the class has once
     * @return the method, or null when the class has none or more than one by that name
     */
    public static MethodNode method(ClassNode classNode, String name)
    {
        MethodNode found = null;
        for (MethodNode method : classNode.methods)
        {
            if (method.name.equals(name))
            {
                if (found != null)
                {
                    return null;
                }
                found = method;
            }
        }
        return found;
    }

    /**
     * @param call a call
     * @return the descriptor of a static method that takes the call's receiver first and then its arguments, or the call's own for a static call
     */
    public static String receiverFirst(MethodInsnNode call)
    {
        return call.getOpcode() == Opcodes.INVOKESTATIC ? call.desc : "(L" + call.owner + ";" + call.desc.substring(1);
    }

    /**
     * @param desc a static method's descriptor
     * @return how many local slots its arguments take
     */
    public static int argumentSlots(String desc)
    {
        return (Type.getArgumentsAndReturnSizes(desc) >> 2) - 1;
    }

    /**
     * Builds a call that passes the arguments a bridge was given on, each loaded from its local unless a replacement is given for it.
     *
     * @param original the call the bridge stands in for
     * @param arguments the bridge's arguments, the receiver first for a call to an instance method
     * @param replace gives the code that loads an argument in place of its local, by the argument's index, or null to load the local
     * @return the call
     */
    public static InsnList call(MethodInsnNode original, Type[] arguments, IntFunction<InsnList> replace)
    {
        InsnList call = new InsnList();
        int local = 0;
        for (int i = 0; i < arguments.length; i++)
        {
            InsnList replaced = replace == null ? null : replace.apply(i);
            if (replaced != null)
            {
                call.add(replaced);
            }
            else
            {
                call.add(new VarInsnNode(arguments[i].getOpcode(Opcodes.ILOAD), local));
            }
            local += arguments[i].getSize();
        }
        call.add(new MethodInsnNode(original.getOpcode(), original.owner, original.name, original.desc, original.itf));
        return call;
    }

    /**
     * Has an exception thrown between two labels of a bridge run a cleanup method before it leaves the bridge, so a draw the bridge opened for the mirror is closed. The handler is added at the end of the bridge, which must not fall through into it.
     *
     * @param bridge the bridge
     * @param start where the guarded code starts
     * @param end where the guarded code ends
     * @param owner the cleanup method's owner
     * @param cleanup the cleanup method, static and taking no arguments
     */
    public static void guard(MethodNode bridge, LabelNode start, LabelNode end, String owner, String cleanup)
    {
        LabelNode handler = new LabelNode();
        bridge.instructions.add(handler);
        bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, cleanup, "()V", false));
        bridge.instructions.add(new InsnNode(Opcodes.ATHROW));
        bridge.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));
    }
}
