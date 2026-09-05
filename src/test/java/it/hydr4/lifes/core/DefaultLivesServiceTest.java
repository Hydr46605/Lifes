package it.hydr4.lifes.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.api.UnknownAccountException;
import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.text.MessageTemplates;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultLivesServiceTest {
    private final AccountDirectory directory = new AccountDirectory();
    private final DefaultLivesService service = new DefaultLivesService(directory, DefaultLivesServiceTest::settings);
    private final List<it.hydr4.lifes.api.LifeChange> changes = new ArrayList<>();

    DefaultLivesServiceTest() {
        service.addListener(changes::add);
    }

    private static LivesSettings settings() {
        return new LivesSettings(3, 10, 1, java.util.Set.of(), java.util.List.of(), java.util.List.of(), it.hydr4.lifes.config.ZeroLivesJoin.REAPPLY, 0, true, MessageTemplates.withOverrides(Map.of()));
    }

    @Test
    void createUsesConfiguredDefaults() {
        var account = service.create(UUID.randomUUID(), "Hydr4");
        assertEquals(3, account.lives());
        assertEquals(0, account.totalDeaths());
        assertTrue(!account.exhausted());
    }

    @Test
    void createRejectsDuplicates() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        assertThrows(IllegalStateException.class, () -> service.create(id, "Hydr4"));
    }

    @Test
    void deathConsumesLivesAndCountsDeaths() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        var change = service.applyDeath(id, "Hydr4", 1);
        assertEquals(2, change.after().lives());
        assertEquals(1, change.after().totalDeaths());
        assertEquals(-1, change.delta());
        assertEquals(1, changes.size());
    }

    @Test
    void deathOnUnknownAccountCreatesItFirst() {
        var change = service.applyDeath(UUID.randomUUID(), "Newcomer", 1);
        assertEquals(2, change.after().lives());
        assertEquals(1, change.after().totalDeaths());
    }

    @Test
    void deathExhaustsExactlyOnceAtZero() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        var first = service.applyDeath(id, "Hydr4", 3);
        assertTrue(first.exhausted(), "landing on zero must exhaust");
        var second = service.applyDeath(id, "Hydr4", 3);
        assertTrue(!second.exhausted(), "already-exhausted accounts must not re-trigger the edge");
        assertEquals(0, second.after().lives());
    }

    @Test
    void deathWithExcessCostClampsAtZero() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        var change = service.applyDeath(id, "Hydr4", 50);
        assertEquals(0, change.after().lives());
    }

    @Test
    void adminSetOutsideTheRangeIsRejected() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        assertThrows(IllegalArgumentException.class, () -> service.adjust(id, LifeChangeReason.ADMIN_SET, 99));
        assertEquals(3, service.find(id).orElseThrow().lives());
    }

    @Test
    void adminAddClampsAtMaximum() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        var change = service.adjust(id, LifeChangeReason.ADMIN_ADD, 50);
        assertEquals(10, change.after().lives());
    }

    @Test
    void adminAddWithAHugeAmountSaturatesInsteadOfDrainingLives() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        var change = service.adjust(id, LifeChangeReason.ADMIN_ADD, Integer.MAX_VALUE);
        assertEquals(10, change.after().lives(), "overflowing the int range must not land on zero lives");
        assertTrue(!change.after().exhausted(), "a gift of lives must never look like exhaustion");
    }

    @Test
    void adminRemoveToZeroTriggersExhaustionEdge() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        var change = service.adjust(id, LifeChangeReason.ADMIN_REMOVE, 3);
        assertTrue(change.exhausted());
        assertTrue(change.after().exhausted());
    }

    @Test
    void adminResetRevivesAndAllowsNewExhaustion() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        service.adjust(id, LifeChangeReason.ADMIN_REMOVE, 3);
        var reset = service.adjust(id, LifeChangeReason.ADMIN_RESET, 0);
        assertTrue(!reset.after().exhausted());
        assertEquals(3, reset.after().lives());
        var again = service.adjust(id, LifeChangeReason.ADMIN_REMOVE, 3);
        assertTrue(again.exhausted(), "revived accounts must be able to exhaust again");
    }

    @Test
    void noOpAdjustDoesNotNotifyListeners() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        service.adjust(id, LifeChangeReason.ADMIN_SET, 3);
        assertEquals(0, changes.size());
    }

    @Test
    void adjustOnUnknownAccountFails() {
        assertThrows(UnknownAccountException.class,
            () -> service.adjust(UUID.randomUUID(), LifeChangeReason.ADMIN_SET, 5));
    }

    @Test
    void findByNameIsCaseInsensitive() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        assertEquals(id, service.findByName("hYdR4").orElseThrow().uuid());
    }

    @Test
    void snapshotNeverMutates() {
        var id = UUID.randomUUID();
        service.create(id, "Hydr4");
        LivesAccount before = service.find(id).orElseThrow();
        service.applyDeath(id, "Hydr4", 1);
        assertEquals(3, before.lives());
    }
}
