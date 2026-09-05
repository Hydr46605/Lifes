package it.hydr4.lifes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LifesRuntimeTest {
    private static final String VALID = """
        version: 1
        lives:
          default: 3
          maximum: 10
        death:
          cost-per-death: 1
          ignored-causes:
            - FALL
            - VOID
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
    void loadsValidSettings() throws IOException {
        var runtime = LifesRuntime.load(write(VALID));
        assertEquals(10, runtime.maximumLives());
        assertEquals(2, runtime.settings().ignoredDeathCauses().size());
        assertEquals(1, runtime.settings().deathActions().size());
    }

    @Test
    void unknownDamageCauseFailsWithTheCauseName() throws IOException {
        var broken = VALID.replace("- VOID", "- NOT_A_CAUSE");
        var exception = assertThrows(ConfigException.class, () -> LifesRuntime.load(write(broken)));
        assertTrue(exception.getMessage().contains("NOT_A_CAUSE"), exception.getMessage());
    }

    @Test
    void reloadPicksUpNewMaximum() throws IOException {
        var file = write(VALID);
        var runtime = LifesRuntime.load(file);
        Files.writeString(file, VALID.replace("maximum: 10", "maximum: 15"));
        runtime.reload();
        assertEquals(15, runtime.maximumLives());
    }

    @Test
    void failedReloadKeepsThePreviousSnapshot() throws IOException {
        var file = write(VALID);
        var runtime = LifesRuntime.load(file);
        Files.writeString(file, "garbage: [");
        assertThrows(ConfigException.class, runtime::reload);
        assertEquals(10, runtime.maximumLives());
    }

    @Test
    void missingFileFailsWithThePath() {
        var exception = assertThrows(ConfigException.class, () -> LifesRuntime.load(dir.resolve("nope.yml")));
        assertTrue(exception.getMessage().contains("nope.yml"), exception.getMessage());
    }
}
