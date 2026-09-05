package it.hydr4.lifes.paper;

import it.hydr4.lifes.api.LivesService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.function.Supplier;

/** Creates a fresh account with the configured default lives on first join. */
public final class JoinListener implements Listener {
    private final Supplier<LivesService> service;

    public JoinListener(Supplier<LivesService> service) {
        this.service = java.util.Objects.requireNonNull(service, "service");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (service.get().find(player.getUniqueId()).isEmpty()) {
            service.get().create(player.getUniqueId(), player.getName());
        }
    }
}
