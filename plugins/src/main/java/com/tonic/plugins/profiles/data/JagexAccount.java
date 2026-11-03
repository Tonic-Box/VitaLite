package com.tonic.plugins.profiles.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A Jagex account as one profile: the label it is listed under, the game session id the auth service
 * issued at sign-in, the characters that session lists, and the character last selected on its card.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JagexAccount {
    private String label = "";
    private String sessionId = "";
    private List<JagexAccountCharacter> characters = new ArrayList<>();
    private String selectedCharacterId = "";

    /**
     * @param accountId the character's account id
     * @return the character with that account id, or null
     */
    public JagexAccountCharacter getCharacter(String accountId) {
        for (JagexAccountCharacter character : characters) {
            if (character.getAccountId().equals(accountId)) {
                return character;
            }
        }
        return null;
    }

    /**
     * @return the selected character, else the first listed, or null when the account has none
     */
    public JagexAccountCharacter getSelectedCharacter() {
        JagexAccountCharacter selected = getCharacter(selectedCharacterId);
        if (selected != null) {
            return selected;
        }
        return characters.isEmpty() ? null : characters.get(0);
    }

    /**
     * @return a copy of this account with copies of its characters
     */
    public JagexAccount copy() {
        List<JagexAccountCharacter> copies = new ArrayList<>();
        for (JagexAccountCharacter character : characters) {
            copies.add(character.copy());
        }
        return new JagexAccount(label, sessionId, copies, selectedCharacterId);
    }
}
