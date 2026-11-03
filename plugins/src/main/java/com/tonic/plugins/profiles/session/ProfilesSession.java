package com.tonic.plugins.profiles.session;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.tonic.Static;
import com.tonic.model.DeviceID;
import com.tonic.plugins.profiles.data.JagexAccount;
import com.tonic.plugins.profiles.data.JagexAccountCharacter;
import com.tonic.plugins.profiles.data.Profile;
import com.tonic.plugins.profiles.util.GsonUtil;
import com.tonic.plugins.profiles.util.OS;
import com.tonic.services.LoginService;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The encrypted profile store: legacy (username and password) profiles and Jagex accounts, kept in
 * profiles.db. Every change is saved at once; a file that cannot be read is never overwritten.
 */
public class ProfilesSession
{
    private static final String FILE_NAME = "profiles.db";
    private static final int VERSION = 2;
    private static final String NAME_SEPARATOR = " / ";
    private static final Type LEGACY_LIST = new TypeToken<List<Profile>>(){}.getType();
    private static final Type JAGEX_LIST = new TypeToken<List<JagexAccount>>(){}.getType();

    private static ProfilesSession INSTANCE;

    public static synchronized ProfilesSession getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new ProfilesSession(Static.VITA_DIR.resolve(FILE_NAME), generateAESKey());
        }

        return INSTANCE;
    }

    private final Path file;
    private final Path backup;
    private final String key;
    private final List<Profile> legacy = new ArrayList<>();
    private final List<JagexAccount> jagex = new ArrayList<>();
    private String loadError;
    private boolean migrated;

    private ProfilesSession(Path file, String key)
    {
        this.file = file;
        this.backup = file.resolveSibling(file.getFileName() + ".bak");
        this.key = key;
    }

    /**
     * @return whether profiles.db could not be read, in which case nothing is saved
     */
    public synchronized boolean isUnreadable()
    {
        return loadError != null;
    }

    /**
     * @return why profiles.db could not be read, or null when it was read
     */
    public synchronized String getLoadError()
    {
        return loadError;
    }

    /**
     * @return copies of the legacy profiles
     */
    public synchronized List<Profile> getLegacyProfiles()
    {
        List<Profile> copies = new ArrayList<>();
        for (Profile profile : legacy)
        {
            copies.add(copy(profile));
        }
        return copies;
    }

    /**
     * @return copies of the Jagex accounts
     */
    public synchronized List<JagexAccount> getJagexAccounts()
    {
        List<JagexAccount> copies = new ArrayList<>();
        for (JagexAccount account : jagex)
        {
            copies.add(account.copy());
        }
        return copies;
    }

    /**
     * @param label the account's label
     * @return a copy of the Jagex account with that label, or null
     */
    public synchronized JagexAccount getJagexAccount(String label)
    {
        int index = jagexIndex(label);
        return index < 0 ? null : jagex.get(index).copy();
    }

    /**
     * The names other plugins pick profiles by: every legacy identifier, then {@code Label / Character} for
     * every character of every Jagex account.
     *
     * @return the profile names
     */
    public synchronized List<String> getProfileNames()
    {
        List<String> names = new ArrayList<>();
        for (Profile profile : legacy)
        {
            names.add(profile.getIdentifier());
        }
        for (JagexAccount account : jagex)
        {
            for (JagexAccountCharacter character : account.getCharacters())
            {
                names.add(character.entryName(account.getLabel()));
            }
        }
        return names;
    }

    /**
     * Reads profiles.db, replacing what is held. A file in the old format (one profile per Jagex character)
     * is grouped into accounts and backed up before its first save in the new format.
     */
    public synchronized void loadProfilesFromFile()
    {
        legacy.clear();
        jagex.clear();
        loadError = null;
        migrated = false;

        String enc;
        try
        {
            if (!Files.exists(file))
            {
                return;
            }
            enc = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
        }
        catch (IOException e)
        {
            loadError = "Couldn't read " + file + ": " + e.getMessage();
            return;
        }

        if (enc.isEmpty())
        {
            return;
        }

        try
        {
            JsonElement root = new JsonParser().parse(decrypt(enc));
            if (root.isJsonArray())
            {
                migrate(root.getAsJsonArray());
                migrated = true;
            }
            else if (root.isJsonObject())
            {
                read(root.getAsJsonObject());
            }
            else
            {
                throw new JsonParseException("profiles.db holds neither a list nor an object");
            }
        }
        catch (RuntimeException e)
        {
            legacy.clear();
            jagex.clear();
            loadError = "Couldn't decrypt or read profiles.db: " + (e.getCause() != null ? e.getCause() : e);
        }
    }

    /**
     * Checks that a new profile or account could be stored under that name.
     *
     * @param name the name
     * @throws IllegalArgumentException when the name is blank, contains " / " or is taken
     * @throws IllegalStateException when profiles.db could not be read
     */
    public synchronized void checkNewName(String name)
    {
        requireReadable();
        requireFreeName(name, null);
    }

    /**
     * Adds a legacy profile.
     *
     * @param profile the profile
     * @throws IllegalArgumentException when its identifier is blank, contains " / " or is taken
     * @throws IllegalStateException when profiles.db could not be read
     * @throws IOException when saving fails; nothing is added then
     */
    public synchronized void addLegacy(Profile profile) throws IOException
    {
        requireReadable();
        requireFreeName(profile.getIdentifier(), null);
        legacy.add(copy(profile));
        try
        {
            save();
        }
        catch (IOException e)
        {
            legacy.remove(legacy.size() - 1);
            throw e;
        }
    }

    /**
     * Replaces the legacy profile with that identifier.
     *
     * @param identifier the stored profile's identifier
     * @param updated the profile to store in its place
     * @throws IllegalArgumentException when no such profile is stored, or the new identifier is blank,
     *                                  contains " / " or is taken
     * @throws IllegalStateException when profiles.db could not be read
     * @throws IOException when saving fails; nothing is changed then
     */
    public synchronized void updateLegacy(String identifier, Profile updated) throws IOException
    {
        requireReadable();
        int index = requireLegacyIndex(identifier);
        requireFreeName(updated.getIdentifier(), identifier);
        Profile previous = legacy.set(index, copy(updated));
        try
        {
            save();
        }
        catch (IOException e)
        {
            legacy.set(index, previous);
            throw e;
        }
    }

    /**
     * Removes the legacy profile with that identifier.
     *
     * @param identifier the profile's identifier
     * @return whether a profile was removed
     * @throws IllegalStateException when profiles.db could not be read
     * @throws IOException when saving fails; nothing is removed then
     */
    public synchronized boolean removeLegacy(String identifier) throws IOException
    {
        requireReadable();
        int index = legacyIndex(identifier);
        if (index < 0)
        {
            return false;
        }
        Profile removed = legacy.remove(index);
        try
        {
            save();
        }
        catch (IOException e)
        {
            legacy.add(index, removed);
            throw e;
        }
        return true;
    }

    /**
     * Adds a Jagex account.
     *
     * @param account the account
     * @throws IllegalArgumentException when its label is blank, contains " / " or is taken
     * @throws IllegalStateException when profiles.db could not be read
     * @throws IOException when saving fails; nothing is added then
     */
    public synchronized void addJagex(JagexAccount account) throws IOException
    {
        requireReadable();
        requireFreeName(account.getLabel(), null);
        jagex.add(account.copy());
        try
        {
            save();
        }
        catch (IOException e)
        {
            jagex.remove(jagex.size() - 1);
            throw e;
        }
    }

    /**
     * Replaces the Jagex account with that label.
     *
     * @param label the stored account's label
     * @param updated the account to store in its place
     * @throws IllegalArgumentException when no such account is stored, or the new label is blank, contains
     *                                  " / " or is taken
     * @throws IllegalStateException when profiles.db could not be read
     * @throws IOException when saving fails; nothing is changed then
     */
    public synchronized void updateJagex(String label, JagexAccount updated) throws IOException
    {
        requireReadable();
        int index = requireJagexIndex(label);
        requireFreeName(updated.getLabel(), label);
        JagexAccount previous = jagex.set(index, updated.copy());
        try
        {
            save();
        }
        catch (IOException e)
        {
            jagex.set(index, previous);
            throw e;
        }
    }

    /**
     * Removes the Jagex account with that label.
     *
     * @param label the account's label
     * @return whether an account was removed
     * @throws IllegalStateException when profiles.db could not be read
     * @throws IOException when saving fails; nothing is removed then
     */
    public synchronized boolean removeJagex(String label) throws IOException
    {
        requireReadable();
        int index = jagexIndex(label);
        if (index < 0)
        {
            return false;
        }
        JagexAccount removed = jagex.remove(index);
        try
        {
            save();
        }
        catch (IOException e)
        {
            jagex.add(index, removed);
            throw e;
        }
        return true;
    }

    /**
     * Fills the login screen with the profile of that name, as getProfileNames lists it, and optionally
     * logs in. Blocks until the client thread has run it.
     *
     * @param name the profile name
     * @param login whether to log in after filling the login screen
     * @return whether a profile has that name
     */
    public boolean loginByName(String name, boolean login)
    {
        Profile profile = null;
        JagexAccount account = null;
        JagexAccountCharacter character = null;
        synchronized (this)
        {
            for (Profile candidate : legacy)
            {
                if (candidate.getIdentifier().equals(name))
                {
                    profile = copy(candidate);
                    break;
                }
            }
            for (int i = 0; profile == null && character == null && i < jagex.size(); i++)
            {
                JagexAccount candidate = jagex.get(i);
                for (JagexAccountCharacter entry : candidate.getCharacters())
                {
                    if (entry.entryName(candidate.getLabel()).equals(name))
                    {
                        account = candidate.copy();
                        character = entry.copy();
                        break;
                    }
                }
            }
        }

        if (profile != null)
        {
            login(profile, login);
            return true;
        }
        if (character != null)
        {
            login(account, character, login);
            return true;
        }
        return false;
    }

    /**
     * Fills the login screen with a legacy profile and optionally logs in. Blocks until the client thread
     * has run it.
     *
     * @param profile the legacy profile
     * @param login whether to log in after filling the login screen
     * @return whether the client was on the login screen
     */
    public boolean login(Profile profile, boolean login)
    {
        return LoginService.login(profile.getUsername(), profile.getPassword(), login);
    }

    /**
     * Fills the login screen with a Jagex account's character and optionally logs in. Blocks until the
     * client thread has run it.
     *
     * @param account the account
     * @param character the character to play
     * @param login whether to log in after filling the login screen
     * @return whether the client was on the login screen
     */
    public boolean login(JagexAccount account, JagexAccountCharacter character, boolean login)
    {
        String displayName = character.isNamed() ? character.getDisplayName() : "";
        return LoginService.login(account.getSessionId(), character.getAccountId(), displayName, login);
    }

    private void read(JsonObject root)
    {
        int version = root.has("version") ? root.get("version").getAsInt() : -1;
        if (version != VERSION)
        {
            throw new JsonParseException("unknown profiles.db version " + version);
        }

        List<Profile> legacyProfiles = GsonUtil.GSON.fromJson(root.get("legacy"), LEGACY_LIST);
        if (legacyProfiles != null)
        {
            for (Profile profile : legacyProfiles)
            {
                if (profile != null)
                {
                    legacy.add(normalized(profile));
                }
            }
        }

        List<JagexAccount> accounts = GsonUtil.GSON.fromJson(root.get("jagex"), JAGEX_LIST);
        if (accounts != null)
        {
            for (JagexAccount account : accounts)
            {
                if (account != null)
                {
                    jagex.add(normalized(account));
                }
            }
        }
    }

    private void migrate(JsonArray root)
    {
        List<Profile> profiles = GsonUtil.GSON.fromJson(root, LEGACY_LIST);
        Map<String, List<Profile>> bySession = new LinkedHashMap<>();
        if (profiles != null)
        {
            for (Profile profile : profiles)
            {
                if (profile == null)
                {
                    continue;
                }
                Profile normalized = normalized(profile);
                if (normalized.isJagexAccount())
                {
                    bySession.computeIfAbsent(normalized.getSessionId(), session -> new ArrayList<>()).add(normalized);
                }
                else
                {
                    legacy.add(normalized);
                }
            }
        }

        for (Map.Entry<String, List<Profile>> group : bySession.entrySet())
        {
            List<Profile> members = group.getValue();
            members.sort((a, b) -> a.getIdentifier().compareToIgnoreCase(b.getIdentifier()));

            JagexAccount account = new JagexAccount();
            account.setLabel(members.get(0).getIdentifier());
            account.setSessionId(group.getKey());
            for (Profile member : members)
            {
                if (account.getCharacter(member.getCharacterId()) == null)
                {
                    String name = member.getCharacterName().isEmpty() ? null : member.getCharacterName();
                    account.getCharacters().add(new JagexAccountCharacter(member.getCharacterId(), name, member.getBankPin()));
                }
            }
            account.setSelectedCharacterId(account.getCharacters().get(0).getAccountId());
            jagex.add(account);
        }
    }

    private void save() throws IOException
    {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        root.add("legacy", GsonUtil.GSON.toJsonTree(legacy, LEGACY_LIST));
        root.add("jagex", GsonUtil.GSON.toJsonTree(jagex, JAGEX_LIST));
        String enc = encrypt(GsonUtil.GSON.toJson(root));

        Files.createDirectories(file.getParent());
        if (migrated && Files.exists(file) && !Files.exists(backup))
        {
            Files.copy(file, backup);
        }

        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(temp, enc.getBytes(StandardCharsets.UTF_8));
        try
        {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        migrated = false;
    }

    private void requireReadable()
    {
        if (loadError != null)
        {
            throw new IllegalStateException("profiles.db could not be read, so it is not changed: " + loadError);
        }
    }

    private void requireFreeName(String name, String except)
    {
        if (name == null || name.trim().isEmpty())
        {
            throw new IllegalArgumentException("Enter a name");
        }
        if (name.equals(except))
        {
            return;
        }
        if (name.contains(NAME_SEPARATOR))
        {
            throw new IllegalArgumentException("A name cannot contain \"" + NAME_SEPARATOR + "\"");
        }
        if (legacyIndex(name) >= 0 || jagexIndex(name) >= 0)
        {
            throw new IllegalArgumentException("A profile named \"" + name + "\" already exists");
        }
    }

    private int requireLegacyIndex(String identifier)
    {
        int index = legacyIndex(identifier);
        if (index < 0)
        {
            throw new IllegalArgumentException("No profile named \"" + identifier + "\" is stored");
        }
        return index;
    }

    private int requireJagexIndex(String label)
    {
        int index = jagexIndex(label);
        if (index < 0)
        {
            throw new IllegalArgumentException("No Jagex account named \"" + label + "\" is stored");
        }
        return index;
    }

    private int legacyIndex(String identifier)
    {
        for (int i = 0; i < legacy.size(); i++)
        {
            if (legacy.get(i).getIdentifier().equals(identifier))
            {
                return i;
            }
        }
        return -1;
    }

    private int jagexIndex(String label)
    {
        for (int i = 0; i < jagex.size(); i++)
        {
            if (jagex.get(i).getLabel().equals(label))
            {
                return i;
            }
        }
        return -1;
    }

    private static Profile copy(Profile profile)
    {
        return new Profile(profile.getIdentifier(), profile.isJagexAccount(), profile.getUsername(), profile.getPassword(),
                profile.getCharacterName(), profile.getSessionId(), profile.getCharacterId(), profile.getBankPin());
    }

    private static Profile normalized(Profile profile)
    {
        return new Profile(orEmpty(profile.getIdentifier()), profile.isJagexAccount(), orEmpty(profile.getUsername()),
                orEmpty(profile.getPassword()), orEmpty(profile.getCharacterName()), orEmpty(profile.getSessionId()),
                orEmpty(profile.getCharacterId()), orEmpty(profile.getBankPin()));
    }

    private static JagexAccount normalized(JagexAccount account)
    {
        JagexAccount normalized = new JagexAccount();
        normalized.setLabel(orEmpty(account.getLabel()));
        normalized.setSessionId(orEmpty(account.getSessionId()));
        normalized.setSelectedCharacterId(orEmpty(account.getSelectedCharacterId()));
        if (account.getCharacters() != null)
        {
            for (JagexAccountCharacter character : account.getCharacters())
            {
                if (character != null && character.getAccountId() != null)
                {
                    String name = character.isNamed() ? character.getDisplayName() : null;
                    normalized.getCharacters().add(new JagexAccountCharacter(character.getAccountId(), name, orEmpty(character.getBankPin())));
                }
            }
        }
        return normalized;
    }

    private static String orEmpty(String text)
    {
        return text == null ? "" : text;
    }

    private String encrypt(String plaintext)
    {
        try
        {
            byte[] keyBytes = Base64.getDecoder().decode(key);
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

    private String decrypt(String base64IvAndCiphertext)
    {
        try
        {
            byte[] combined = Base64.getDecoder().decode(base64IvAndCiphertext);
            byte[] keyBytes = Base64.getDecoder().decode(key);
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
