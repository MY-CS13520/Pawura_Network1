package com.pawura.util;

import java.util.regex.Pattern;

/**
 * ValidationUtils – utility methods for input validation.
 */
public final class ValidationUtils {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{3,30}$");
    private static final Pattern EMAIL_PATTERN    = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$");

    private ValidationUtils() {}

    /** Throws IllegalArgumentException if value is null or blank. */
    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
    }

    /** Validates username format. */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /** Validates password length. */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /** Validates email format. */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    // Add other validation methods as needed
}