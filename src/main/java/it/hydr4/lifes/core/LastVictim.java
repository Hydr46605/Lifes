package it.hydr4.lifes.core;

import it.hydr4.lifes.api.LifeChange;
import it.hydr4.lifes.api.LifeChangeReason;
import it.hydr4.lifes.api.LivesListener;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Remembers the last player hit by a counted death, so any viewer can render the
 * victim's head through {@code PLAYER_HEAD:%lifes_last_victim%}. Empty until the
 * first death; platform-free on purpose.
 */
public final class LastVictim implements LivesListener {
    private final AtomicReference<String> name = new AtomicReference<>("");

    /** Last victim's name, or empty when nobody died yet. */
    public String name() {
        return name.get();
    }

    @Override
    public void onLifeChange(LifeChange change) {
        if (change.reason() == LifeChangeReason.DEATH) {
            name.set(change.after().name());
        }
    }
}
