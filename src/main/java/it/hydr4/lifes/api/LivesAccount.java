package it.hydr4.lifes.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of one player's lives state.
 *
 * @param uuid         account owner
 * @param name         last known player name
 * @param lives        current lives, always within {@code [0, maximum]}
 * @param totalDeaths  number of recorded deaths
 * @param lastDeathAt  instant of the last recorded death, null if never died
 * @param exhausted    true once the account ran out of lives
 */
public record LivesAccount(
    UUID uuid,
    String name,
    int lives,
    int totalDeaths,
    Instant lastDeathAt,
    boolean exhausted
) {
    public LivesAccount {
        java.util.Objects.requireNonNull(uuid, "uuid");
        java.util.Objects.requireNonNull(name, "name");
        if (lives < 0) {
            throw new IllegalArgumentException("lives must be >= 0: " + lives);
        }
        if (totalDeaths < 0) {
            throw new IllegalArgumentException("totalDeaths must be >= 0: " + totalDeaths);
        }
    }
}
