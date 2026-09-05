package it.hydr4.lifes.death;

/** One configured consequence of a death or of life exhaustion. */
public interface LifesAction {
    void execute(ActionContext context);
}
