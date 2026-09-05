package it.hydr4.lifes.api;

/** Thrown when an operation targets an account that does not exist. */
public class UnknownAccountException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnknownAccountException(java.util.UUID id) {
        super("Unknown lives account: " + id);
    }
}
