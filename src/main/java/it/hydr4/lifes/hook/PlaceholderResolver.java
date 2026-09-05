package it.hydr4.lifes.hook;

import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.api.LivesService;

import java.util.UUID;
import java.util.function.Supplier;

/** Platform-free placeholder resolution; unknown placeholders yield null. */
public final class PlaceholderResolver {
    private final LivesService service;
    private final Supplier<Integer> maximumLives;
    private final Supplier<Integer> defaultLives;

    public PlaceholderResolver(LivesService service, Supplier<Integer> maximumLives, Supplier<Integer> defaultLives) {
        this.service = java.util.Objects.requireNonNull(service, "service");
        this.maximumLives = java.util.Objects.requireNonNull(maximumLives, "maximumLives");
        this.defaultLives = java.util.Objects.requireNonNull(defaultLives, "defaultLives");
    }

    /** @return the placeholder value, or null when the placeholder is unknown. */
    public String resolve(UUID id, String params) {
        java.util.Objects.requireNonNull(params, "params");
        var account = id == null ? null : service.find(id).orElse(null);
        return switch (params) {
            case "lives" -> account == null ? "" : String.valueOf(account.lives());
            case "max" -> String.valueOf(maximumLives.get());
            case "default" -> String.valueOf(defaultLives.get());
            case "remaining" -> account == null ? ""
                : String.valueOf(Math.max(0, maximumLives.get() - account.lives()));
            case "total_deaths" -> account == null ? "" : String.valueOf(account.totalDeaths());
            case "status" -> account == null ? "" : account.exhausted() ? "exhausted" : "alive";
            case "last_death" -> account == null || account.lastDeathAt() == null
                ? "never"
                : account.lastDeathAt().toString();
            default -> null;
        };
    }
}
