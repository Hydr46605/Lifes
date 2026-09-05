package it.hydr4.lifes.config;

import java.util.List;
import java.util.Map;

/** One action entry from settings.yml with path-aware option accessors. */
public record DeathActionSpec(String type, Map<String, Object> options, String path) {
    public DeathActionSpec {
        options = Map.copyOf(options);
    }

    public String requiredString(String key) {
        var value = options.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new it.hydr4.lifes.ConfigException(path(key), "expected a non-blank string");
        }
        return text;
    }

    public String optionalString(String key, String fallback) {
        var value = options.get(key);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof String text) || text.isBlank()) {
            throw new it.hydr4.lifes.ConfigException(path(key), "expected a non-blank string");
        }
        return text;
    }

    public double optionalDouble(String key, double fallback, double minimum, double maximum) {
        var value = options.get(key);
        if (value == null) {
            return fallback;
        }
        var number = asDouble(value, key);
        if (number < minimum || number > maximum) {
            throw new it.hydr4.lifes.ConfigException(path(key), "must be between " + minimum + " and " + maximum);
        }
        return number;
    }

    public String optionalChoice(String key, String fallback, List<String> allowed) {
        var value = optionalString(key, fallback);
        if (!allowed.contains(value)) {
            throw new it.hydr4.lifes.ConfigException(path(key), "expected one of " + allowed);
        }
        return value;
    }

    private double asDouble(Object value, String key) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new it.hydr4.lifes.ConfigException(path(key), "expected a number, got " + value.getClass().getSimpleName());
    }

    public String path(String key) {
        return path + "." + key;
    }
}
