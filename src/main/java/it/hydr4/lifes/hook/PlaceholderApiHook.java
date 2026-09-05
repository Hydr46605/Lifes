package it.hydr4.lifes.hook;

import it.hydr4.lifes.LifesRuntime;
import it.hydr4.lifes.api.LivesService;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Optional PlaceholderAPI attachment; close to unregister. */
public final class PlaceholderApiHook implements AutoCloseable {
    private final LifesExpansion expansion;

    private PlaceholderApiHook(LifesExpansion expansion) {
        this.expansion = expansion;
    }

    /** Returns the attached hook, or empty when PlaceholderAPI is absent or refused registration. */
    public static Optional<PlaceholderApiHook> tryAttach(LifesRuntime runtime, LivesService service, Plugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return Optional.empty();
        }
        var logger = plugin.getLogger();
        try {
            var expansion = new LifesExpansion(runtime, service, plugin.getPluginMeta().getVersion());
            if (!expansion.register()) {
                logger.warning("PlaceholderAPI refused the Lifes expansion registration.");
                return Optional.empty();
            }
            logger.info("PlaceholderAPI hooks registered.");
            return Optional.of(new PlaceholderApiHook(expansion));
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Attaching the PlaceholderAPI expansion failed; placeholders stay disabled.", exception);
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        expansion.unregister();
    }
}
