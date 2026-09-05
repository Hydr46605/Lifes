package it.hydr4.lifes.death;

import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesAccount;
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
        if (change.exhausted() || (change.reason() == LifeChangeReason.DEATH && change.after().exhausted())) {
            // A death that finds the account already at zero lives re-runs the exit pipeline, so a
            // lifted ban or a crash between the save and the ban cannot leave a dead account playing.
            run(sets.exhaustion(), change);
        } else if (change.reason() == LifeChangeReason.DEATH) {
            run(sets.death(), change);
        }
    }

    /**
     * Runs the exit pipeline for an account that is already exhausted, without a life change.
     *
     * <p>The synthesized change carries the same snapshot on both sides: actions only read
     * {@code after()}, and the pipeline is entered directly rather than through the edge test.
     */
    public void runExhaustion(LivesAccount account) {
        java.util.Objects.requireNonNull(account, "account");
        run(actions.get().exhaustion(), new LifeChange(LifeChangeReason.DEATH, account, account, 0));
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
