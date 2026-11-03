package com.tonic.plugins.profiles.jagex;

import lombok.Getter;

/**
 * The Jagex auth service refused a request, could not be reached, or answered with a Cloudflare challenge.
 */
@Getter
public class JagexAuthException extends Exception {
    private final int httpStatus;
    private final boolean cloudflare;

    private JagexAuthException(int httpStatus, boolean cloudflare, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.cloudflare = cloudflare;
    }

    /**
     * A non-200 reply from the service.
     *
     * @param httpStatus the reply's status
     * @return the exception
     */
    public static JagexAuthException rejected(int httpStatus) {
        return new JagexAuthException(httpStatus, false, "Jagex rejected the request (HTTP " + httpStatus + "): " + reason(httpStatus));
    }

    /**
     * No usable reply: a network failure or a body without the expected field.
     *
     * @param detail what went wrong
     * @return the exception
     */
    public static JagexAuthException unreachable(String detail) {
        return new JagexAuthException(0, false, "No reply from the Jagex auth service: " + detail);
    }

    /**
     * The reply was a Cloudflare bot challenge rather than the service's answer.
     *
     * @param httpStatus the reply's status
     * @return the exception
     */
    public static JagexAuthException cloudflare(int httpStatus) {
        return new JagexAuthException(httpStatus, true, "Cloudflare blocked the request (HTTP " + httpStatus + "), try again later");
    }

    /**
     * @return whether the stored session no longer works and has to be signed in again
     */
    public boolean isSessionExpired() {
        return !cloudflare && (httpStatus == 401 || httpStatus == 403);
    }

    private static String reason(int httpStatus) {
        switch (httpStatus) {
            case 401:
            case 403:
                return "the session has expired, sign in again";
            case 404:
                return "unknown character for this session";
            default:
                return "the auth service is unavailable";
        }
    }
}
