package com.entitykart.monolith.validation;

import java.util.regex.Pattern;

public class PasswordValidator {

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?])[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?]{8,128}$";
    private static final Pattern PATTERN = Pattern.compile(PASSWORD_PATTERN);

    public static boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        return PATTERN.matcher(password).matches();
    }

    public static void validate(String password) {
        if (!isValid(password)) {
            throw new IllegalArgumentException("Password must be between 8 and 128 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
        }
    }
}
