package it.hydr4.lifes.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class MessagesTest {
    @Test
    void prefixedMessagesStartWithThePrefix() {
        var messages = new Messages(MessageTemplates.withOverrides(Map.of()));
        var rendered = messages.render(MessageKey.LIVES_SELF, "lives", 3, "maximum", 10);
        assertEquals("[Lifes] You have 3 lives.", plain(rendered));
    }

    @Test
    void defaultsDoNotPresentTheAdminCapAsThePlayerPool() {
        var messages = new Messages(MessageTemplates.withOverrides(Map.of()));
        var rendered = plain(messages.render(MessageKey.LIVES_SELF, "lives", 3, "maximum", 10));
        assertTrue(!rendered.contains("/"), "a bare life count must not read as a fraction: " + rendered);
    }

    @Test
    void maximumPlaceholderStillResolvesWhenAnAdminAsksForIt() {
        var messages = new Messages(MessageTemplates.withOverrides(Map.of(
            "lives-self", "You have {lives}/{maximum}.")));
        var rendered = messages.render(MessageKey.LIVES_SELF, "lives", 3, "maximum", 10);
        assertEquals("[Lifes] You have 3/10.", plain(rendered));
    }

    @Test
    void overridesReplaceDefaults() {
        var messages = new Messages(MessageTemplates.withOverrides(Map.of("prefix", "<gray>></gray> ")));
        var rendered = messages.render(MessageKey.LIVES_SELF, "lives", 2, "maximum", 5);
        assertEquals("> You have 2 lives.", plain(rendered));
    }

    @Test
    void playerNamesCannotInjectTags() {
        var messages = new Messages(MessageTemplates.withOverrides(Map.of(
            "lives-other", "hi {player}")));
        var rendered = messages.render(MessageKey.LIVES_OTHER, "player", "<red>evil", "lives", 1);
        assertEquals("[Lifes] hi <red>evil", plain(rendered));
    }

    @Test
    void unknownKeysFallBackToDefaults() {
        var messages = new Messages(MessageTemplates.withOverrides(Map.of()));
        var rendered = messages.render(MessageKey.LIVES_RELOAD_DONE);
        assertEquals("[Lifes] Configuration reloaded.", plain(rendered));
    }

    private String plain(net.kyori.adventure.text.Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
