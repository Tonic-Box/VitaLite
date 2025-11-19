package com.tonic.util;

import com.tonic.services.ClickStrategy;
import com.tonic.services.ConfigManager;
import com.tonic.services.pathfinder.PathfinderAlgo;
import com.tonic.util.config.ConfigGroup;
import com.tonic.util.config.ConfigKey;
import com.tonic.util.config.VitaConfig;

import java.nio.file.Path;

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

    @ConfigKey(value = "pathfinderImpl", defaultValue = "HYBRID_BFS")
    PathfinderAlgo getPathfinderImpl();
    @ConfigKey(value = "pathfinderImpl")
    void setPathfinderImpl(PathfinderAlgo impl);

    @ConfigKey(value = "drawWalkerPath", defaultValue = "true")
    boolean shouldDrawWalkerPath();
    @ConfigKey(value = "drawWalkerPath")
    void setShouldDrawWalkerPath(boolean shouldDraw);

    @ConfigKey(value = "drawCollision", defaultValue = "false")
    boolean shouldDrawCollision();
    @ConfigKey(value = "drawCollision")
    void setShouldDrawCollision(boolean shouldDraw);

    @ConfigKey(value = "drawInteractable", defaultValue = "false")
    boolean shouldDrawInteractable();
    @ConfigKey(value = "drawInteractable")
    void setShouldDrawInteractable(boolean shouldDraw);

    @ConfigKey(value = "logNames", defaultValue = "true")
    boolean shouldLogNames();
    @ConfigKey(value = "logNames")
    void setShouldLogNames(boolean shouldDraw);

    @ConfigKey(value = "neverLog", defaultValue = "true")
    boolean shouldNeverLog();
    @ConfigKey(value = "neverLog")
    void setNeverLog(boolean neverLog);
    @ConfigKey(value = "mouseMovements", defaultValue = "false")
    boolean shouldSpoofMouseMovemnt();
    @ConfigKey(value = "mouseMovements")
    void setSpoofMouseMovement(boolean spoof);

    @ConfigKey(value = "warning", defaultValue = "false")
    boolean getHasAcceptedWarning();
    @ConfigKey(value = "warning")
    void setHasAcceptedWarning(boolean accepted);

    @ConfigKey(value = "visualizeMovements", defaultValue = "false")
    boolean shouldVisualizeMovements();
    @ConfigKey(value = "visualizeMovements")
    void setVisualizeMovements(boolean visualize);

    @ConfigKey(value = "visualizeClicks", defaultValue = "false")
    boolean shouldVisualizeClicks();
    @ConfigKey(value = "visualizeClicks")
    void setVisualizeClicks(boolean visualize);

    @ConfigKey(value = "logHistoryLimit", defaultValue = "50")
    int getLogHistoryLimit();
    @ConfigKey(value = "logHistoryLimit")
    void setLogHistoryLimit(int limit);

    @ConfigKey(value = "drawStratPath", defaultValue = "false")
    boolean getDrawStratPath();
    @ConfigKey(value = "drawStratPath")
    void setDrawStratPath(boolean draw);
}
