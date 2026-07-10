package com.hotelmanager.security;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 14;
    // BCrypt consumes at most 72 UTF-8 bytes; keep the accepted character set
    // ASCII-only through the complexity rule so characters map 1:1 to bytes.
    public static final int MAX_LENGTH = 72;

    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Pattern PRINTABLE_ASCII = Pattern.compile("[\\x21-\\x7E]+");

    private PasswordPolicy() {
    }

    public static void requireValid(String password) {
        if (password == null
                || password.length() < MIN_LENGTH
                || password.length() > MAX_LENGTH
                || WHITESPACE.matcher(password).find()
                || !PRINTABLE_ASCII.matcher(password).matches()
                || !LOWER.matcher(password).find()
                || !UPPER.matcher(password).find()
                || !DIGIT.matcher(password).find()
                || !SYMBOL.matcher(password).find()) {
            throw new IllegalArgumentException(
                    "Password must be 14-72 ASCII characters and include upper and lower case, a number, and a symbol"
            );
        }
    }
}
