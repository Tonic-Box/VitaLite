package com.tonic.plugins.profiles.session;

import com.google.gson.reflect.TypeToken;
import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.model.DeviceID;
import com.tonic.plugins.profiles.data.Profile;
import com.tonic.plugins.profiles.util.GsonUtil;
import com.tonic.plugins.profiles.util.OS;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Getter
public class ProfilesSession
{
    private static final String BASE64_KEY = generateAESKey();
    private static final String DIRECTORY = Static.VITA_DIR + "\\profiles.db";

    private static ProfilesSession INSTANCE;

    public static ProfilesSession getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new ProfilesSession();
        }

        return INSTANCE;
    }

    private final Set<Profile> profiles;

    private ProfilesSession()
    {
        profiles = new HashSet<>();
    }

    public Profile getByName(String name)
    {
        return profiles.stream().filter(p -> p.getIdentifier().equals(name)).findFirst().orElse(null);
    }

    public void saveProfilesToFile()
    {
        try {
            Path path = Paths.get(DIRECTORY);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
        }
        try (Writer writer = new FileWriter(DIRECTORY)) {
            String json = GsonUtil.GSON.toJson(profiles);
            String enc = encrypt(json);
            writer.write(enc);
        } catch (IOException e) {
        }
    }

    public void loadProfilesFromFile()
    {
        try
        {
            Path path = Paths.get(DIRECTORY);
            if (!Files.exists(path))
            {
                Files.createFile(path);
            }
        }
        catch (IOException ignored)
        {
        }
        try (Reader reader = new FileReader(DIRECTORY))
        {
            StringBuilder builder = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1)
            {
                builder.append((char) ch);
            }
            String enc = builder.toString();

            if (enc.isEmpty())
            {
                return;
            }

            String json = decrypt(enc);
            Type setType = new TypeToken<Set<Profile>>(){}.getType();
            Set<Profile> loadedProfiles = GsonUtil.GSON.fromJson(json, setType);

            if (loadedProfiles == null)
            {
                profiles.clear();
            }
            else
            {
                profiles.addAll(loadedProfiles);
            }
        }
        catch (IOException ignored)
        {
        }
    }

    public void login(Profile profile, boolean login)
    {
        if (profile.isJagexAccount())
        {
            setLoginWithJagexAccount(profile, login);
        }
        else
        {
            setLoginWithUsernamePassword(profile, login);
        }
    }

    public void setLoginWithJagexAccount(Profile profile, boolean login)
    {
        Client client = Static.getClient();
        TClient tClient = (TClient) client;
        Static.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN)
            {
                return;
            }
            client.setUsername("");
            client.setPassword("");
            tClient.setAccountTypeJagex();
            tClient.setLoginIndex(10);
            tClient.setSessionId(profile.getSessionId());
            tClient.setCharacterId(profile.getCharacterId());
            tClient.setDisplayName(profile.getCharacterName());

            if (login)
            {
                client.setGameState(GameState.LOGGING_IN);
            }
        });

    }

    public void setLoginWithUsernamePassword(Profile profile, boolean login)
    {
        Client client = Static.getClient();
        TClient tClient = (TClient) client;
        Static.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN)
            {
                return;
            }

            tClient.setSessionId(null);
            tClient.setCharacterId(null);
            tClient.setDisplayName(null);

            tClient.setAccountTypeLegacy();

            tClient.setLoginIndex(2);

            client.setUsername(profile.getUsername());
            client.setPassword(profile.getPassword());

            if (login)
            {
                client.setGameState(GameState.LOGGING_IN);
            }
        });
    }

    private static String encrypt(String plaintext)
    {
        try
        {
            byte[] keyBytes = Base64.getDecoder().decode(BASE64_KEY);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Encryption failed", e);
        }
    }


    private static String decrypt(String base64IvAndCiphertext)
    {
        try
        {
            byte[] combined = Base64.getDecoder().decode(base64IvAndCiphertext);
            byte[] keyBytes = Base64.getDecoder().decode(BASE64_KEY);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            int ctLen = combined.length - iv.length;
            byte[] ct = new byte[ctLen];
            System.arraycopy(combined, iv.length, ct, 0, ctLen);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] plainBytes = cipher.doFinal(ct);
            return new String(plainBytes, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private static String generateAESKey()
    {
        try
        {
            String deviceId = DeviceID.vanillaGetDeviceID(OS.detect().getValue());

            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest(deviceId.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        }
        catch (Exception e)
        {
            return Base64.getEncoder().encodeToString(
                    "DEFAULT_KEY_FALLBACK_1234567890".getBytes()
            );
        }
    }
}
