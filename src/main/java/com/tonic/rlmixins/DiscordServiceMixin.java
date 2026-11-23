package com.tonic.rlmixins;

import com.tonic.injector.annotations.At;
import com.tonic.injector.annotations.AtTarget;
import com.tonic.injector.annotations.Delegate;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Redirect;

/**
 * Passes RuneLite's Discord connection and presence through StreamerPresence, which connects to Discord and shows the presence only while streamer mode is on.
 */
@Mixin("net/runelite/client/discord/DiscordService")
public class DiscordServiceMixin
{
    private static final String DISCORD_RPC = "net/runelite/discord/DiscordRPC";
    private static final String PRESENCE = "com/tonic/services/streamer/StreamerPresence";

    /**
     * Hands RuneLite's connection to Discord to StreamerPresence, which connects while streamer mode is on.
     *
     * @param rpc the Discord library
     * @param appId RuneLite's Discord application id
     * @param handlers RuneLite's Discord callbacks
     * @param autoRegister whether Discord registers the application
     * @param steamId the Steam id, if any
     */
    @Redirect(method = "init", at = @At(value = AtTarget.INVOKE, owner = DISCORD_RPC, target = "Discord_Initialize"), expectedMatches = 1)
    @Delegate(PRESENCE + ".initialize")
    public static native void streamerInitialize(Object rpc, String appId, Object handlers, boolean autoRegister, String steamId);

    /**
     * Hands each presence to StreamerPresence, which sends it while streamer mode is on.
     *
     * @param rpc the Discord library
     * @param presence the presence
     */
    @Redirect(method = "updatePresence", at = @At(value = AtTarget.INVOKE, owner = DISCORD_RPC, target = "Discord_UpdatePresence"), expectedMatches = 1)
    @Delegate(PRESENCE + ".update")
    public static native void streamerUpdate(Object rpc, Object presence);

    /**
     * Hands each presence clear to StreamerPresence.
     *
     * @param rpc the Discord library
     */
    @Redirect(method = "clearPresence", at = @At(value = AtTarget.INVOKE, owner = DISCORD_RPC, target = "Discord_ClearPresence"), expectedMatches = 1)
    @Delegate(PRESENCE + ".clear")
    public static native void streamerClear(Object rpc);
}
