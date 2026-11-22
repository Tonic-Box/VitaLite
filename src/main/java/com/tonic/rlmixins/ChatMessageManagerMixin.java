package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Insert;
import com.tonic.injector.annotations.Mixin;

/**
 * Tells MirrorChat who queued each message and hands that on while the message is added. It runs while streamer mode is off too, so a message queued just before it is turned on is still known.
 */
@Mixin("net/runelite/client/chat/ChatMessageManager")
public class ChatMessageManagerMixin
{
    private static final String QUEUED = "(Lnet/runelite/client/chat/QueuedMessage;)V";
    private static final String MIRROR_CHAT = "com/tonic/services/streamer/MirrorChat";

    /**
     * Notes who queued a message.
     *
     * @param message the message queued
     */
    @Insert(method = "queue", desc = QUEUED, at = @At(value = AtTarget.HEAD))
    @Delegate(MIRROR_CHAT + ".queued")
    public static native void streamerMessageQueued(Object message);

    /**
     * Hands on who queued a message as it starts being added.
     *
     * @param message the message being added
     */
    @Insert(method = "add", desc = QUEUED, at = @At(value = AtTarget.HEAD))
    @Delegate(MIRROR_CHAT + ".adding")
    public static native void streamerMessageAdding(Object message);

    /**
     * Ends handing on who queued a message once it is added.
     */
    @Insert(method = "add", desc = QUEUED, at = @At(value = AtTarget.RETURN), all = true)
    @Delegate(MIRROR_CHAT + ".addDone")
    public static native void streamerMessageAdded();
}
