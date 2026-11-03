package com.tonic.plugins.profiles.jagex;

import com.tonic.plugins.profiles.data.JagexAccount;
import com.tonic.plugins.profiles.data.JagexAccountCharacter;
import com.tonic.plugins.profiles.jagex.model.JagCharacter;
import com.tonic.plugins.profiles.session.ProfilesSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Jagex account operations that talk to the auth service: add an account from a sign-in or a session
 * id, list its characters again, and replace an expired session. Calls block; a UI runs them off its event
 * thread.
 */
public class JagexAccounts {
    private final ProfilesSession store;
    private final JagexAuthClient auth;

    public JagexAccounts(ProfilesSession store, JagexAuthClient auth) {
        this.store = store;
        this.auth = auth;
    }

    /**
     * Creates a game session from a sign-in's id token and stores it with its characters under the label.
     *
     * @param label the account's label
     * @param idToken the id token the browser sign-in returned
     * @return the stored account
     * @throws IllegalArgumentException when the label is blank, contains " / " or is taken
     * @throws IllegalStateException when profiles.db could not be read
     * @throws JagexAuthException when the auth service refuses or cannot be reached
     * @throws IOException when saving fails
     */
    public JagexAccount add(String label, String idToken) throws IOException, JagexAuthException {
        store.checkNewName(label);
        return importSession(label, auth.createSession(idToken));
    }

    /**
     * Stores an existing game session with its characters under the label.
     *
     * @param label the account's label
     * @param sessionId the game session id
     * @return the stored account
     * @throws IllegalArgumentException when the label is blank, contains " / " or is taken
     * @throws IllegalStateException when profiles.db could not be read
     * @throws JagexAuthException when the auth service refuses the session or cannot be reached
     * @throws IOException when saving fails
     */
    public JagexAccount importSession(String label, String sessionId) throws IOException, JagexAuthException {
        store.checkNewName(label);
        List<JagCharacter> listed = auth.listCharacters(sessionId);
        JagexAccount account = new JagexAccount(label, sessionId, new ArrayList<>(), "");
        JagexAccount stored = merged(account, sessionId, listed);
        store.addJagex(stored);
        return stored;
    }

    /**
     * Lists the account's characters again with its stored session. Characters still listed keep their PIN,
     * characters no longer listed are dropped, and the selection is kept while its character is listed.
     *
     * @param label the account's label
     * @return the stored account
     * @throws IllegalArgumentException when no account with that label is stored
     * @throws IllegalStateException when profiles.db could not be read
     * @throws JagexAuthException when the session has expired (see isSessionExpired), or the auth service
     *                            refuses or cannot be reached
     * @throws IOException when saving fails
     */
    public JagexAccount refresh(String label) throws IOException, JagexAuthException {
        List<JagCharacter> listed = auth.listCharacters(require(label).getSessionId());
        JagexAccount current = require(label);
        JagexAccount stored = merged(current, current.getSessionId(), listed);
        store.updateJagex(label, stored);
        return stored;
    }

    /**
     * Replaces the account's session with one from a new sign-in, keeping its label and PINs. A sign-in
     * that lists none of the account's characters is refused, so a sign-in to another Jagex account can
     * never take over the card.
     *
     * @param label the account's label
     * @param idToken the id token the browser sign-in returned
     * @return the stored account
     * @throws IllegalArgumentException when no account with that label is stored
     * @throws IllegalStateException when profiles.db could not be read
     * @throws JagexLoginException when the sign-in is a different Jagex account
     * @throws JagexAuthException when the auth service refuses or cannot be reached
     * @throws IOException when saving fails
     */
    public JagexAccount signInAgain(String label, String idToken) throws IOException, JagexAuthException, JagexLoginException {
        require(label);
        String sessionId = auth.createSession(idToken);
        List<JagCharacter> listed = auth.listCharacters(sessionId);
        JagexAccount current = require(label);
        if (!current.getCharacters().isEmpty() && !sharesCharacter(current, listed)) {
            throw new JagexLoginException(JagexLoginException.Kind.WRONG_ACCOUNT);
        }
        JagexAccount stored = merged(current, sessionId, listed);
        store.updateJagex(label, stored);
        return stored;
    }

    private JagexAccount require(String label) {
        JagexAccount account = store.getJagexAccount(label);
        if (account == null) {
            throw new IllegalArgumentException("The account \"" + label + "\" is no longer stored");
        }
        return account;
    }

    private static boolean sharesCharacter(JagexAccount account, List<JagCharacter> listed) {
        for (JagCharacter character : listed) {
            if (account.getCharacter(character.getAccountId()) != null) {
                return true;
            }
        }
        return false;
    }

    private static JagexAccount merged(JagexAccount account, String sessionId, List<JagCharacter> listed) {
        List<JagexAccountCharacter> characters = new ArrayList<>();
        for (JagCharacter character : listed) {
            JagexAccountCharacter known = account.getCharacter(character.getAccountId());
            String name = character.getDisplayName() == null || character.getDisplayName().isEmpty() ? null : character.getDisplayName();
            characters.add(new JagexAccountCharacter(character.getAccountId(), name, known == null ? "" : known.getBankPin()));
        }
        JagexAccount updated = new JagexAccount(account.getLabel(), sessionId, characters, account.getSelectedCharacterId());
        if (updated.getCharacter(updated.getSelectedCharacterId()) == null) {
            updated.setSelectedCharacterId(characters.isEmpty() ? "" : characters.get(0).getAccountId());
        }
        return updated;
    }
}
