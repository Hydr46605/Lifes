package it.hydr4.lifes.hook;

import it.hydr4.lifes.api.LivesAccount;
import it.hydr4.lifes.api.LivesService;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Platform-free placeholder resolution; unknown placeholders yield null.
 *
 * <p>Dead-account placeholders: {@code dead_count}, {@code dead_list},
 * {@code dead_list_<limit>} and {@code dead_list_<limit>_<separator>} ({@code :} works as
 * well as {@code _}). The separator is one of {@code comma} (default), {@code newline},
 * {@code pipe}, or {@code plus} for the {@code +}-joined speedrun style. Bare
 * {@code dead_list} shows the first {@value DeadAccounts#DEFAULT_LIMIT} names and appends
 * {@code (+N more)} when truncated. An unreadable limit yields the empty string.
 */
public final class PlaceholderResolver {
    private final LivesService service;
    private final Supplier<Integer> maximumLives;
    private final Supplier<Integer> defaultLives;
    private final Supplier<Collection<LivesAccount>> accounts;

    public PlaceholderResolver(LivesService service, Supplier<Integer> maximumLives, Supplier<Integer> defaultLives) {
        this(service, maximumLives, defaultLives, java.util.List::of);
    }

    public PlaceholderResolver(LivesService service, Supplier<Integer> maximumLives,
        Supplier<Integer> defaultLives, Supplier<Collection<LivesAccount>> accounts) {
        this.service = java.util.Objects.requireNonNull(service, "service");
        this.maximumLives = java.util.Objects.requireNonNull(maximumLives, "maximumLives");
        this.defaultLives = java.util.Objects.requireNonNull(defaultLives, "defaultLives");
        this.accounts = java.util.Objects.requireNonNull(accounts, "accounts");
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
            case "dead_count" -> String.valueOf(DeadAccounts.count(accounts));
            default -> deadList(params);
        };
    }

    private String deadList(String params) {
        if (!params.startsWith("dead_list")) {
            return null;
        }
        var rest = params.substring("dead_list".length()).replace(':', '_');
        if (rest.isEmpty()) {
            return join(DeadAccounts.DEFAULT_LIMIT, ", ");
        }
        if (!rest.startsWith("_")) {
            return null;
        }
        var parts = rest.substring(1).split("_", -1);
        if (parts.length > 2) {
            return "";
        }
        int limit;
        try {
            limit = Integer.parseInt(parts[0]);
        } catch (NumberFormatException exception) {
            return "";
        }
        if (limit < 1 || limit > DeadAccounts.MAX_LIMIT) {
            return "";
        }
        var separator = ", ";
        if (parts.length == 2) {
            separator = switch (parts[1]) {
                case "comma" -> ", ";
                case "newline" -> "\n";
                case "pipe" -> " | ";
                case "plus" -> "+";
                default -> null;
            };
            if (separator == null) {
                return "";
            }
        }
        return join(limit, separator);
    }

    private String join(int limit, String separator) {
        var total = DeadAccounts.count(accounts);
        var names = DeadAccounts.names(accounts, limit);
        var shown = names.size() > limit ? names.subList(0, limit) : names;
        var body = String.join(separator, shown);
        if (names.size() > limit) {
            body += separator.equals("\n") ? "\n(+%d more)".formatted(total - limit)
                : " (+%d more)".formatted(total - limit);
        }
        return body;
    }
}
