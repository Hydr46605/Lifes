package it.hydr4.lifes.discord;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import it.hydr4.lifes.ConfigException;
import java.io.StringReader;
import java.util.Map;
import java.util.Objects;

/**
 * A Discord message template: checked as JSON once at load, rendered per event.
 *
 * <p>Substitution runs over the template text rather than over a parsed tree, so the payload that
 * reaches Discord is the one the administrator wrote, down to key order. Values are escaped for a
 * JSON string context before they go in, which is what stops a substituted value from breaking out
 * of the string that holds it.
 */
public final class DiscordPayload {
    /**
     * Strict on purpose. {@code JsonParser} forces lenient parsing, which accepts comments and
     * trailing commas that Discord itself rejects; a default {@code Gson} does not.
     */
    private static final Gson STRICT = new Gson();

    private final String template;

    private DiscordPayload(String template) {
        this.template = template;
    }

    /** Compiles the template, failing with the configuration path when it is not a JSON object. */
    public static DiscordPayload compile(String raw, String path) {
        Objects.requireNonNull(raw, "raw");
        if (raw.isBlank()) {
            throw new ConfigException(path, "expected a JSON object, got empty text");
        }
        JsonElement parsed;
        try (var reader = new StringReader(raw)) {
            parsed = STRICT.fromJson(reader, JsonElement.class);
        } catch (JsonParseException exception) {
            throw new ConfigException(path, "is not valid JSON: " + reasonOf(exception));
        }
        if (parsed == null || parsed.isJsonNull()) {
            throw new ConfigException(path, "expected a JSON object, got null");
        }
        if (!parsed.isJsonObject()) {
            throw new ConfigException(path, "expected a JSON object, found " + shapeOf(parsed));
        }
        return new DiscordPayload(raw);
    }

    /** Substitutes {@code {key}} placeholders; values are escaped for a JSON string context. */
    public String render(Map<String, String> placeholders) {
        var body = template;
        for (var entry : placeholders.entrySet()) {
            body = body.replace("{" + entry.getKey() + "}", escape(entry.getValue()));
        }
        return body;
    }

    /** Escapes so a value can never terminate the string it is substituted into. */
    private static String escape(String value) {
        var out = new StringBuilder(value.length() + 8);
        for (var index = 0; index < value.length(); index++) {
            var current = value.charAt(index);
            switch (current) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (current < 0x20) {
                        out.append(String.format("\\u%04x", (int) current));
                    } else {
                        out.append(current);
                    }
                }
            }
        }
        return out.toString();
    }

    private static String shapeOf(JsonElement element) {
        if (element.isJsonArray()) {
            return "a JSON array";
        }
        if (element.isJsonPrimitive()) {
            return "a JSON scalar";
        }
        return "no JSON object";
    }

    private static String reasonOf(Throwable exception) {
        var message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
