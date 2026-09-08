package it.hydr4.lifes.hook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesService;
import it.hydr4.lifes.core.AccountDirectory;
import it.hydr4.lifes.core.DefaultLivesService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeadAccountsTest {
    private final AccountDirectory directory = new AccountDirectory();
    private final LivesService service = new DefaultLivesService(directory, () -> new it.hydr4.lifes.config.LivesSettings(
        3, 10, 1, java.util.Set.of(), java.util.List.of(), java.util.List.of(),
        java.util.List.of(), java.util.List.of(),
        it.hydr4.lifes.config.UltimateUiSettings.disabled(),
        it.hydr4.lifes.config.ZeroLivesJoin.REAPPLY, 0, true, null));
    private final PlaceholderResolver resolver = new PlaceholderResolver(
        service, () -> 10, () -> 3, directory::all);

    private UUID exhaust(String name) {
        var id = UUID.randomUUID();
        service.create(id, name);
        service.adjust(id, LifeChangeReason.ADMIN_REMOVE, 3);
        return id;
    }

    @Test
    void deadCountStartsAtZero() {
        assertEquals("0", resolver.resolve(null, "dead_count"));
    }

    @Test
    void deadListIsEmptyWithoutDeaths() {
        assertEquals("", resolver.resolve(null, "dead_list"));
    }

    @Test
    void deadCountAndListReflectExhaustion() {
        service.create(UUID.randomUUID(), "Hydr4");
        exhaust("Zed");
        exhaust("Amy");
        assertEquals("2", resolver.resolve(null, "dead_count"));
        assertEquals("Amy, Zed", resolver.resolve(null, "dead_list"));
    }

    @Test
    void deadListSupportsLimitAndSeparator() {
        exhaust("Zed");
        exhaust("Amy");
        exhaust("Max");
        assertEquals("Amy | Max (+1 more)", resolver.resolve(null, "dead_list_2_pipe"));
        assertEquals("Amy\nMax\nZed", resolver.resolve(null, "dead_list:10:newline"));
        assertEquals("Amy+Max+Zed", resolver.resolve(null, "dead_list_10_plus"));
    }

    @Test
    void deadListTruncatesWithAMoreNote() {
        for (var index = 0; index < DeadAccounts.DEFAULT_LIMIT + 1; index++) {
            exhaust("Victim%02d".formatted(index));
        }
        var text = resolver.resolve(null, "dead_list");
        assertTrue(text.endsWith(" (+1 more)"), text);
        assertTrue(!text.contains("Victim20"), text);
    }

    @Test
    void deadListRejectsBadArguments() {
        exhaust("Ghost");
        assertEquals("", resolver.resolve(null, "dead_list_zero"));
        assertEquals("", resolver.resolve(null, "dead_list_101"));
        assertEquals("", resolver.resolve(null, "dead_list_2_dots"));
        assertEquals("", resolver.resolve(null, "dead_list_2_comma_extra"));
    }

    @Test
    void unknownPlaceholdersStillYieldNull() {
        assertNull(resolver.resolve(null, "dead_pool"));
    }

    @Test
    void deadIndexYieldsSingleNames() {
        service.create(UUID.randomUUID(), "Hydr4");
        exhaust("Zed");
        exhaust("Amy");
        assertEquals("Amy", resolver.resolve(null, "dead_1"));
        assertEquals("Zed", resolver.resolve(null, "dead_2"));
        assertEquals("", resolver.resolve(null, "dead_3"));
        assertEquals("", resolver.resolve(null, "dead_0"));
    }

    @Test
    void lastVictimTracksCountedDeaths() {
        var tracker = new it.hydr4.lifes.core.LastVictim();
        var withVictim = new PlaceholderResolver(service, () -> 10, () -> 3, directory::all, tracker::name);
        assertEquals("", withVictim.resolve(null, "last_victim"));
        service.addListener(tracker);
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        service.adjust(id, LifeChangeReason.ADMIN_ADD, 1);
        assertEquals("", withVictim.resolve(null, "last_victim"));
        service.applyDeath(id, "Hydr4", 1);
        assertEquals("Hydr4", withVictim.resolve(null, "last_victim"));
    }
}
