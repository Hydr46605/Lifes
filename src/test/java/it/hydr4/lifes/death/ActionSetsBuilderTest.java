package it.hydr4.lifes.death;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.death.actions.DiscordAction;
import it.hydr4.lifes.death.actions.MessageAction;
import it.hydr4.lifes.death.actions.PermabanAction;
import it.hydr4.lifes.discord.OfflineGateway;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActionSetsBuilderTest {
    @Test
    void buildsDeathAndExhaustionPipelines() {
        var settings = new LivesSettings(
            3, 10, 1, java.util.Set.of(),
            List.of(new DeathActionSpec("MESSAGE", Map.of("target", "BROADCAST", "message", "died"), "death.actions[0]")),
            List.of(new DeathActionSpec("PERMABAN", Map.of(), "exhaustion.actions[0]")),
            it.hydr4.lifes.config.ZeroLivesJoin.REAPPLY, 0, true, null);
        var sets = ActionSetsBuilder.build(settings, OfflineGateway.create());
        assertEquals(1, sets.death().size());
        assertInstanceOf(MessageAction.class, sets.death().get(0));
        assertEquals(1, sets.exhaustion().size());
        assertInstanceOf(PermabanAction.class, sets.exhaustion().get(0));
    }

    @Test
    void unknownTypeFailsWithTheConfiguredPath() {
        var settings = new LivesSettings(
            3, 10, 1, java.util.Set.of(),
            List.of(new DeathActionSpec("EXPLODE", Map.of(), "death.actions[0]")),
            List.of(),
            it.hydr4.lifes.config.ZeroLivesJoin.REAPPLY, 0, true, null);
        var exception = assertThrows(ConfigException.class, () -> ActionSetsBuilder.build(settings, OfflineGateway.create()));
        assertEquals(
            "death.actions[0]: unknown action type 'EXPLODE'; expected MESSAGE, SOUND, COMMAND, PERMABAN or DISCORD",
            exception.getMessage());
    }

    @Test
    void missingMessageOptionFailsWithPath() {
        var settings = new LivesSettings(
            3, 10, 1, java.util.Set.of(),
            List.of(new DeathActionSpec("MESSAGE", Map.of(), "death.actions[0]")),
            List.of(),
            it.hydr4.lifes.config.ZeroLivesJoin.REAPPLY, 0, true, null);
        var exception = assertThrows(ConfigException.class, () -> ActionSetsBuilder.build(settings, OfflineGateway.create()));
        assertEquals("death.actions[0].message: expected a non-blank string", exception.getMessage());
    }

    @Test
    void discordActionBuilds() {
        var settings = new LivesSettings(
            3, 10, 1, java.util.Set.of(),
            List.of(new DeathActionSpec("DISCORD",
                Map.of("webhook", "https://discord.com/api/webhooks/1234567890123456789/secret-token",
                    "payload", "{\"content\": \"{player} died\"}"),
                "death.actions[0]")),
            List.of(),
            it.hydr4.lifes.config.ZeroLivesJoin.REAPPLY, 0, true, null);
        var sets = ActionSetsBuilder.build(settings, OfflineGateway.create());
        assertEquals(1, sets.death().size());
        assertInstanceOf(DiscordAction.class, sets.death().get(0));
    }

    @Test
    void discordWithAForeignHostFailsWithPath() {
        var settings = new LivesSettings(
            3, 10, 1, java.util.Set.of(),
            List.of(new DeathActionSpec("DISCORD",
                Map.of("webhook", "https://example.com/api/webhooks/1234567890123456789/secret-token",
                    "payload", "{\"content\": \"{player} died\"}"),
                "death.actions[0]")),
            List.of(),
            it.hydr4.lifes.config.ZeroLivesJoin.REAPPLY, 0, true, null);
        var exception = assertThrows(ConfigException.class, () -> ActionSetsBuilder.build(settings, OfflineGateway.create()));
        assertTrue(exception.getMessage().startsWith("death.actions[0].webhook: host must be one of"),
            exception.getMessage());
    }

    @Test
    void discordWithANonObjectPayloadFailsWithPath() {
        var settings = new LivesSettings(
            3, 10, 1, java.util.Set.of(),
            List.of(new DeathActionSpec("DISCORD",
                Map.of("webhook", "https://discord.com/api/webhooks/1234567890123456789/secret-token",
                    "payload", "[\"not\", \"an object\"]"),
                "death.actions[0]")),
            List.of(),
            it.hydr4.lifes.config.ZeroLivesJoin.REAPPLY, 0, true, null);
        var exception = assertThrows(ConfigException.class, () -> ActionSetsBuilder.build(settings, OfflineGateway.create()));
        assertEquals("death.actions[0].payload: expected a JSON object, found a JSON array",
            exception.getMessage());
    }

    @Test
    void discordWithAnUnknownOptionFailsWithPath() {
        var settings = new LivesSettings(
            3, 10, 1, java.util.Set.of(),
            List.of(new DeathActionSpec("DISCORD",
                Map.of("webhook", "https://discord.com/api/webhooks/1234567890123456789/secret-token",
                    "payload", "{\"content\": \"{player} died\"}",
                    "usernam", "Lifes"),
                "death.actions[0]")),
            List.of(),
            it.hydr4.lifes.config.ZeroLivesJoin.REAPPLY, 0, true, null);
        var exception = assertThrows(ConfigException.class, () -> ActionSetsBuilder.build(settings, OfflineGateway.create()));
        assertTrue(exception.getMessage().startsWith("death.actions[0].usernam: unknown option"),
            exception.getMessage());
    }
}
