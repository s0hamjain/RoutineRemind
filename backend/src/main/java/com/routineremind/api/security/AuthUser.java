package com.routineremind.api.security;

/**
 * The authenticated caller, resolved from a verified Identity Platform ID token.
 */
public record AuthUser(String uid, String email, String name) {

    public static final String REQUEST_ATTRIBUTE = "routineremind.authUser";
}
