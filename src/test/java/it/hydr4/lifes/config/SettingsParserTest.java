package it.hydr4.lifes.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.lifes.ConfigException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsParserTest {
    private static final String VALID = """
        version: 1
        lives:
          default: 3
          maximum: 10
        death:
          cost-per-death: 1
          ignored-causes: []
          actions:
            - type: MESSAGE
              target: VICTIM
              message: "<red>lost</red>"
        exhaustion:
          actions:
            - type: PERMABAN
              reason: "gone"
        persistence:
          save-interval-seconds: 300
          save-off-thread: true
        messages:
          lives-self: "You have {lives}."
        """;

    @TempDir
    Path dir;

    private Path write(String content) throws IOException {
        var file = dir.resolve("settings.yml");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void parsesValidConfiguration() throws IOException {
        var settings = SettingsParser.parse(write(VALID));
        assertEquals(3, settings.defaultLives());
        assertEquals(10, settings.maximumLives());
        assertEquals(1, settings.deathCost());
        assertEquals(1, settings.deathActions().size());
        assertEquals(1, settings.exhaustionActions().size());
        assertEquals(300, settings.saveIntervalSeconds());
        assertTrue(settings.saveOffThread());
    }

    @Test
    void messagesSectionMayBeEmpty() throws IOException {
        var content = VALID.replace("messages:\n  lives-self: \"You have {lives}.\"", "messages: {}");
        var settings = SettingsParser.parse(write(content));
        assertEquals(3, settings.defaultLives());
    }

    @Test
    void unknownRootKeyFailsWithPath() throws IOException {
        var exception = assertThrows(ConfigException.class,
            () -> SettingsParser.parse(write(VALID + "hacks: true\n")));
        assertTrue(exception.getMessage().contains("hacks"), exception.getMessage());
    }

    @Test
    void unknownMessageKeyFailsWithPath() throws IOException {
        var exception = assertThrows(ConfigException.class,
            () -> SettingsParser.parse(write(VALID + "  lives-bogus: \"x\"\n")));
        assertTrue(exception.getMessage().contains("messages.lives-bogus"), exception.getMessage());
    }

    @Test
    void wrongTypeFailsWithPath() throws IOException {
        var broken = VALID.replace("default: 3", "default: three");
        var exception = assertThrows(ConfigException.class, () -> SettingsParser.parse(write(broken)));
        assertTrue(exception.getMessage().contains("lives.default"), exception.getMessage());
    }

    @Test
    void missingRequiredKeyFailsWithPath() throws IOException {
        var broken = VALID.replace("  maximum: 10\n", "");
        var exception = assertThrows(ConfigException.class, () -> SettingsParser.parse(write(broken)));
        assertTrue(exception.getMessage().contains("lives.maximum"), exception.getMessage());
    }

    @Test
    void maximumBelowDefaultFails() throws IOException {
        var broken = VALID.replace("maximum: 10", "maximum: 1");
        assertThrows(ConfigException.class, () -> SettingsParser.parse(write(broken)));
    }

    @Test
    void causeNamesMustBeUppercase() throws IOException {
        var broken = VALID.replace("ignored-causes: []", "ignored-causes: [fall]");
        var exception = assertThrows(ConfigException.class, () -> SettingsParser.parse(write(broken)));
        assertTrue(exception.getMessage().contains("death.ignored-causes"), exception.getMessage());
    }

    @Test
    void booleanMustBeBoolean() throws IOException {
        var broken = VALID.replace("save-off-thread: true", "save-off-thread: yes-please");
        var exception = assertThrows(ConfigException.class, () -> SettingsParser.parse(write(broken)));
        assertTrue(exception.getMessage().contains("persistence.save-off-thread"), exception.getMessage());
    }

    @Test
    void actionMustBeAMapping() throws IOException {
        var broken = """
            version: 1
            lives:
              default: 3
              maximum: 10
            death:
              cost-per-death: 1
              ignored-causes: []
              actions:
                - HELLO
            exhaustion:
              actions: []
            persistence:
              save-interval-seconds: 0
              save-off-thread: true
            messages: {}
            """;
        var exception = assertThrows(ConfigException.class, () -> SettingsParser.parse(write(broken)));
        assertTrue(exception.getMessage().contains("expected an action mapping"), exception.getMessage());
    }

    @Test
    void loadReportsTheSourcePath() throws IOException {
        var file = write("version: 1");
        var exception = assertThrows(ConfigException.class, () -> SettingsParser.parse(file));
        assertTrue(exception.getMessage().startsWith(file.toString()), exception.getMessage());
        List.of();
    }
}
