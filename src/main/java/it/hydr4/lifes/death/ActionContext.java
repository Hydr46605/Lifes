package it.hydr4.lifes.death;

import it.hydr4.lifes.api.LifeChange;
import org.bukkit.OfflinePlayer;

/** Input for one action execution. */
public record ActionContext(LifeChange change, OfflinePlayer player, int maximumLives) {
    public ActionContext {
        java.util.Objects.requireNonNull(change, "change");
        java.util.Objects.requireNonNull(player, "player");
    }
}
