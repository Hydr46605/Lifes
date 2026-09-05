package it.hydr4.lifes.util;

/** Argument validation with uniform failure messages. */
public final class Checks {
    private Checks() {
    }

    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    public static String notBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static int atLeast(int value, int minimum, String name) {
        if (value < minimum) {
            throw new IllegalArgumentException(name + " must be >= " + minimum + ": " + value);
        }
        return value;
    }

    public static int inRange(int value, int minimum, int maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                name + " must be between " + minimum + " and " + maximum + ": " + value
            );
        }
        return value;
    }
}
