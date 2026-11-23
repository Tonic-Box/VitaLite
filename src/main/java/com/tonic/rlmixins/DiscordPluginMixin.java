package com.tonic.rlmixins;

import com.tonic.injector.annotations.MethodOverride;
import com.tonic.injector.annotations.Mixin;

/**
 * Turns off the Discord plugin's party features, so it neither sends the player's Discord account to party members nor fetches theirs.
 */
@Mixin("net/runelite/client/plugins/discord/DiscordPlugin")
public class DiscordPluginMixin
{
    /**
     * Sends nothing to the party when it syncs.
     *
     * @param sync the party sync
     */
    @MethodOverride("onUserSync")
    public void onUserSync(Object sync)
    {
    }

    /**
     * Ignores the Discord accounts party members send.
     *
     * @param info a party member's Discord account
     */
    @MethodOverride("onDiscordUserInfo")
    public void onDiscordUserInfo(Object info)
    {
    }
}
