package it.hydr4.lifes.core;

import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.api.LivesListener;
import it.hydr4.lifes.api.LivesService;
import it.hydr4.lifes.api.UnknownAccountException;
import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.util.Checks;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/** Domain rules: clamping, death bookkeeping, exhaustion edge. No Bukkit, no IO. */
public final class DefaultLivesService implements LivesService {
    private final AccountDirectory directory;
    private final Supplier<LivesSettings> settings;
    private final List<LivesListener> listeners = new CopyOnWriteArrayList<>();

    public DefaultLivesService(AccountDirectory directory, Supplier<LivesSettings> settings) {
        this.directory = Checks.notNull(directory, "directory");
        this.settings = Checks.notNull(settings, "settings");
    }

    @Override
    public Optional<LivesAccount> find(UUID id) {
        return directory.find(id);
    }

    @Override
    public Optional<LivesAccount> findByName(String name) {
        return directory.findByName(name);
    }

    @Override
    public LivesAccount create(UUID id, String name) {
        return directory.create(id, name, settings.get().defaultLives());
    }

    @Override
    public LifeChange applyDeath(UUID id, String name, int cost) {
        Checks.notNull(id, "id");
        Checks.notBlank(name, "name");
        Checks.atLeast(cost, 0, "cost");
        var now = Instant.now();
        if (directory.find(id).isEmpty()) {
            directory.create(id, name, settings.get().defaultLives());
        }
        var result = new java.util.concurrent.atomic.AtomicReference<LifeChange>();
        directory.compute(id, (uuid, live) -> {
            var lives = Math.max(0, live.lives() - cost);
            var after = new LivesAccount(uuid, name, lives, live.totalDeaths() + 1, now, lives == 0);
            result.set(new LifeChange(LifeChangeReason.DEATH, live, after, lives - live.lives()));
            return after;
        });
        return commit(result.get());
    }

    @Override
    public LifeChange adjust(UUID id, LifeChangeReason reason, int amount) {
        Checks.notNull(id, "id");
        Checks.notNull(reason, "reason");
        var result = new java.util.concurrent.atomic.AtomicReference<LifeChange>();
        directory.compute(id, (uuid, live) -> {
            if (live == null) {
                throw new UnknownAccountException(uuid);
            }
            var maximum = settings.get().maximumLives();
            var after = switch (reason) {
                case ADMIN_SET -> clamp(live, Checks.inRange(amount, 0, maximum, "amount"));
                // Widened on purpose: lives + a huge amount must saturate at the cap, not wrap
                // around to a negative number that clamp() would then read as zero lives.
                case ADMIN_ADD -> clamp(live, (long) live.lives() + Checks.atLeast(amount, 0, "amount"));
                case ADMIN_REMOVE -> clamp(live, (long) live.lives() - Checks.atLeast(amount, 0, "amount"));
                case ADMIN_RESET -> clamp(live, settings.get().defaultLives());
                case DEATH -> throw new IllegalArgumentException("DEATH must use applyDeath");
            };
            result.set(new LifeChange(reason, live, after, after.lives() - live.lives()));
            return after;
        });
        return commit(result.get());
    }

    @Override
    public void addListener(LivesListener listener) {
        listeners.add(Checks.notNull(listener, "listener"));
    }

    private LivesAccount clamp(LivesAccount before, long lives) {
        var bounded = (int) Math.max(0L, Math.min(lives, settings.get().maximumLives()));
        var exhausted = bounded == 0;
        return new LivesAccount(before.uuid(), before.name(), bounded, before.totalDeaths(), before.lastDeathAt(), exhausted);
    }

    private LifeChange commit(LifeChange change) {
        if (change.changed()) {
            for (var listener : listeners) {
                listener.onLifeChange(change);
            }
        }
        return change;
    }
}
