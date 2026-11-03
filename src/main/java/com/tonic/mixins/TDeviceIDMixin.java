package com.tonic.mixins;

import ch.qos.logback.classic.spi.PlatformInfo;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.MethodOverride;
import com.tonic.injector.annotations.Mixin;
import com.tonic.model.DeviceID;
import com.tonic.util.ReflectBuilder;

import java.util.UUID;

@Mixin("PlatformInfo")
public abstract class TDeviceIDMixin
{
//    @MethodOverride("getDeviceId")
//    public static String getDeviceId(PlatformInfo info, int os)
//    {
//        return process(os);
//    }

    @MethodOverride("getDeviceId")
    public String getDeviceId(int os)
    {
        return process(os);
    }

    @Inject
    private static String process(int os)
    {
        if (!Static.getVitaConfig().shouldCacheDeviceId())
        {
            return DeviceID.UNKNOWN_DEVICE_ID;
        }

        String username = ReflectBuilder.of(Static.getClient())
                .method("getUsername", null, null)
                .get();

        String identifier = username != null && !username.isEmpty() ? username : ((TClient) Static.getClient()).getCharacterId();

        String cachedDeviceId = DeviceID.getCachedUUID(identifier);
        if (cachedDeviceId == null)
        {
            cachedDeviceId = UUID.randomUUID().toString();
            DeviceID.writeCachedUUID(identifier, cachedDeviceId);
            Logger.info("Generated new deviceId (UUID): " + cachedDeviceId + " for account: " + identifier);
        }
        Logger.info("Using cached deviceId (UUID): " + cachedDeviceId + " for account: " + identifier);
        return cachedDeviceId;
    }
}
