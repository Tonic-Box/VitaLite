package com.tonic.mixins;

import com.tonic.api.TClient;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.MethodHook;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;

@Mixin("Client")
public abstract class TJagexAccountsMixin implements TClient
{
    @Shadow("JX_DISPLAY_NAME")
    public static String displayName;

    @Inject
    public static String characterId;

    @Shadow("setJxCharacterId")
    public abstract void setJxCharacterId(String id);

    @Shadow("setJxSessionId")
    public abstract void setJxSessionId(String id);

    @Shadow("setJxRefreshToken")
    public abstract void setJxRefreshToken(String token);

    @Shadow("setJxAccessToken")
    public abstract void setJxAccessToken(String token);

    @MethodHook("setJxCharacterId")
    public static void onCharacterIdSet(String id)
    {
        characterId = id;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String getCharacterId()
    {
        return characterId;
    }

    @Override
    public void setDisplayName(String name)
    {
        displayName = name;
    }

    @Override
    public void setCharacterId(String id)
    {
        setJxCharacterId(id);
    }

    @Override
    public void setSessionId(String id)
    {
        setJxSessionId(id);
    }

    @Override
    public void setRefreshToken(String token)
    {
        setJxRefreshToken(token);
    }

    @Override
    public void setAccessToken(String token)
    {
        setJxAccessToken(token);
    }
}
