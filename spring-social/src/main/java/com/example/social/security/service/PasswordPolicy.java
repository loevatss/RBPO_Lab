package com.example.social.security.service;

public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validateOrThrow(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("password is required");
        }
        String password = rawPassword.trim();
        if (password.length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }
        if (!containsSpecialCharacter(password)) {
            throw new IllegalArgumentException("password must contain a special character");
        }
    }

    private static boolean containsSpecialCharacter(String password) {
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                return true;
            }
        }
        return false;
    }
}
