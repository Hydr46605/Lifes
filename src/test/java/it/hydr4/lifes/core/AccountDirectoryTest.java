package it.hydr4.lifes.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.lifes.api.LivesAccount;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountDirectoryTest {
    @Test
    void createFailsOnDuplicateId() {
        var directory = new AccountDirectory();
        var id = UUID.randomUUID();
        directory.create(id, "Hydr4", 3);
        assertThrows(IllegalStateException.class, () -> directory.create(id, "Hydr4", 3));
    }

    @Test
    void computeReplacesSnapshotAtomically() {
        var directory = new AccountDirectory();
        var id = UUID.randomUUID();
        directory.create(id, "Hydr4", 3);
        var updated = directory.compute(id, (uuid, current) ->
            new LivesAccount(uuid, current.name(), current.lives() - 1, 1, Instant.now(), false));
        assertEquals(2, updated.lives());
        assertEquals(2, directory.find(id).orElseThrow().lives());
    }

    @Test
    void findByNameIgnoresCase() {
        var directory = new AccountDirectory();
        var id = UUID.randomUUID();
        directory.create(id, "Hydr4", 3);
        assertTrue(directory.findByName("HYDR4").isPresent());
        assertTrue(directory.findByName("unknown").isEmpty());
    }

    @Test
    void loadAllCountsDuplicates() {
        var directory = new AccountDirectory();
        var id = UUID.randomUUID();
        directory.create(id, "Hydr4", 3);
        Map<UUID, LivesAccount> loaded = new HashMap<>();
        loaded.put(id, new LivesAccount(id, "Hydr4", 5, 0, null, false));
        loaded.put(UUID.randomUUID(), new LivesAccount(UUID.randomUUID(), "Other", 5, 0, null, false));
        assertEquals(1, directory.loadAll(loaded));
        assertEquals(2, directory.size());
    }
}
