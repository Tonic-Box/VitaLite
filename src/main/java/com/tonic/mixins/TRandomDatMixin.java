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

    /**
     * Keeps the random.dat bytes the server sent for the account being played instead of writing them to the shared file. The client copies them into randomDat in the same method, so the copy is made here before it is skipped.
     *
     * @param buffer the buffer holding the bytes
     * @param offset where they start
     * @return whether the client's write is skipped
     */
    @Disable("writeRandomDat")
    public static boolean onWriteRandomDat(TBuffer buffer, int offset)
    {
        if (!Static.getVitaConfig().shouldCacheRandomDat())
        {
            return false;
        }

        randomDat = Arrays.copyOfRange(buffer.getArray(), offset, offset + 24);
        String username = ReflectBuilder.of(client)
                .method("getUsername", null, null)
                .get();

        String identifier = username != null && !username.isEmpty() ? username : ((TClient) client).getCharacterId();
        if (identifier == null || identifier.isEmpty())
        {
            return true;
        }

        RandomDat.writeCachedRandomDatData(identifier, randomDat);
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
