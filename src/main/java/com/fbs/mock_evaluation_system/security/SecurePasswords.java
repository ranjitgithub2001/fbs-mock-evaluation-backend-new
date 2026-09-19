package com.fbs.mock_evaluation_system.security;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecurePasswords {

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurePasswords() {
    }

    public static String randomUnusableSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
