package com.pawura.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordUtils – utilities for hashing and verifying passwords.
 *
 * NOTE: For real BCrypt usage add the at.favre.lib:bcrypt dependency.
 * This implementation provides a SHA-256 + salt fallback so the project
 * compiles without the BCrypt library on the class-path.
 *
 * The demo seed data uses a placeholder hash that always matches "password123".
 */
public final class PasswordUtils {

    private static final String DEMO_HASH =
        "$2a$10$Nf5oXvjV7R.R9NEoZU8RXO7OqVCB2e7bF0n3vLnBUMTlf/WiC5Ehu";
    private static final String DEMO_PASSWORD = "password123";

    private PasswordUtils() {}

    /**
     * Hash a plain-text password.
     * Uses BCrypt if available, otherwise SHA-256+salt.
     */
    public static String hash(String plainPassword) {
        try {
            Class<?> bcrypt = Class.forName("at.favre.lib.crypto.bcrypt.BCrypt");
            Object hasher   = bcrypt.getMethod("withDefaults").invoke(null);
            return (String) hasher.getClass()
                .getMethod("hashToString", int.class, char[].class)
                .invoke(hasher, 12, plainPassword.toCharArray());
        } catch (Exception ignored) {
            // BCrypt not on classpath – use SHA-256 + salt
            return sha256Hash(plainPassword);
        }
    }

    /**
     * Verify a plain-text password against its stored hash.
     */
    public static boolean verify(String plainPassword, String storedHash) {
        // Support demo seed data
        if (storedHash.equals(DEMO_HASH)) return plainPassword.equals(DEMO_PASSWORD);
        try {
            Class<?> bcrypt   = Class.forName("at.favre.lib.crypto.bcrypt.BCrypt");
            Object   verifier = bcrypt.getMethod("verifyer").invoke(null);
            Object   result   = verifier.getClass()
                .getMethod("verify", char[].class, String.class)
                .invoke(verifier, plainPassword.toCharArray(), storedHash);
            return (boolean) result.getClass().getMethod("verified").invoke(result);
        } catch (Exception ignored) {
            return storedHash.equals(sha256Hash(plainPassword));
        }
    }

    // ── SHA-256 fallback ──────────────────────────────────────────────────────
    private static String sha256Hash(String password) {
        try {
            SecureRandom  rng  = new SecureRandom();
            byte[]        salt = new byte[16];
            rng.nextBytes(salt);
            MessageDigest md   = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes());
            return "SHA256:" + Base64.getEncoder().encodeToString(salt)
                   + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
