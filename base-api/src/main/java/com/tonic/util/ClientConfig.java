package com.tonic.util;

import com.tonic.services.ClickStrategy;
import com.tonic.services.ConfigManager;
import com.tonic.util.config.ConfigGroup;
import com.tonic.util.config.ConfigKey;
import com.tonic.util.config.VitaConfig;

@ConfigGroup("VitaLiteOptions")
public interface ClientConfig extends VitaConfig {

    @ConfigKey(value = "clickStrategy", defaultValue = "STATIC")
    ClickStrategy getClickStrategy();
    @ConfigKey(value = "clickStrategy")
    void setClickStrategy(ClickStrategy strategy);

    @ConfigKey(value = "clickPointX", defaultValue = "-1")
    int getClickPointX();
    @ConfigKey(value = "clickPointX")
    void setClickPointX(int x);

    @ConfigKey(value = "clickPointY", defaultValue = "-1")
    int getClickPointY();
    @ConfigKey(value = "clickPointY")
    void setClickPointY(int y);

    @ConfigKey(value = "cachedRandomDat", defaultValue = "true")
    boolean shouldCacheRandomDat();
    @ConfigKey(value = "cachedRandomDat")
    void setShouldCacheRandomDat(boolean shouldCache);

    @ConfigKey(value = "cachedDeviceID", defaultValue = "true")
    boolean shouldCacheDeviceId();
    @ConfigKey(value = "cachedDeviceID")
    void setShouldCacheDeviceId(boolean shouldCache);

    @ConfigKey(value = "cachedBank", defaultValue = "true")
    boolean shouldCacheBank();
    @ConfigKey(value = "cachedBank")
    void setShouldCacheBank(boolean shouldCache);

    @ConfigKey(value = "drawWalkerPath", defaultValue = "true")
    boolean shouldDrawWalkerPath();
    @ConfigKey(value = "drawWalkerPath")
    void setShouldDrawWalkerPath(boolean shouldDraw);

    @ConfigKey(value = "drawCollision", defaultValue = "false")
    boolean shouldDrawCollision();
    @ConfigKey(value = "drawCollision")
    void setShouldDrawCollision(boolean shouldDraw);
}
