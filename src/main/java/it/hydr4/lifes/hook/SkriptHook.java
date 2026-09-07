package it.hydr4.lifes.hook;

import it.hydr4.lifes.api.LivesService;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional Skript attachment without a compile-time Skript dependency.
 *
 * <p>When Skript is present the running {@link LivesService} is published to the Bukkit
 * {@code ServicesManager} and to {@link LifesSkript}, so scripts can drive the whole API
 * through skript-reflect (find, create, applyDeath, adjust, listeners) and can listen to
 * {@code it.hydr4.lifes.paper.LifeChangeEvent} as a Bukkit event. When Skript is absent
 * this is a no-op. Nothing here references Skript classes, so no {@code NoClassDefFoundError}
 * is possible on servers without it.
 */
public final class SkriptHook implements AutoCloseable {
    private final Plugin plugin;

    private SkriptHook(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Publishes the service for Skript, or returns empty when Skript is absent. */
    public static Optional<SkriptHook> tryAttach(LivesService service, Plugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Skript") == null) {
            return Optional.empty();
        }
        Logger logger = plugin.getLogger();
        try {
            plugin.getServer().getServicesManager()
                .register(LivesService.class, service, plugin, ServicePriority.Normal);
            LifesSkript.publish(service);
            logger.info("Skript hooks registered (LivesService published, LifeChangeEvent available).");
            return Optional.of(new SkriptHook(plugin));
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Attaching the Skript hook failed; Skript access stays disabled.", exception);
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        try {
            plugin.getServer().getServicesManager().unregisterAll(plugin);
        } finally {
            LifesSkript.unpublish();
        }
    }
}
