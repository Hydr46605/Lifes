package it.hydr4.lifes.hook;

import it.hydr4.lifes.api.LivesAccount;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Shared dead-account view: exhausted accounts sorted A-Z (case-insensitive).
 * One home for the placeholder, command and Skript surfaces so they can never disagree.
 */
public final class DeadAccounts {
    /** Hard cap for any dead-list output; keeps chat lines and placeholders bounded. */
    public static final int MAX_LIMIT = 100;
    /** Default entries shown when no limit is requested. */
    public static final int DEFAULT_LIMIT = 20;

    private DeadAccounts() {
    }

    /**
     * Names of exhausted accounts, sorted A-Z, with one extra slot: when the result holds more
     * than {@code limit} names the output was truncated and the last name must be dropped in
     * favour of a {@code (+N more)} note, where {@code N = total - limit}.
     */
    public static List<String> names(Supplier<Collection<LivesAccount>> accounts, int limit) {
        var capped = Math.max(1, Math.min(limit, MAX_LIMIT));
        return accounts.get().stream()
            .filter(LivesAccount::exhausted)
            .map(LivesAccount::name)
            .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
            .limit(capped + 1L)
            .toList();
    }

    /** Number of exhausted accounts. */
    public static long count(Supplier<Collection<LivesAccount>> accounts) {
        return accounts.get().stream().filter(LivesAccount::exhausted).count();
    }

    /** Name at the 1-based position of the sorted dead list, or empty when out of range. */
    public static Optional<String> at(Supplier<Collection<LivesAccount>> accounts, int index) {
        return accounts.get().stream()
            .filter(LivesAccount::exhausted)
            .map(LivesAccount::name)
            .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
            .skip(index - 1L)
            .findFirst();
    }
}
