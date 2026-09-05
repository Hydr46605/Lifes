package it.hydr4.lifes.command.suggest;

import it.hydr4.democracy.api.command.CommandSource;
import it.hydr4.democracy.api.command.SuggestionProvider;
import it.hydr4.lifes.core.AccountDirectory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Suggests player names for player arguments.
 *
 * <p>Paper resolves Brigadier suggestions off the main thread, so this provider must not touch
 * the Bukkit API: it reads the {@link PlayerNameIndex} snapshot and the account directory, both
 * safe to read concurrently. Online players are listed before known offline accounts.
 */
public final class OnlinePlayerSuggestions implements SuggestionProvider {
    /** Upper bound on one response, so a large world does not flood the client. */
    static final int LIMIT = 60;

    private final PlayerNameIndex online;
    private final AccountDirectory accounts;

    public OnlinePlayerSuggestions(PlayerNameIndex online, AccountDirectory accounts) {
        this.online = Objects.requireNonNull(online, "online");
        this.accounts = Objects.requireNonNull(accounts, "accounts");
    }

    @Override
    public List<String> suggest(CommandSource source, String input, String prefix) {
        var lowered = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        var connected = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        var known = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (var name : online.onlineNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                connected.add(name);
            }
        }
        for (var account : accounts.all()) {
            var name = account.name();
            if (!online.isOnline(name) && name.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                known.add(name);
            }
        }

        var result = new ArrayList<String>(LIMIT);
        appendBounded(connected, result);
        appendBounded(known, result);
        return result;
    }

    private static void appendBounded(Iterable<String> source, List<String> target) {
        for (var name : source) {
            if (target.size() >= LIMIT) {
                return;
            }
            target.add(name);
        }
    }
}
