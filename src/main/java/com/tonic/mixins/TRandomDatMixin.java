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

import java.util.Arrays;
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

    @Insert(method = "randomDatData2", at = @At(value = AtTarget.RETURN, shift = Shift.HEAD), all = true)
    public static void onWriteNewRandomDatData(byte[] buffer, int var1) //writeRandomDat
    {
        if (!Static.getVitaConfig().shouldCacheRandomDat())
        {
            return;
        }

        String username = ReflectBuilder.of(client)
                .method("getUsername", null, null)
                .get();

        String identifier = username != null && !username.isEmpty() ? username : characterId;

        byte[] newRandomDatData = Arrays.copyOf(buffer, 24);
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
