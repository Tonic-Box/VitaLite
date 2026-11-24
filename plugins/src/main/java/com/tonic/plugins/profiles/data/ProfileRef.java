package com.tonic.plugins.profiles.data;

import lombok.Getter;

/**
 * A profile in the store as other plugins pick it: a legacy profile by its identifier, or a Jagex character by its account id, which stays the same when the account or character is renamed.
 */
@Getter
public final class ProfileRef
{
    /**
     * What sort of profile a reference points at.
     */
    public enum Kind
    {
        LEGACY,
        JAGEX
    }

    private static final String LEGACY_PREFIX = "legacy:";
    private static final String JAGEX_PREFIX = "jagex:";

    private final Kind kind;
    private final String id;
    private final String name;

    private ProfileRef(Kind kind, String id, String name)
    {
        this.kind = kind;
        this.id = id;
        this.name = name;
    }

    /**
     * @param identifier the legacy profile's identifier
     * @return a reference to that legacy profile
     */
    public static ProfileRef legacy(String identifier)
    {
        return new ProfileRef(Kind.LEGACY, identifier, identifier);
    }

    /**
     * @param accountId the character's account id
     * @param name the character's entry name, {@code Label / Name}
     * @return a reference to that Jagex character
     */
    public static ProfileRef jagex(String accountId, String name)
    {
        return new ProfileRef(Kind.JAGEX, accountId, name);
    }

    /**
     * @return the key a choice of this profile is saved under: {@code legacy:<identifier>} or {@code jagex:<accountId>}
     */
    public String getKey()
    {
        return (kind == Kind.LEGACY ? LEGACY_PREFIX : JAGEX_PREFIX) + id;
    }

    /**
     * @param key a saved key
     * @return whether it is a profile key, as getKey writes them
     */
    public static boolean isKey(String key)
    {
        return key != null && (key.startsWith(LEGACY_PREFIX) || key.startsWith(JAGEX_PREFIX));
    }

    @Override
    public String toString()
    {
        return name;
    }
}
