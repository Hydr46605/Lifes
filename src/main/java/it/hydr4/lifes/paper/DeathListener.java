package it.hydr4.lifes.paper;

import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LivesService;
import it.hydr4.lifes.config.LivesSettings;
import it.hydr4.lifes.text.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.function.Supplier;

/** Records deaths at MONITOR priority; ignored causes consume nothing. */
public final class DeathListener implements Listener {
    private final LivesService service;
    private final Supplier<LivesSettings> settings;

    public DeathListener(LivesService service, Supplier<LivesSettings> settings) {
        this.service = service;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        var victim = event.getEntity();
        if (isIgnored(victim)) {
            return;
        }
        service.applyDeath(victim.getUniqueId(), victim.getName(), settings.get().deathCost());
    }

    private boolean isIgnored(Player victim) {
        var ignored = settings.get().ignoredDeathCauses();
        if (ignored.isEmpty()) {
            return false;
        }
        var cause = victim.getLastDamageCause();
        return cause instanceof EntityDamageEvent damageEvent && ignored.contains(damageEvent.getCause().name());
    }
}
