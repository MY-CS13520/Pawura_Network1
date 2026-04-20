package com.pawura.util;

import java.util.regex.Pattern;

/**
 * ValidationUtils – centralised input validation helpers.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    private ValidationUtils() {}

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static boolean isInLatRange(double lat) {
        return lat >= -90 && lat <= 90;
    }

    public static boolean isInLonRange(double lon) {
        return lon >= -180 && lon <= 180;
    }

    /** Throw if value is blank, using the field name in the message. */
    public static void requireNonBlank(String value, String fieldName) {
        if (isBlank(value))
            throw new IllegalArgumentException(fieldName + " must not be blank.");
    }

    /** Returns trimmed string or "" if null. */
    public static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
