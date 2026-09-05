package it.hydr4.lifes.death;

import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Executes pipelines; one failing action never stops the others. */
public final class ActionRunner implements LivesListener {
    private final Supplier<ActionSets> actions;
    private final IntSupplier maximumLives;
    private final Logger logger;

    public ActionRunner(Supplier<ActionSets> actions, IntSupplier maximumLives, Logger logger) {
        this.actions = Objects.requireNonNull(actions, "actions");
        this.maximumLives = Objects.requireNonNull(maximumLives, "maximumLives");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void onLifeChange(LifeChange change) {
        var sets = actions.get();
        if (change.exhausted()) {
            run(sets.exhaustion(), change);
        } else if (change.reason() == LifeChangeReason.DEATH) {
            run(sets.death(), change);
        }
    }

    private void run(List<LifesAction> pipeline, LifeChange change) {
        if (pipeline.isEmpty()) {
            return;
        }
        var player = Bukkit.getPlayer(change.after().uuid());
        var context = new ActionContext(
            change,
            player != null ? player : Bukkit.getOfflinePlayer(change.after().uuid()),
            maximumLives.getAsInt()
        );
        for (var action : pipeline) {
            try {
                action.execute(context);
            } catch (RuntimeException exception) {
                logger.log(
                    Level.SEVERE,
                    "Death action " + action.getClass().getSimpleName() + " failed for " + change.after().name(),
                    exception
                );
            }
        }
    }
}
