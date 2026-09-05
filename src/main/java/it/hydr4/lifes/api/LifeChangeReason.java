package it.hydr4.lifes.api;

/** Why a life count changed. */
public enum LifeChangeReason {
    DEATH,
    ADMIN_SET,
    ADMIN_ADD,
    ADMIN_REMOVE,
    ADMIN_RESET
}
