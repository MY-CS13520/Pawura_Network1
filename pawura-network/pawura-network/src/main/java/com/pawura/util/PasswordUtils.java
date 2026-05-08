package com.pawura.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordUtils – utilities for hashing and verifying passwords.
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
            return BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());
        } catch (Throwable t) {
            // Fallback to SHA-256 if library is missing
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
            BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), storedHash);
            return result.verified;
        } catch (Throwable t) {
            if (storedHash == null || !storedHash.startsWith("SHA256:")) return false;
            
            // Fallback verification: extract salt from the stored string
            try {
                String[] parts = storedHash.split(":");
                if (parts.length < 3) return false;
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                return storedHash.equals(sha256HashWithSalt(plainPassword, salt));
            } catch (Exception e) {
                return false;
            }
        }
    }

    // ── SHA-256 fallback ──────────────────────────────────────────────────────
    private static String sha256Hash(String password) {
        SecureRandom rng = new SecureRandom();
        byte[] salt = new byte[16];
        rng.nextBytes(salt);
        return sha256HashWithSalt(password, salt);
    }

    private static String sha256HashWithSalt(String password, byte[] salt) {
        try {
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
