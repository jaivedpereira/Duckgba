package com.google.common.base;

/**
 * Minimal stand-in for the subset of Guava's {@code Preconditions} the
 * embedded coffee-gb core relies on.
 */
public final class Preconditions {

    private Preconditions() {}

    public static void checkArgument(boolean expression) {
        if (!expression) throw new IllegalArgumentException();
    }

    public static void checkArgument(boolean expression, Object errorMessage) {
        if (!expression) throw new IllegalArgumentException(String.valueOf(errorMessage));
    }

    public static void checkArgument(boolean expression, String fmt, Object... args) {
        if (!expression) throw new IllegalArgumentException(String.format(fmt, args));
    }

    public static <T> T checkNotNull(T ref) {
        if (ref == null) throw new NullPointerException();
        return ref;
    }

    public static <T> T checkNotNull(T ref, Object errorMessage) {
        if (ref == null) throw new NullPointerException(String.valueOf(errorMessage));
        return ref;
    }
}
