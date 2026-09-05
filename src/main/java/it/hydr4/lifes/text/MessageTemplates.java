package it.hydr4.lifes.text;

import java.util.Map;

/** Immutable mapping of every message key to its effective template. */
public record MessageTemplates(Map<MessageKey, String> templates) {
    public MessageTemplates {
        templates = Map.copyOf(templates);
    }

    public String template(MessageKey key) {
        var template = templates.get(key);
        if (template == null) {
            throw new IllegalStateException("Missing template for " + key.key());
        }
        return template;
    }

    /** Applies settings overrides on top of the code defaults. */
    public static MessageTemplates withOverrides(Map<String, String> overrides) {
        var effective = new java.util.EnumMap<MessageKey, String>(MessageKey.class);
        for (var value : MessageKey.values()) {
            effective.put(value, value.defaultTemplate());
        }
        for (var entry : overrides.entrySet()) {
            MessageKey.byKey(entry.getKey()).ifPresent(key -> effective.put(key, entry.getValue()));
        }
        return new MessageTemplates(effective);
    }
}
