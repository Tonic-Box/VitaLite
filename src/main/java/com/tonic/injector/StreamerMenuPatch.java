package com.tonic.injector;

import com.tonic.injector.pipeline.RedirectTransformer;
import com.tonic.injector.util.InjectionFailures;
import com.tonic.injector.util.StreamerBridges;
import com.tonic.util.MappingProvider;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JMethod;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * Sends the mouseover text the client draws in each method that draws the menu to TClientMixin's streamerDrawMouseOverText while streamer mode is on, so the streamer mirror can show the text its stock-only menu would give.
 */
public final class StreamerMenuPatch
{
    private static final String HOOK = "streamerDrawMouseOverText";

    private StreamerMenuPatch()
    {
    }

    /**
     * Patches the client class, leaving every other class alone.
     *
     * @param classNode a gamepack class
     */
    public static void patch(ClassNode classNode)
    {
        JClass client = MappingProvider.getClass("Client");
        JClass menu = MappingProvider.getClass("Menu");
        if (client == null || menu == null || !classNode.name.equals(client.getObfuscatedName()))
        {
            return;
        }
        JMethod draw = MappingProvider.getMethod(menu, "draw");
        JMethod drawText = MappingProvider.getStaticMethod("drawText");
        MethodNode hook = StreamerBridges.method(classNode, HOOK);
        if (!InjectionFailures.require(draw != null && drawText != null, "streamer menu patch: Menu.draw or drawText is not mapped") || !InjectionFailures.require(hook != null, "streamer menu patch: " + HOOK + " was not injected into the client"))
        {
            return;
        }

        int patched = 0;
        for (MethodNode method : classNode.methods)
        {
            if (method == hook || StreamerBridges.callsTo(method, draw).isEmpty())
            {
                continue;
            }
            List<MethodInsnNode> texts = StreamerBridges.callsTo(method, drawText);
            if (!InjectionFailures.require(texts.size() == 1, "streamer menu patch: " + texts.size() + " text draws in " + method.name + method.desc + ", which draws the menu, where one was expected"))
            {
                continue;
            }
            RedirectTransformer.redirect(method, texts.get(0), classNode.name, hook, StreamerBridges.STREAMER_MIRRORING);
            patched++;
        }
        InjectionFailures.require(patched > 0, "streamer menu patch: no client method draws the menu");
    }
}
