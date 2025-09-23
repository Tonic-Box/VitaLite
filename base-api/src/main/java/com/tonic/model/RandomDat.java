package com.tonic.model;

import com.tonic.services.ConfigManager;
import java.util.Base64;

public class RandomDat {
    private static boolean cachedRandomDat = true;
    private static final ConfigManager cachedDataProperties = new ConfigManager("CachedRandomDat");

    public static boolean useCachedRandomDat()
    {
        return cachedRandomDat;
    }

    public static void setUseCachedRandomDat(boolean use)
    {
        cachedRandomDat = use;
    }

    public static synchronized byte[] getCachedRandomDatData(String username)
    {
        byte[] data = null;
        String property = cachedDataProperties.getString(username);
        if (property != null)
        {
            data = Base64.getDecoder().decode(property);
        }
        return data;
    }

    public static void writeCachedRandomDatData(String username, byte[] data)
    {
        cachedDataProperties.setProperty(username, Base64.getEncoder().encodeToString(data));
    }
}
