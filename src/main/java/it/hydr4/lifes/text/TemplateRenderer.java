package it.hydr4.lifes.text;

import java.util.HashMap;
import java.util.Map;

/** Renders MiniMessage templates with escaped placeholder values. */
public final class TemplateRenderer {
    private TemplateRenderer() {
    }

    /** Renders MiniMessage with placeholders substituted and escaped. */
    public static net.kyori.adventure.text.Component render(String template, Map<String, String> placeholders) {
        var body = template;
        for (var entry : placeholders.entrySet()) {
            body = body.replace("{" + entry.getKey() + "}", escape(entry.getValue()));
        }
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(body);
    }

    /** Escapes MiniMessage tags so placeholder values can never inject markup. */
    private static String escape(String value) {
        return value.replace("<", "\\<");
    }

    /** Substitutes placeholders without MiniMessage parsing (plain text output). */
    public static String renderPlain(String template, Map<String, String> placeholders) {
        var body = template;
        for (var entry : placeholders.entrySet()) {
            body = body.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return body;
    }

    /** Builds a placeholder map from alternating key/value arguments. */
    public static Map<String, String> pairs(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("placeholder pairs must alternate key and value");
        }
        var map = new HashMap<String, String>(keyValues.length / 2);
        for (var index = 0; index < keyValues.length; index += 2) {
            map.put(String.valueOf(keyValues[index]), String.valueOf(keyValues[index + 1]));
        }
        return map;
    }
}
