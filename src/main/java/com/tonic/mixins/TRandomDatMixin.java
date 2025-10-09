package com.tonic.mixins;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TBuffer;
import com.tonic.api.TClient;
import com.tonic.injector.annotations.*;
import com.tonic.injector.util.BytecodeBuilder;
import com.tonic.injector.util.MappingProvider;
import com.tonic.model.RandomDat;
import com.tonic.util.ReflectBuilder;
import com.tonic.util.dto.JClass;
import com.tonic.util.dto.JField;
import net.runelite.api.Client;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

@Mixin("Client")
public abstract class TRandomDatMixin
{
    @Shadow("client")
    public static Client client;

    @Shadow("characterId")
    public static String characterId;

    @Shadow("randomDat")
    public static byte[] randomDat;

    @Insert(method = "writeRandomDat", at = @At(value = AtTarget.RETURN, shift = Shift.HEAD), all = true)
    public static void onWriteNewRandomDatData(byte[] var0, int var1, byte[] newRandomDatData, int var3, int var4)
    {
        if (!Static.getVitaConfig().shouldCacheRandomDat())
        {
            return;
        }

        String username = ReflectBuilder.of(client)
                .method("getUsername", null, null)
                .get();

        String identifier = username != null && !username.isEmpty() ? username : characterId;

        RandomDat.writeCachedRandomDatData(identifier, newRandomDatData);
        Logger.info("Storing cached random.dat data for user " + identifier);
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

            String identifier = username != null && !username.isEmpty() ? username : characterId;
            randomDat = RandomDat.getCachedRandomDatData(identifier);
            Logger.info("Using cached random.dat data for user " + identifier);
        }
        catch (Exception ex)
        {
            System.out.println("Issue from caller: " + caller);
            System.exit(0);
        }
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
