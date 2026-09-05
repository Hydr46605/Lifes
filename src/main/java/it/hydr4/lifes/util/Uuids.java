package it.hydr4.lifes.util;

import java.util.UUID;

/** Strict UUID parsing that never throws on the hot path. */
public final class Uuids {
    private Uuids() {
    }

    public static UUID parseOrNull(String raw) {
        if (raw == null || raw.length() != 36) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
