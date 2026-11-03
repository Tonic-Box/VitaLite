package com.tonic.plugins.profiles.jagex;

import lombok.Getter;

/**
 * A browser sign-in that did not return an id token, with the reason.
 */
@Getter
public class JagexLoginException extends Exception {
    public enum Kind {
        AUTHORIZATION_DENIED("Sign-in was denied or cancelled in the browser."),
        NO_ID_TOKEN("The browser callback didn't include a login token."),
        STATE_MISMATCH("The browser returned a different sign-in attempt, try again."),
        LOOPBACK_TIMEOUT("Timed out waiting for the browser sign-in."),
        PORT_IN_USE("Port 80 is in use, close the program holding it and try again."),
        CANCELLED("Sign-in cancelled."),
        WRONG_ACCOUNT("That sign-in is a different Jagex account than this card's, sign out in the browser and try again.");

        private final String message;

        Kind(String message) {
            this.message = message;
        }
    }

    private final Kind kind;

    public JagexLoginException(Kind kind) {
        super(kind.message);
        this.kind = kind;
    }
}
