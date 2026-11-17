package com.tonic.mixins;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TBuffer;
import com.tonic.api.TClient;
import com.tonic.injector.annotations.*;
import com.tonic.model.RandomDat;
import com.tonic.util.ReflectBuilder;
import net.runelite.api.Client;

import java.util.Arrays;

@Mixin("Client")
public abstract class TRandomDatMixin
{
    @Shadow("client")
    public static Client client;

    @Shadow("randomDat")
    public static byte[] randomDat;

    @Disable("writeRandomDat")
    public static boolean onWriteRandomDat(TBuffer buffer, int offset)
    {
        if (!Static.getVitaConfig().shouldCacheRandomDat())
        {
            return false;
        }

        String username = ReflectBuilder.of(client)
                .method("getUsername", null, null)
                .get();

        String identifier = username != null && !username.isEmpty() ? username : ((TClient) client).getCharacterId();
        if (identifier == null || identifier.isEmpty())
        {
            return true;
        }

        byte[] data = Arrays.copyOfRange(buffer.getArray(), offset, offset + 24);
        RandomDat.writeCachedRandomDatData(identifier, data);
        Logger.info("Storing cached random.dat data for user " + identifier);
        return true;
    }

    @Inject
    public static void setRandomDat(String caller)
    {
        if (!Static.getVitaConfig().shouldCacheRandomDat())
        {
            return;
        }

        try
        {
            String username = ReflectBuilder.of(client)
                    .method("getUsername", null, null)
                    .get();

            String identifier = username != null && !username.isEmpty() ? username : ((TClient) client).getCharacterId();
            byte[] data = RandomDat.getCachedRandomDatData(identifier);
            if(data != null)
            {
                randomDat = Arrays.copyOf(data, 24);
                Logger.info("Using cached random.dat data for user " + identifier);
            }
            else
            {
                randomDat = new byte[24];
                for(int i = 0; i < 24; i++)
                {
                    randomDat[i] = -1;
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println("Issue from caller: " + caller);
            ex.printStackTrace();
            System.exit(0);
        }
    }
}
