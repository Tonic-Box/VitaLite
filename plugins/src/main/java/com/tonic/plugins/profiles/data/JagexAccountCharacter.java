package com.tonic.plugins.profiles.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One character under a Jagex account: the account id it logs in with, its display name (null until the
 * character is named) and its bank PIN (empty when none).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JagexAccountCharacter {
    /** What a character without a display name is shown as. */
    public static final String UNNAMED = "{No Name}";

    private String accountId = "";
    private String displayName;
    private String bankPin = "";

    /**
     * @return whether the character has a display name yet
     */
    public boolean isNamed() {
        return displayName != null && !displayName.isEmpty();
    }

    /**
     * @return the display name, or {@code {No Name}} when there is none
     */
    public String shownName() {
        return isNamed() ? displayName : UNNAMED;
    }

    /**
     * The name other plugins pick this character by: {@code Label / Name}, or
     * {@code Label / {No Name} (accountId)} for an unnamed character so each stays unique.
     *
     * @param label the label of the account the character belongs to
     * @return the entry name
     */
    public String entryName(String label) {
        return label + " / " + (isNamed() ? displayName : UNNAMED + " (" + accountId + ")");
    }

    /**
     * @return a copy of this character
     */
    public JagexAccountCharacter copy() {
        return new JagexAccountCharacter(accountId, displayName, bankPin);
    }
}
