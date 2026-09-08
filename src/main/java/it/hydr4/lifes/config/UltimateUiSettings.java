package it.hydr4.lifes.config;

/**
 * Optional UltimateUI HUD refresh: after every life change the text element is rewritten
 * while the configured GUI is open. Both blank means disabled.
 */
public record UltimateUiSettings(String refreshGui, String refreshElement, String refreshText) {
    public static UltimateUiSettings disabled() {
        return new UltimateUiSettings("", "", "{lives}");
    }

    /** True when a GUI and an element are both configured. */
    public boolean enabled() {
        return !refreshGui.isBlank() && !refreshElement.isBlank();
    }
}
