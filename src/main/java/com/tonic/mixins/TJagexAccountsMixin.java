package com.tonic.mixins;

import com.tonic.api.TClient;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;

@Mixin("Client")
public abstract class TJagexAccountsMixin implements TClient
{
    @Shadow("JX_DISPLAY_NAME")
    public static String displayName;

    @Shadow("JX_CHARACTER_ID")
    public static String characterId;

    @Shadow("JX_SESSION_ID")
    public static String sessionId;

    @Shadow("JX_REFRESH_TOKEN")
    public static String refreshToken;

    @Shadow("JX_ACCESS_TOKEN")
    public static String accessToken;

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
        characterId = id;
    }

    @Override
    public void setSessionId(String id)
    {
        sessionId = id;
    }

    @Override
    public void setRefreshToken(String token)
    {
        refreshToken = token;
    }

    @Override
    public void setAccessToken(String token)
    {
        accessToken = token;
    }
}
