package it.hydr4.lifes.hook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.lifes.core.AccountDirectory;
import it.hydr4.lifes.core.DefaultLivesService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LifesSkriptTest {
    @Test
    void emptyWhenUnpublished() {
        LifesSkript.unpublish();
        assertTrue(LifesSkript.service().isEmpty());
    }

    @Test
    void publishedServiceExposesLives() {
        var directory = new AccountDirectory();
        var service = new DefaultLivesService(directory, () -> new it.hydr4.lifes.config.LivesSettings(
            3, 10, 1, java.util.Set.of(), java.util.List.of(), java.util.List.of(),
            java.util.List.of(), java.util.List.of(),
            it.hydr4.lifes.config.UltimateUiSettings.disabled(),
            it.hydr4.lifes.config.ZeroLivesJoin.REAPPLY, 0, true,
            it.hydr4.lifes.text.MessageTemplates.withOverrides(java.util.Map.of())));
        LifesSkript.publish(service);
        try {
            var id = UUID.randomUUID();
            service.create(id, "Hydr4");
            assertEquals(3, LifesSkript.livesOf(id).orElseThrow());
            assertEquals(3, LifesSkript.livesOfName("hydr4").orElseThrow());
            assertTrue(LifesSkript.livesOf(UUID.randomUUID()).isEmpty());
        } finally {
            LifesSkript.unpublish();
        }
    }
}
