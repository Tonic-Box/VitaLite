package com.tonic.mixins;

import com.tonic.api.TClient;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.MethodHook;
import com.tonic.injector.annotations.Mixin;

/**
 * Keeps the Jagex character the client was last given, as the random.dat cache keys Jagex accounts by it. Setting the
 * account itself is RuneLite's Client.switchJagexAccount.
 */
@Mixin("Client")
public abstract class TJagexAccountsMixin implements TClient
{
    @Inject
    public static String characterId;

    @MethodHook("setJxCharacterId")
    public static void onCharacterIdSet(String id)
    {
        characterId = id;
    }

    @Override
    public String getCharacterId()
    {
        return characterId;
    }
}
