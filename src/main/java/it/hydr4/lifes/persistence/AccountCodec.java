package it.hydr4.lifes.persistence;

import it.hydr4.lifes.api.LivesAccount;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Converts one account between its snapshot and its YAML mapping. */
final class AccountCodec {
    private AccountCodec() {
    }

    /** Decodes one user mapping; returns null for entries that are not salvageable. */
    static LivesAccount decode(UUID id, Map<?, ?> fields) {
        var name = fields.get("name");
        var lives = fields.get("lives");
        var deaths = fields.get("total-deaths");
        var lastDeath = fields.get("last-death");
        if (!(name instanceof String accountName) || accountName.isBlank()) {
            return null;
        }
        if (!(lives instanceof Integer livesValue) || livesValue < 0) {
            return null;
        }
        if (!(deaths instanceof Integer deathsValue) || deathsValue < 0) {
            return null;
        }
        var lastDeathAt = parseInstant(lastDeath);
        var exhaustedField = fields.get("exhausted");
        var exhausted = exhaustedField instanceof Boolean flag ? flag : livesValue == 0;
        return new LivesAccount(id, accountName, livesValue, deathsValue, lastDeathAt, exhausted);
    }

    static Map<String, Object> encode(LivesAccount account) {
        var fields = new LinkedHashMap<String, Object>();
        fields.put("name", account.name());
        fields.put("lives", account.lives());
        fields.put("total-deaths", account.totalDeaths());
        fields.put("last-death", account.lastDeathAt() == null ? null : account.lastDeathAt().toString());
        fields.put("exhausted", account.exhausted());
        return fields;
    }

    private static Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        try {
            return Instant.parse(value.toString());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
