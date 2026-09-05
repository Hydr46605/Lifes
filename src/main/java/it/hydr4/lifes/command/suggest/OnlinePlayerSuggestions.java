package it.hydr4.lifes.command.suggest;

import it.hydr4.democracy.api.command.CommandSource;
import it.hydr4.democracy.api.command.SuggestionProvider;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Suggests online player names, filtered by the token being typed. */
public final class OnlinePlayerSuggestions implements SuggestionProvider {
    @Override
    public List<String> suggest(CommandSource source, String input, String prefix) {
        var lowered = prefix.toLowerCase(Locale.ROOT);
        var names = new ArrayList<String>();
        try {
            for (var player : Bukkit.getOnlinePlayers()) {
                var name = player.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(lowered)) {
                    names.add(name);
                }
            }
        } catch (RuntimeException ignored) {
            // No server available (tests, early lifecycle): no dynamic suggestions.
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }
}
