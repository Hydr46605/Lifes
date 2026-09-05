package it.hydr4.lifes.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import it.hydr4.lifes.LifesRuntime;
import it.hydr4.lifes.api.LivesService;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** PlaceholderAPI expansion backed by {@link PlaceholderResolver}. */
public final class LifesExpansion extends PlaceholderExpansion {
    private final PlaceholderResolver resolver;
    private final String version;

    public LifesExpansion(LifesRuntime runtime, LivesService service, String version) {
        this.resolver = new PlaceholderResolver(
            service,
            runtime::maximumLives,
            () -> runtime.settings().defaultLives()
        );
        this.version = version;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lifes";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Hydr4";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        try {
            return resolve(player, params);
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String resolve(OfflinePlayer player, String params) {
        return resolver.resolve(player == null ? null : player.getUniqueId(), params);
    }
}
