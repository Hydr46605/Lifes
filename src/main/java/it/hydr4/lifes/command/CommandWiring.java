package it.hydr4.lifes.command;

import it.hydr4.democracy.core.command.CommandRegistration;
import it.hydr4.democracy.core.command.DefaultCommandCatalog;
import it.hydr4.lifes.LifesRuntime;
import it.hydr4.lifes.api.LivesService;
import it.hydr4.lifes.command.suggest.OnlinePlayerSuggestions;
import it.hydr4.lifes.command.suggest.PlayerNameIndex;
import it.hydr4.lifes.command.suggest.SuggestionKeys;
import it.hydr4.lifes.core.AccountDirectory;
import org.bukkit.plugin.Plugin;

/** Registers the generated command; close to unregister. */
public final class CommandWiring implements AutoCloseable {
    private final CommandRegistration registration;

    private CommandWiring(CommandRegistration registration) {
        this.registration = registration;
    }

    public static CommandWiring register(
        Plugin plugin,
        LifesRuntime runtime,
        LivesService service,
        AccountDirectory directory,
        PlayerNameIndex names
    ) {
        var catalog = new DefaultCommandCatalog();
        catalog.registerSuggestionProvider(SuggestionKeys.ONLINE_PLAYERS, new OnlinePlayerSuggestions(names, directory));
        var generated = new LivesCommandDemocracyCommand(new LivesCommand(runtime, service));
        var registration = catalog.register(generated);
        PaperCommandTree.register(plugin, catalog, generated, runtime);
        return new CommandWiring(registration);
    }

    @Override
    public void close() {
        registration.close();
    }
}
