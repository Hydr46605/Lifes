package it.hydr4.lifes;

/**
 * Raised for any invalid or unreadable configuration. The message always
 * starts with the configuration path that caused the failure.
 */
public class ConfigException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConfigException(String path, String reason) {
        super(path + ": " + reason);
    }
}
