package it.hydr4.lifes.core;

import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.util.Checks;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/** Thread-safe account store; every mutation is copy-on-write. */
public final class AccountDirectory {
    private final ConcurrentHashMap<UUID, LivesAccount> accounts = new ConcurrentHashMap<>();

    /** Creates an account; fails when the id is already present. */
    public LivesAccount create(UUID id, String name, int defaultLives) {
        Checks.notNull(id, "id");
        Checks.notBlank(name, "name");
        var account = new LivesAccount(id, name, defaultLives, 0, null, defaultLives == 0);
        var existing = accounts.putIfAbsent(id, account);
        if (existing != null) {
            throw new IllegalStateException("Account already exists: " + id);
        }
        return account;
    }

    /** Atomically derives a new snapshot from the current one. */
    public LivesAccount compute(UUID id, BiFunction<UUID, LivesAccount, LivesAccount> transition) {
        Checks.notNull(id, "id");
        Checks.notNull(transition, "transition");
        return accounts.compute(id, transition);
    }

    public Optional<LivesAccount> find(UUID id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public Optional<LivesAccount> findByName(String name) {
        Checks.notNull(name, "name");
        var wanted = name.toLowerCase(java.util.Locale.ROOT);
        for (var account : accounts.values()) {
            if (account.name().toLowerCase(java.util.Locale.ROOT).equals(wanted)) {
                return Optional.of(account);
            }
        }
        return Optional.empty();
    }

    public Collection<LivesAccount> all() {
        return accounts.values();
    }

    /** Seeds the directory from persistence; duplicates keep the stored value. */
    public int loadAll(Map<UUID, LivesAccount> loaded) {
        Checks.notNull(loaded, "loaded");
        var duplicates = 0;
        for (var entry : loaded.entrySet()) {
            if (accounts.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
                duplicates++;
            }
        }
        return duplicates;
    }

    public int size() {
        return accounts.size();
    }
}
