package it.hydr4.lifes.hook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesService;
import it.hydr4.lifes.core.AccountDirectory;
import it.hydr4.lifes.core.DefaultLivesService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlaceholderResolverTest {
    private final AccountDirectory directory = new AccountDirectory();
    private final LivesService service = new DefaultLivesService(directory, () -> new it.hydr4.lifes.config.LivesSettings(
        3, 10, 1, java.util.Set.of(), java.util.List.of(), java.util.List.of(), 0, true, null));
    private final PlaceholderResolver resolver = new PlaceholderResolver(
        service, () -> 10, () -> 3);

    @Test
    void unknownPlaceholderYieldsNull() {
        assertNull(resolver.resolve(UUID.randomUUID(), "not_a_placeholder"));
    }

    @Test
    void accountPlaceholdersAreEmptyWithoutAnAccount() {
        var id = UUID.randomUUID();
        assertEquals("", resolver.resolve(id, "lives"));
        assertEquals("", resolver.resolve(id, "status"));
        assertEquals("never", resolver.resolve(id, "last_death"));
    }

    @Test
    void globalPlaceholdersWorkWithoutAnAccount() {
        assertEquals("10", resolver.resolve(null, "max"));
        assertEquals("3", resolver.resolve(null, "default"));
    }

    @Test
    void accountPlaceholdersReflectState() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        service.applyDeath(id, "Hydr4", 2);
        assertEquals("1", resolver.resolve(id, "lives"));
        assertEquals("9", resolver.resolve(id, "remaining"));
        assertEquals("1", resolver.resolve(id, "total_deaths"));
        assertEquals("alive", resolver.resolve(id, "status"));

        service.adjust(id, LifeChangeReason.ADMIN_REMOVE, 1);
        assertEquals("exhausted", resolver.resolve(id, "status"));
    }
}
