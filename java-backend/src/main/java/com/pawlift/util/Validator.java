package com.pawlift.util;

import com.pawlift.exception.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Validator() {
    }

    public static String required(String value, String field, int max) {
        if (value == null || value.isBlank()) throw new BadRequestException(field + " is required");
        return checkLength(value.trim(), field, max);
    }

    /** Returns "" for null. */
    public static String optional(String value, String field, int max) {
        if (value == null) return "";
        return checkLength(value.trim(), field, max);
    }

    public static String email(String value) {
        String e = required(value, "email", 255).toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(e).matches()) throw new BadRequestException("Invalid email address");
        return e;
    }

    public static String password(String value) {
        if (value == null || value.length() < 8)
            throw new BadRequestException("Password must be at least 8 characters");
        if (value.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BadRequestException("Password must be at most 72 bytes");
        return value;
    }

    private static String checkLength(String v, String field, int max) {
        if (v.length() > max) throw new BadRequestException(field + " must be at most " + max + " characters");
        return v;
    }
}
