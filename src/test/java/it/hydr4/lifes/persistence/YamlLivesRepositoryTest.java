package it.hydr4.lifes.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.lifes.ConfigException;
import it.hydr4.lifes.api.LivesAccount;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlLivesRepositoryTest {
    @TempDir
    Path dir;

    private Path file() {
        return dir.resolve("saves.yml");
    }

    @Test
    void roundTripsEveryField() throws IOException {
        var repository = new YamlLivesRepository(file());
        var died = Instant.now();
        var accounts = new HashMap<UUID, LivesAccount>();
        var fresh = new LivesAccount(UUID.randomUUID(), "Hydr4", 3, 0, null, false);
        var spent = new LivesAccount(UUID.randomUUID(), "Guest", 0, 12, died, true);
        accounts.put(fresh.uuid(), fresh);
        accounts.put(spent.uuid(), spent);
        repository.saveAll(accounts);

        var loaded = repository.loadAll();
        assertEquals(2, loaded.size());
        assertEquals(fresh, loaded.get(fresh.uuid()));
        assertEquals(spent, loaded.get(spent.uuid()));
        assertTrue(died.equals(loaded.get(spent.uuid()).lastDeathAt()));
    }

    @Test
    void globalCorruptionIsPreservedAndAborts() throws IOException {
        var repository = new YamlLivesRepository(file());
        Files.writeString(file(), "users: not: a: mapping");
        var exception = assertThrows(ConfigException.class, repository::loadAll);
        assertTrue(exception.getMessage().contains("saves file is corrupt"), exception.getMessage());
        try (var siblings = Files.list(dir)) {
            assertTrue(siblings.anyMatch(p -> p.getFileName().toString().startsWith("saves.yml.broken-")));
        }
    }

    @Test
    void badEntriesAreSkippedButOthersSurvive() throws IOException {
        var repository = new YamlLivesRepository(file());
        var good = UUID.randomUUID();
        var bad = UUID.randomUUID();
        Files.writeString(file(), """
            version: 1
            users:
              "%s":
                name: Good
                lives: 2
                total-deaths: 1
                last-death: 2026-01-01T00:00:00Z
              "%s":
                name: Bad
                lives: many
                total-deaths: 1
            """.formatted(good, bad));

        var loaded = repository.loadAll();
        assertEquals(1, loaded.size());
        assertEquals("Good", loaded.get(good).name());
    }

    @Test
    void unknownVersionAborts() throws IOException {
        var repository = new YamlLivesRepository(file());
        Files.writeString(file(), "version: 999\nusers: {}\n");
        assertThrows(ConfigException.class, repository::loadAll);
    }

    @Test
    void missingFileLoadsEmpty() {
        var repository = new YamlLivesRepository(file());
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void noTempFilesRemainAfterSave() throws IOException {
        var repository = new YamlLivesRepository(file());
        repository.saveAll(Map.of());
        try (var siblings = Files.list(dir)) {
            assertTrue(siblings.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
    }
}
