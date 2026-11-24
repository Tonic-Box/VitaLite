package com.tonic.plugins.breakhandler;

import com.tonic.plugins.profiles.data.ProfileRef;
import com.tonic.plugins.profiles.session.ProfilesSession;
import com.tonic.services.ConfigManager;
import com.tonic.services.LoginService;
import com.tonic.services.breakhandler.BreakHandler;
import com.tonic.services.breakhandler.settings.Property;

/**
 * The account the Break Handler logs back in to: the client's current account, or a profile from the Profiles store saved by its stable key.
 */
public final class BreakAccount
{
    /**
     * What a login attempt came to.
     */
    public enum Result
    {
        /**
         * The login was started.
         */
        STARTED,
        /**
         * The client is not on the login screen.
         */
        NOT_ON_LOGIN_SCREEN,
        /**
         * The current account was chosen but the client holds neither a Jagex character nor a username to log in with.
         */
        NO_ACCOUNT,
        /**
         * The chosen profile is no longer in the Profiles store.
         */
        MISSING_PROFILE
    }

    /**
     * The saved choice that logs back in to whatever account the client was last on.
     */
    public static final String CURRENT = "current";

    private static final String OLD_MODE = "BreakHandler.account.mode";
    private static final String OLD_PROFILE = "BreakHandler.account.profile";
    private static final String[] OLD_KEYS = {OLD_MODE, OLD_PROFILE, "BreakHandler.account.username", "BreakHandler.account.password"};

    private final BreakHandler breakHandler;
    private final ConfigManager config;

    /**
     * @param breakHandler the break handler whose settings hold the choice
     */
    public BreakAccount(BreakHandler breakHandler)
    {
        this.breakHandler = breakHandler;
        this.config = breakHandler.getConfigManager();
    }

    /**
     * Turns the old account settings (profile mode with a profile name, or a username and password) into the saved choice the first time, then deletes them, the plaintext password with them.
     */
    public void migrate()
    {
        if (!config.hasProperty(Property.ACCOUNT_TARGET.key()))
        {
            String target = CURRENT;
            String name = config.getStringOrDefault(OLD_PROFILE, "");
            if (config.getBooleanOrDefault(OLD_MODE, false) && !name.isEmpty())
            {
                ProfileRef ref = profiles().findRefByName(name);
                if (ref != null)
                {
                    target = ref.getKey();
                }
                else
                {
                    breakHandler.log("[%s] The saved profile %s is not in Profiles, logging back in to the current account instead", "Break Handler", name);
                }
            }
            config.setProperty(Property.ACCOUNT_TARGET.key(), target);
        }
        for (String key : OLD_KEYS)
        {
            if (config.hasProperty(key))
            {
                config.removeProperty(key);
            }
        }
    }

    /**
     * @return the saved choice: CURRENT or a profile key
     */
    public String target()
    {
        String target = config.getStringOrDefault(Property.ACCOUNT_TARGET.key(), CURRENT);
        return ProfileRef.isKey(target) ? target : CURRENT;
    }

    /**
     * @param target CURRENT or a profile key
     */
    public void choose(String target)
    {
        config.setProperty(Property.ACCOUNT_TARGET.key(), target);
    }

    /**
     * Chooses the profile with a name, as the -DvProfile launch option gives it.
     *
     * @param name the profile name
     * @return whether the Profiles store has a profile by that name
     */
    public boolean chooseByName(String name)
    {
        ProfileRef ref = profiles().findRefByName(name);
        if (ref == null)
        {
            return false;
        }
        choose(ref.getKey());
        return true;
    }

    /**
     * @return the chosen profile, or null when the current account is chosen or the profile is gone
     */
    public ProfileRef chosenProfile()
    {
        String target = target();
        return CURRENT.equals(target) ? null : profiles().findRef(target);
    }

    /**
     * @return what the choice logs in to, for the log
     */
    public String describe()
    {
        String target = target();
        if (CURRENT.equals(target))
        {
            return "the current account";
        }
        ProfileRef ref = profiles().findRef(target);
        return ref == null ? "a profile no longer in Profiles (" + target + ")" : ref.getName();
    }

    /**
     * Starts logging in to the chosen account. Blocks until the client thread has run it.
     *
     * @return what the attempt came to
     */
    public Result login()
    {
        String target = target();
        if (CURRENT.equals(target))
        {
            switch (LoginService.loginCurrent())
            {
                case STARTED:
                    return Result.STARTED;
                case NO_ACCOUNT:
                    return Result.NO_ACCOUNT;
                default:
                    return Result.NOT_ON_LOGIN_SCREEN;
            }
        }
        ProfilesSession profiles = profiles();
        ProfileRef ref = profiles.findRef(target);
        if (ref == null)
        {
            return Result.MISSING_PROFILE;
        }
        return profiles.login(ref, true) ? Result.STARTED : Result.NOT_ON_LOGIN_SCREEN;
    }

    private static ProfilesSession profiles()
    {
        ProfilesSession profiles = ProfilesSession.getInstance();
        profiles.loadProfilesFromFile();
        return profiles;
    }
}
