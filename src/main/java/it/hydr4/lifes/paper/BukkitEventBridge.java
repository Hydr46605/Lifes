package it.hydr4.lifes.paper;

import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LivesListener;
import org.bukkit.Bukkit;

/** Bridges domain changes to the Bukkit event system for external plugins. */
public final class BukkitEventBridge implements LivesListener {
    @Override
    public void onLifeChange(LifeChange change) {
        Bukkit.getPluginManager().callEvent(new LifeChangeEvent(change));
    }
}
