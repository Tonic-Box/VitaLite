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
    void setClickStrategy(ClickStrategy strategy);

    @ConfigKey(value = "clickPointX", defaultValue = "-1")
    int getClickPointX();
    void setClickPointX(int x);

    @ConfigKey(value = "clickPointY", defaultValue = "-1")
    int getClickPointY();
    void setClickPointY(int y);

    @ConfigKey(value = "cachedRandomDat", defaultValue = "true")
    boolean shouldCacheRandomDat();
    void setShouldCacheRandomDat(boolean shouldCache);

    @ConfigKey(value = "cachedDeviceID", defaultValue = "true")
    boolean shouldCacheDeviceId();
    void setShouldCacheDeviceId(boolean shouldCache);

    @ConfigKey(value = "cachedBank", defaultValue = "true")
    boolean shouldCacheBank();
    void setShouldCacheBank(boolean shouldCache);

    @ConfigKey(value = "drawWalkerPath", defaultValue = "true")
    boolean shouldDrawWalkerPath();
    void setShouldDrawWalkerPath(boolean shouldDraw);
}
