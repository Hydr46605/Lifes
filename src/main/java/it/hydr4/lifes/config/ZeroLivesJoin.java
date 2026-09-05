package it.hydr4.lifes.config;

/**
 * What happens when an account that is already out of lives connects.
 *
 * <p>The exhaustion pipeline normally runs on the transition into zero lives, so an account that
 * reaches a server restart or a lifted ban still at zero lives needs an explicit policy.
 */
public enum ZeroLivesJoin {
    /** Run the configured exhaustion pipeline again, which bans the player by default. */
    REAPPLY,
    /** Kick the player with a message and leave the account untouched for an admin to fix. */
    KICK,
    /** Do nothing; the player keeps playing with zero lives. */
    IGNORE
}
