package com.tonic.mixins;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TBuffer;
import com.tonic.api.TClient;
import com.tonic.injector.annotations.*;
import com.tonic.model.RandomDat;
import com.tonic.util.ReflectBuilder;
import com.tonic.util.dto.JField;
import net.runelite.api.Client;

@Mixin("Client")
public abstract class TRandomDatMixin
{
    @Shadow("characterId")
    public static String characterId;

    @Insert(method = "writeRandomDat", at = @At(value = AtTarget.RETURN, shift = Shift.HEAD), all = true)
    public static void onWriteNewRandomDatData(byte[] var0, int var1, byte[] newRandomDatData, int var3, int var4)
    {
        if (!Static.getVitaConfig().shouldCacheRandomDat())
        {
            return;
        }

        String username = ReflectBuilder.of(Static.getClient())
                .method("getUsername", null, null)
                .get();

        String identifier = username != null && !username.isEmpty() ? username : characterId;

        RandomDat.writeCachedRandomDatData(identifier, newRandomDatData);
        Logger.info("Storing cached random.dat data for user " + identifier);
    }

    @Disable("randomDatData2")
    public static boolean randomDatData2(TBuffer buffer)
    {
        if (!Static.getVitaConfig().shouldCacheRandomDat())
        {
            return true;
        }

        String username = ReflectBuilder.of(Static.getClient())
                .method("getUsername", null, null)
                .get();

        String identifier = username != null && !username.isEmpty() ? username : characterId;

        byte[] cachedData = RandomDat.getCachedRandomDatData(identifier);
        if (cachedData == null)
        {
            for (byte i = 0; i < 24; i++)
            {
                buffer.writeByte(-1);
            }
        }
        else
        {
            for(byte b : cachedData) {
                buffer.writeByte(b);
            }
        }
        Logger.info("Using cached random.dat data for user " + identifier);
        return false;
    }
}
