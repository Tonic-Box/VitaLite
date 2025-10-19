package com.tonic.plugins.profiles.session;

import com.google.gson.reflect.TypeToken;
import com.tonic.Static;
import com.tonic.model.DeviceID;
import com.tonic.plugins.profiles.ProfilesPlugin;
import com.tonic.plugins.profiles.data.AuthHooks;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    @Getter
    private final Set<Profile> profiles;

    @Getter
    private final AuthHooks authHooks;

    private ProfilesSession()
    {
        profiles = new HashSet<>();
        authHooks = GsonUtil.loadJsonResource(ProfilesPlugin.class, "authHooks.json", AuthHooks.class);
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

    private void setLoginIndex(int index)
    {
        Client client = Static.getClient();

        try
        {
            if (this.authHooks.getSetLoginIndexGarbageValue() <= Byte.MAX_VALUE && this.authHooks.getSetLoginIndexGarbageValue() >= Byte.MIN_VALUE)
            {
                Class<?> paramComposition = Class.forName(this.authHooks.getSetLoginIndexClassName(), true,
                        client.getClass().getClassLoader());
                Method updateLoginIndex = paramComposition.getDeclaredMethod(this.authHooks.getSetLoginIndexMethodName(),
                        int.class, byte.class);
                updateLoginIndex.setAccessible(true);
                updateLoginIndex.invoke(null, index, (byte) this.authHooks.getSetLoginIndexGarbageValue());
                updateLoginIndex.setAccessible(false);
            }
            else if (this.authHooks.getSetLoginIndexGarbageValue() <= Short.MAX_VALUE && this.authHooks.getSetLoginIndexGarbageValue() >= Short.MIN_VALUE)
            {
                Class<?> paramComposition = Class.forName(this.authHooks.getSetLoginIndexClassName(), true,
                        client.getClass().getClassLoader());
                Method updateLoginIndex = paramComposition.getDeclaredMethod(this.authHooks.getSetLoginIndexMethodName(),
                        int.class, short.class);
                updateLoginIndex.setAccessible(true);
                updateLoginIndex.invoke(null, index, (short) this.authHooks.getSetLoginIndexGarbageValue());
                updateLoginIndex.setAccessible(false);
            }
            else
            {
                Class<?> paramComposition = Class.forName(this.authHooks.getSetLoginIndexClassName(), true,
                        client.getClass().getClassLoader());
                Method updateLoginIndex = paramComposition.getDeclaredMethod(this.authHooks.getSetLoginIndexMethodName(),
                        int.class, int.class);
                updateLoginIndex.setAccessible(true);
                updateLoginIndex.invoke(null, index, this.authHooks.getSetLoginIndexGarbageValue());
                updateLoginIndex.setAccessible(false);
            }
        }
        catch (Exception ignored)
        {

        }
    }

    public void setLoginWithJagexAccount(Profile profile, boolean login)
    {
        Client client = Static.getClient();
        ClientThread clientThread = Static.getInjector().getInstance(ClientThread.class);

        clientThread.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN)
            {
                return;
            }

            try
            {
                setLoginIndex(10);

                Class<?> jxSessionClass = Class.forName(this.authHooks.getJxSessionClassName(), true, client.getClass().getClassLoader());
                Field jxSessionField = jxSessionClass.getDeclaredField(this.authHooks.getJxSessionFieldName());
                jxSessionField.setAccessible(true);
                jxSessionField.set(null, profile.getSessionId());
                jxSessionField.setAccessible(false);

                Class<?> jxAccountIdClass = Class.forName(this.authHooks.getJxAccountIdClassName(), true, client.getClass().getClassLoader());
                Field jxAccountIdField = jxAccountIdClass.getDeclaredField(this.authHooks.getJxAccountIdFieldName());
                jxAccountIdField.setAccessible(true);
                jxAccountIdField.set(null, profile.getCharacterId());
                jxAccountIdField.setAccessible(false);

                Class<?> jxDisplayNameClass = Class.forName(this.authHooks.getJxDisplayNameClassName(), true, client.getClass().getClassLoader());
                Field jxDisplayNameField = jxDisplayNameClass.getDeclaredField(this.authHooks.getJxDisplayNameFieldName());
                jxDisplayNameField.setAccessible(true);
                jxDisplayNameField.set(null, profile.getCharacterName());
                jxDisplayNameField.setAccessible(false);
            }
            catch (Exception ignored)
            {

            }

            if (login)
            {
                client.setGameState(GameState.LOGGING_IN);
            }
        });

    }

    public void setLoginWithUsernamePassword(Profile profile, boolean login)
    {
        Client client = Static.getClient();
        ClientThread clientThread = Static.getInjector().getInstance(ClientThread.class);

        clientThread.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN)
            {
                return;
            }

            try
            {
                Class<?> jxSessionClass = Class.forName(this.authHooks.getJxSessionClassName(), true, client.getClass().getClassLoader());
                Field jxSessionField = jxSessionClass.getDeclaredField(this.authHooks.getJxSessionFieldName());
                jxSessionField.setAccessible(true);
                jxSessionField.set(null, null);
                jxSessionField.setAccessible(false);

                Class<?> jxAccountIdClass = Class.forName(this.authHooks.getJxAccountIdClassName(), true, client.getClass().getClassLoader());
                Field jxAccountIdField = jxAccountIdClass.getDeclaredField(this.authHooks.getJxAccountIdFieldName());
                jxAccountIdField.setAccessible(true);
                jxAccountIdField.set(null, null);
                jxAccountIdField.setAccessible(false);

                Class<?> jxDisplayNameClass = Class.forName(this.authHooks.getJxDisplayNameClassName(), true, client.getClass().getClassLoader());
                Field jxDisplayNameField = jxDisplayNameClass.getDeclaredField(this.authHooks.getJxDisplayNameFieldName());
                jxDisplayNameField.setAccessible(true);
                jxDisplayNameField.set(null, null);
                jxAccountIdField.setAccessible(false);

                Class<?> jxLegacyAccountValueClass = Class.forName(this.authHooks.getJxLegacyValueClassName(), true, client.getClass().getClassLoader());
                Field jxLegacyAccountValueField = jxLegacyAccountValueClass.getDeclaredField(this.authHooks.getJxLegacyValueFieldName());
                jxLegacyAccountValueField.setAccessible(true);
                Object jxLegacyAccountObject = jxLegacyAccountValueField.get(null);
                jxLegacyAccountValueField.setAccessible(false);

                Class<?> clientClass = client.getClass(); // Class.forName("client", true, client.getClass.getClassLoader());
                Field jxAccountCheckField = clientClass.getDeclaredField(this.authHooks.getJxAccountCheckFieldName());
                jxAccountCheckField.setAccessible(true);
                jxAccountCheckField.set(null, jxLegacyAccountObject);
                jxAccountCheckField.setAccessible(false);
            }
            catch (Exception ignored)
            {

            }

            try
            {
                setLoginIndex(2);
            }
            catch (Exception ignored)
            {

            }

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
