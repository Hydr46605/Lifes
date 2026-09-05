package it.hydr4.lifes.api;

/**
 * Immutable outcome of one committed life change.
 *
 * @param reason why the change happened
 * @param before snapshot before the change
 * @param after  snapshot after the change
 * @param delta  signed lives delta applied ({@code after.lives - before.lives})
 */
public record LifeChange(LifeChangeReason reason, LivesAccount before, LivesAccount after, int delta) {
    public LifeChange {
        java.util.Objects.requireNonNull(reason, "reason");
        java.util.Objects.requireNonNull(before, "before");
        java.util.Objects.requireNonNull(after, "after");
    }

    /** True exactly when this change exhausted the account (edge, not level). */
    public boolean exhausted() {
        return !before.exhausted() && after.exhausted();
    }

    /** True when the change altered any observable state. */
    public boolean changed() {
        return !before.equals(after);
    }
}
