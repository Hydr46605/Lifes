package it.hydr4.lifes.death;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.config.DeathActionSpec;
import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.death.actions.MessageAction;
import it.hydr4.lifes.death.actions.PermabanAction;
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
            0, true, null);
        var sets = ActionSetsBuilder.build(settings);
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
            0, true, null);
        var exception = assertThrows(ConfigException.class, () -> ActionSetsBuilder.build(settings));
        assertEquals(
            "death.actions[0]: unknown action type 'EXPLODE'; expected MESSAGE, SOUND, COMMAND or PERMABAN",
            exception.getMessage());
    }

    @Test
    void missingMessageOptionFailsWithPath() {
        var settings = new LivesSettings(
            3, 10, 1, java.util.Set.of(),
            List.of(new DeathActionSpec("MESSAGE", Map.of(), "death.actions[0]")),
            List.of(),
            0, true, null);
        var exception = assertThrows(ConfigException.class, () -> ActionSetsBuilder.build(settings));
        assertEquals("death.actions[0].message: expected a non-blank string", exception.getMessage());
    }
}
