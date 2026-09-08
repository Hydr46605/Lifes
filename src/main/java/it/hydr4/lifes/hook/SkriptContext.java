package it.hydr4.lifes.hook;

import it.hydr4.lifes.api.LivesAccount;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Optional Skript surfaces beyond the bare service: account listing for dead helpers,
 * a placeholder resolver, and the UltimateUI hook for UI helpers. Empty by default.
 */
public record SkriptContext(
    Supplier<Collection<LivesAccount>> accounts,
    Supplier<PlaceholderResolver> placeholders,
    UltimateUiHook uiOrNull
) {
    public static SkriptContext empty() {
        return new SkriptContext(java.util.List::of, null, null);
    }
}
