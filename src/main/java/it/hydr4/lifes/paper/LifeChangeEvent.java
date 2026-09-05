package it.hydr4.lifes.paper;

import it.hydr4.lifes.api.LifeChange;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Bukkit event raised for every committed life change. Not cancellable. */
public final class LifeChangeEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final LifeChange change;

    public LifeChangeEvent(LifeChange change) {
        this.change = java.util.Objects.requireNonNull(change, "change");
    }

    public LifeChange change() {
        return change;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
