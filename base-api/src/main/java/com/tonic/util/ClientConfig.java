package com.tonic.util;

import com.tonic.services.ClickStrategy;
import com.tonic.services.ConfigManager;

public class ClientConfig
{
    private final ConfigManager config = new ConfigManager("VitaLiteOptions");

    public ClickStrategy getClickStrategy()
    {
        String strategyName = config.getStringOrDefault("clickStrategy", "STATIC");
        try
        {
            return ClickStrategy.valueOf(strategyName);
        }
        catch (IllegalArgumentException e)
        {
            return ClickStrategy.STATIC;
        }
    }

    public void setClickStrategy(ClickStrategy strategy)
    {
        config.setProperty("clickStrategy", strategy.name());
    }

    public int getClickPointX()
    {
        return config.getIntOrDefault("clickPointX", -1);
    }

    public void setClickPointX(int x)
    {
        config.setProperty("clickPointX", x);
    }

    public int getClickPointY()
    {
        return config.getIntOrDefault("clickPointY", -1);
    }

    public void setClickPointY(int y)
    {
        config.setProperty("clickPointY", y);
    }

    public boolean shouldCacheRandomDat()
    {
        return config.getBooleanOrDefault("cachedRandomDat", true);
    }

    public void setShouldCacheRandomDat(boolean shouldCache)
    {
        config.setProperty("cachedRandomDat", shouldCache);
    }

    public boolean shouldCacheDeviceId()
    {
        return config.getBooleanOrDefault("cachedDeviceID", true);
    }

    public void setShouldCacheDeviceId(boolean shouldCache)
    {
        config.setProperty("cachedDeviceID", shouldCache);
    }

    public boolean shouldCacheBank()
    {
        return config.getBooleanOrDefault("cachedBank", true);
    }

    public void setShouldCacheBank(boolean shouldCache)
    {
        config.setProperty("cachedBank", shouldCache);
    }

    public boolean shouldDrawWalkerPath()
    {
        return config.getBooleanOrDefault("drawWalkerPath", true);
    }

    public void setShouldDrawWalkerPath(boolean shouldDraw)
    {
        config.setProperty("drawWalkerPath", shouldDraw);
    }
}
