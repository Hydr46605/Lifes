package it.hydr4.lifes.command;

import it.hydr4.democracy.core.command.CommandRegistration;
import it.hydr4.democracy.core.command.DefaultCommandCatalog;
import it.hydr4.democracy.core.command.DefaultCommandDispatcher;
import it.hydr4.democracy.core.command.DefaultCommandParser;
import it.hydr4.democracy.paper.command.PaperCommandRegistrar;
import it.hydr4.lifes.LifesRuntime;
import it.hydr4.lifes.api.LivesService;
import it.hydr4.lifes.command.suggest.OnlinePlayerSuggestions;
import it.hydr4.lifes.command.suggest.SuggestionKeys;
import org.bukkit.plugin.Plugin;

/** Registers the generated command; close to unregister. */
public final class CommandWiring implements AutoCloseable {
    private final DefaultCommandCatalog catalog;
    private final PaperCommandRegistrar registrar;
    private final CommandRegistration registration;

    private CommandWiring(DefaultCommandCatalog catalog, PaperCommandRegistrar registrar, CommandRegistration registration) {
        this.catalog = catalog;
        this.registrar = registrar;
        this.registration = registration;
    }

    public static CommandWiring register(Plugin plugin, LifesRuntime runtime, LivesService service) {
        var catalog = new DefaultCommandCatalog();
        catalog.registerSuggestionProvider(SuggestionKeys.ONLINE_PLAYERS, new OnlinePlayerSuggestions());
        var handler = new LivesCommand(runtime, service);
        var generated = new LivesCommandDemocracyCommand(handler);
        var registration = catalog.register(generated);
        var dispatcher = new DefaultCommandDispatcher(catalog, new DefaultCommandParser(catalog));
        var registrar = new PaperCommandRegistrar(plugin, catalog, dispatcher);
        registrar.register();
        return new CommandWiring(catalog, registrar, registration);
    }

    @Override
    public void close() {
        registration.close();
    }
}
