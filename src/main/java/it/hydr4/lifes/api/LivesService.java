package it.hydr4.lifes.api;

import java.util.Optional;
import java.util.UUID;

/** Domain boundary for lives state. Implementations must be thread-safe. */
public interface LivesService {
    Optional<LivesAccount> find(UUID id);

    Optional<LivesAccount> findByName(String name);

    /** Creates the account with the given initial lives; fails if it exists. */
    LivesAccount create(UUID id, String name);

    /**
     * Records one death: applies the cost, increments the death counter and
     * updates the last-death instant. Creates the account on first death.
     */
    LifeChange applyDeath(UUID id, String name, int cost);

    /** Applies an administrative change to an existing account. */
    LifeChange adjust(UUID id, LifeChangeReason reason, int amount);

    void addListener(LivesListener listener);
}
