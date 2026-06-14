package com.librarysystem.util;

public final class ValidationUtil {
    private ValidationUtil() {
    }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    public static Integer parseOptionalYear(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        int year = Integer.parseInt(value.trim());
        if (year < 0 || year > 3000) {
            throw new IllegalArgumentException("Publish year is out of range.");
        }
        return year;
    }
}
