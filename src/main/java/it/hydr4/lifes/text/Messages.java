package it.hydr4.lifes.text;

import it.hydr4.lifes.ConfigException;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.EnumMap;
import java.util.Map;

/** Templates parsed once at load; bad MiniMessage fails here, not mid-game. */
public final class Messages {
    private final Map<MessageKey, String> templates;

    public Messages(MessageTemplates source) {
        var parsed = new EnumMap<MessageKey, String>(MessageKey.class);
        var mini = MiniMessage.miniMessage();
        for (var value : MessageKey.values()) {
            var template = source.template(value);
            try {
                mini.deserialize(template);
            } catch (RuntimeException exception) {
                throw new ConfigException("messages." + value.key(), "invalid MiniMessage: " + exception.getMessage());
            }
            parsed.put(value, template);
        }
        templates = java.util.Collections.unmodifiableMap(parsed);
    }

    /** Renders a message with its prefix when the key is marked prefixed. */
    public net.kyori.adventure.text.Component render(MessageKey key, Object... placeholderPairs) {
        var body = templates.get(key);
        if (key.prefixed()) {
            body = templates.get(MessageKey.PREFIX) + body;
        }
        return TemplateRenderer.render(body, TemplateRenderer.pairs(placeholderPairs));
    }
}
