package com.tonic.data;

public enum LoginResponseType {
    CONNECTION_ERROR,      // Network/connection issues
    SERVER_ISSUE,         // Server-side problems
    CLIENT_OUTDATED,      // Client needs update
    BANNED,               // Account banned
    LOCKED,               // Account locked
    AUTHENTICATION,       // Login credentials/2FA issues
    ACCOUNT_RESTRICTION,  // Account limitations (members, displayname, DOB, etc.)
    WORLD_RESTRICTION,    // World-specific limitations
    RATE_LIMITED,         // Too many attempts/connections
    SESSION_ISSUE,        // Session/login state problems
    SPECIAL_HANDLING,     // Special cases with custom handling
    MISC_ERROR           // General/unexpected errors
}
