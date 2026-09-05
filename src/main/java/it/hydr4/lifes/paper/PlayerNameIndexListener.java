package it.hydr4.lifes.paper;

import it.hydr4.lifes.command.suggest.PlayerNameIndex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

/** Keeps the suggestion name index in step with who is connected. */
public final class PlayerNameIndexListener implements Listener {
    private final PlayerNameIndex index;

    public PlayerNameIndexListener(PlayerNameIndex index) {
        this.index = Objects.requireNonNull(index, "index");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        index.online(event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        index.offline(event.getPlayer().getName());
    }
}
