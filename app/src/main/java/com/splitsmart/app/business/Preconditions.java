package com.splitsmart.app.business;

/** Validation helpers; failures surface to the user as the exception message. */
public final class Preconditions {
    private Preconditions() {
    }

    /** Throws IllegalArgumentException with {@code message} when {@code condition} is false. */
    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    /** Returns {@code value}, or throws IllegalStateException when it is null. */
    public static <T> T requireState(T value, String message) {
        if (value == null) throw new IllegalStateException(message);
        return value;
    }

    /** Returns {@code value}, or throws IllegalArgumentException when it is null. */
    public static <T> T requireFound(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }

    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
