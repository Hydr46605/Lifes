package it.hydr4.lifes.text;

/** Every user-facing message with its default MiniMessage template. */
public enum MessageKey {
    PREFIX("prefix", "<dark_gray>[<gold>Lifes</gold>]</dark_gray> <gray>", false),
    LIVES_SELF("lives-self", "You have <bold><gold>{lives}</gold></bold> lives.", true),
    LIVES_OTHER("lives-other", "{player} has <bold><gold>{lives}</gold></bold> lives.", true),
    LIVES_SET("lives-set", "Set <white>{player}</white> to <bold><gold>{lives}</gold></bold> lives.", true),
    LIVES_ADD("lives-add", "Gave <white>{amount}</white> lives to <white>{player}</white> (now <bold><gold>{lives}</gold></bold>).", true),
    LIVES_REMOVE("lives-remove", "Removed <white>{amount}</white> lives from <white>{player}</white> (now <bold><gold>{lives}</gold></bold>).", true),
    LIVES_RESET("lives-reset", "Reset <white>{player}</white> to <bold><gold>{lives}</gold></bold> lives.", true),
    LIVES_UNKNOWN_PLAYER("lives-unknown-player", "No known account for <white>{player}</white>.", true),
    LIVES_INVALID_AMOUNT("lives-invalid-amount", "Amount must be between {minimum} and {maximum}.", true),
    LIVES_TARGET_NOTIFY("lives-target-notify", "An administrator set your lives to <bold><gold>{lives}</gold></bold>.", true),
    LIVES_EXHAUSTED_KICK("lives-exhausted-kick", "<red>You are out of lives. The game is over for this account.</red>", false),
    LIVES_RELOAD_DONE("lives-reload-done", "Configuration reloaded.", true);

    private final String key;
    private final String defaultTemplate;
    private final boolean prefixed;

    MessageKey(String key, String defaultTemplate, boolean prefixed) {
        this.key = key;
        this.defaultTemplate = defaultTemplate;
        this.prefixed = prefixed;
    }

    public String key() {
        return key;
    }

    public String defaultTemplate() {
        return defaultTemplate;
    }

    public boolean prefixed() {
        return prefixed;
    }

    /** Resolves a settings key to its enum constant, empty when unknown. */
    public static java.util.Optional<MessageKey> byKey(String key) {
        for (var value : values()) {
            if (value.key.equals(key)) {
                return java.util.Optional.of(value);
            }
        }
        return java.util.Optional.empty();
    }
}
