package it.hydr4.lifes.api;

/** Observer notified after every committed life change. */
public interface LivesListener {
    void onLifeChange(LifeChange change);
}
