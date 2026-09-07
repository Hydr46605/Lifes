package it.hydr4.lifes.hook;

import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.api.LivesService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Static entry point for Skript scripts (via skript-reflect) and other integrations.
 *
 * <p>Kept free of any Skript API types on purpose: the plugin never depends on Skript at
 * compile time, so this works with every Skript version and can never break the build when
 * Skript is absent. {@link SkriptHook} publishes the running {@link LivesService} here on
 * enable and clears it on disable.
 */
public final class LifesSkript {
    private static final AtomicReference<LivesService> SERVICE = new AtomicReference<>();

    private LifesSkript() {
    }

    static void publish(LivesService service) {
        SERVICE.set(service);
    }

    static void unpublish() {
        SERVICE.set(null);
    }

    /** The live service, or empty when the plugin is disabled. */
    public static Optional<LivesService> service() {
        return Optional.ofNullable(SERVICE.get());
    }

    /** Lives of an account by UUID, or empty when unknown. */
    public static Optional<Integer> livesOf(UUID id) {
        return service().flatMap(service -> service.find(id)).map(LivesAccount::lives);
    }

    /** Lives of an account by last known name, or empty when unknown. */
    public static Optional<Integer> livesOfName(String name) {
        return service().flatMap(service -> service.findByName(name)).map(LivesAccount::lives);
    }

    /** Full account snapshot by UUID, or empty when unknown. */
    public static Optional<LivesAccount> accountOf(UUID id) {
        return service().flatMap(service -> service.find(id));
    }

    /**
     * Adjusts an account from a Skript-friendly reason name (for example {@code "ADMIN_ADD"}).
     * Unknown accounts and unknown reason names throw, mirroring the Java API.
     */
    public static void adjust(UUID id, String reasonName, int amount) {
        var service = SERVICE.get();
        if (service == null) {
            throw new IllegalStateException("Lifes is not enabled");
        }
        service.adjust(id, LifeChangeReason.valueOf(reasonName), amount);
    }
}
