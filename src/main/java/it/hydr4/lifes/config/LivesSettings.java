package it.hydr4.lifes.config;

import it.hydr4.lifes.text.MessageTemplates;

import java.util.List;
import java.util.Set;

/** Validated settings; parsing is the only checkpoint. */
public record LivesSettings(
    int defaultLives,
    int maximumLives,
    int deathCost,
    Set<String> ignoredDeathCauses,
    List<DeathActionSpec> deathActions,
    List<DeathActionSpec> exhaustionActions,
    ZeroLivesJoin zeroLivesJoin,
    int saveIntervalSeconds,
    boolean saveOffThread,
    MessageTemplates messages
) {
    public LivesSettings {
        ignoredDeathCauses = Set.copyOf(ignoredDeathCauses);
        deathActions = List.copyOf(deathActions);
        exhaustionActions = List.copyOf(exhaustionActions);
    }
}
